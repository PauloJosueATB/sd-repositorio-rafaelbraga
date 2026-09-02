import sys
import socket
import time

TIMEOUT = 1.0  # 1 segundo
NUM_PINGS = 10

def main():
    if len(sys.argv) != 3:
        print("Uso: python3 ping_client_ex1.py <host> <port>")
        return

    host = sys.argv[1]
    port = int(sys.argv[2])

    client_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    client_socket.settimeout(TIMEOUT)

    rtts = []
    packets_sent = 0
    packets_received = 0

    print(f"PING {host} ({host}): 56 bytes de dados")

    for seq in range(NUM_PINGS):
        send_time = time.time()
        time_ms = int(send_time * 1000)
        payload = f"PING {seq} {time_ms}\r\n"
        
        packets_sent += 1

        try:
            client_socket.sendto(payload.encode('utf-8'), (host, port))
            data, server_address = client_socket.recvfrom(1024)
            
            receive_time = time.time()
            rtt = (receive_time - send_time) * 1000  # RTT em milissegundos
            
            rtts.append(rtt)
            packets_received += 1
            
            response = data.decode('utf-8').strip()
            print(f"Resposta de {server_address[0]}: seq={seq} rtt={rtt:.2f} ms")

        except socket.timeout:
            print(f"Esgotado o tempo limite do pedido para seq {seq}")

        time.sleep(1.0)

    client_socket.close()

    print(f"\n--- Estatísticas do Ping para {host} ---")
    loss_rate = ((packets_sent - packets_received) / packets_sent) * 100
    print(f"{packets_sent} pacotes transmitidos, {packets_received} recebidos, {loss_rate:.1f}% de perda de pacotes")

    if rtts:
        rtt_min = min(rtts)
        rtt_max = max(rtts)
        rtt_avg = sum(rtts) / len(rtts)
        print(f"rtt min/avg/max = {rtt_min:.2f}/{rtt_avg:.2f}/{rtt_max:.2f} ms")

if __name__ == "__main__":
    main()