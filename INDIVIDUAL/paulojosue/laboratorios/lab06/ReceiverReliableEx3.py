import sys
import socket

def main():
    if len(sys.argv) != 2:
        print("Uso: python3 reliable_udp_receiver.py <porta>")
        return

    port = int(sys.argv[1])
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    server_socket.bind(('', port))

    print(f"Receptor Confiável UDP ouvindo na porta {port}...")

    expected_seq = 0

    while True:
        data, client_address = server_socket.recvfrom(1024)
        message = data.decode('utf-8').strip()

        try:
            seq_str, content = message.split(":", 1)
            seq_num = int(seq_str)

            print(f"Recebido pacote seq={seq_num}: '{content}'")

            ack_message = f"ACK:{seq_num}"
            server_socket.sendto(ack_message.encode('utf-8'), client_address)
            print(f"ACK:{seq_num} enviado para {client_address[0]}")

        except ValueError:
            pass

if __name__ == "__main__":
    main()