import sys
import socket
import time

TIMEOUT = 1.0  # 1 segundo
NUM_PINGS = 10

def main():
    if len(sys.argv) != 3:
        print("Required arguments: host port")
        return

    host = sys.argv[1]
    port = int(sys.argv[2])

    client_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    client_socket.settimeout(TIMEOUT)

    for seq in range(NUM_PINGS):
        send_time = time.time()
        time_ms = int(send_time * 1000)

        payload = f"PING {seq} {time_ms}\r\n"
        
        try:
            client_socket.sendto(payload.encode('utf-8'), (host, port))
            
            data, server_address = client_socket.recvfrom(1024)
            receive_time = time.time()
            rtt = int((receive_time - send_time) * 1000)
            
            response = data.decode('utf-8').strip()
            print(f"Reply from {server_address[0]}: {response} RTT={rtt} ms")

        except socket.timeout:
            print(f"Request timed out for seq {seq}")

        time.sleep(1.0)

    client_socket.close()

if __name__ == "__main__":
    main()