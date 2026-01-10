import asyncio
import websockets

async def repl_handler(websocket):
    print(f"Client connected from {websocket.remote_address}")
    
    # Consume the initial greeting from the client
    try:
        initial_message = await websocket.recv()
        print(f"< {initial_message}")
    except websockets.exceptions.ConnectionClosed:
        print("Client disconnected before sending greeting")
        return
        
    print("Type JavaScript code to execute on the client. Type 'exit' to quit.")
    
    try:
        while True:
            # Use run_in_executor to avoid blocking the event loop with input()
            command = await asyncio.get_event_loop().run_in_executor(None, input, "js> ")
            
            if command.lower() in ['exit', 'quit']:
                break
                
            if not command.strip():
                continue

            # Send command to client
            await websocket.send(command)
            
            # Wait for execution result
            response = await websocket.recv()
            print(f"< {response}")
            
    except websockets.exceptions.ConnectionClosed:
        print("Client disconnected")
    except Exception as e:
        print(f"Error: {e}")

async def main():
    print("REPL Server starting on 0.0.0.0:1337...")
    async with websockets.serve(repl_handler, "0.0.0.0", 1337):
        await asyncio.Future()  # run forever

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nServer stopped.")
