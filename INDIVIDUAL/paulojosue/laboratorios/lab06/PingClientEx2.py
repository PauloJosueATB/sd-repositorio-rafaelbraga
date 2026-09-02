import sys
import socket
import time

TIMEOUT = 1.0
NUM_PINGS = 10

def main():
    if len(sys.argv) != 3:
        print("Uso: python3 ping_client_ex2.py <host> <port>")
        return

    host = sys.argv[1]
    port = int(sys.argv[2])

    client_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    client_socket.settimeout(TIMEOUT)

    rtts = []
    packets_sent = 0
    packets_received = 0

    for seq in range(NUM_PINGS):
        cycle_start_time = time.time() 
        
        time_ms = int(cycle_start_time * 1000)
        payload = f"PING {seq} {time_ms}\r\n"
        packets_sent += 1

        try:
            client_socket.sendto(payload.encode('utf-8'), (host, port))
            data, server_address = client_socket.recvfrom(1024)
            
            receive_time = time.time()
            rtt = (receive_time - cycle_start_time) * 1000
            rtts.append(rtt)
            packets_received += 1
            
            print(f"Resposta de {server_address[0]}: seq={seq} rtt={rtt:.2f} ms")

        except socket.timeout:
            print(f"Esgotado o tempo limite do pedido para seq {seq}")

        elapsed = time.time() - cycle_start_time

        time_to_sleep = max(0.0, 1.0 - elapsed)
        time.sleep(time_to_sleep)

    client_socket.close()

if __name__ == "__main__":
    main()