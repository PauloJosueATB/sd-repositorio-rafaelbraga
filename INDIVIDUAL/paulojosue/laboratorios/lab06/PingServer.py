import sys
import socket
import random
import time

LOSS_RATE = 0.3
AVERAGE_DELAY = 100  # ms

def main():
    if len(sys.argv) != 2:
        print("Required arguments: port")
        return

    port = int(sys.argv[1])
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    server_socket.bind(('', port))

    print(f"PingServer iniciado na porta {port}...")

    while True:
        data, client_address = server_socket.recvfrom(1024)

        first_line = data.decode('utf-8', errors='ignore').splitlines()[0]
        print(f"Received from {client_address[0]}:{first_line}")

        if random.random() < LOSS_RATE:
            print("Reply not sent.")
            continue

        delay = (random.random() * 2 * AVERAGE_DELAY) / 1000.0
        time.sleep(delay)

        server_socket.sendto(data, client_address)
        print("Reply sent.")

if __name__ == "__main__":
    main()