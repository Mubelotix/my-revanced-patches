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
    
    command_buffer = ""

    try:
        while True:
            # Use run_in_executor to avoid blocking the event loop with input()
            prompt = "..> " if command_buffer else "js> "
            line = await asyncio.get_event_loop().run_in_executor(None, input, prompt)
            
            if line.strip().lower() in ['exit', 'quit']:
                break
            
            command_buffer += line + "\n"
            
            # Basic heuristic for multiline support
            # Counts open/close braces to decide if the statement is complete
            if (command_buffer.count('{') > command_buffer.count('}') or
                command_buffer.count('(') > command_buffer.count(')') or
                command_buffer.count('[') > command_buffer.count(']')):
                continue

            if not command_buffer.strip():
                command_buffer = ""
                continue

            # Send command to client
            await websocket.send(command_buffer)
            command_buffer = ""
            
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
