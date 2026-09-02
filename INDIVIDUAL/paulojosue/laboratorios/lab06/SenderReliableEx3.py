import sys
import socket
import time

TIMEOUT = 1.0

class ReliableUdpSender:
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.socket.settimeout(TIMEOUT)

    def send_reliable(self, seq_num, message):
        payload = f"{seq_num}:{message}"
        ack_received = False
        attempts = 0

        while not ack_received:
            attempts += 1
            print(f"Tentativa {attempts}: Enviando seq={seq_num} -> '{message}'")
            self.socket.sendto(payload.encode('utf-8'), (self.host, self.port))

            try:
                data, _ = self.socket.recvfrom(1024)
                ack_msg = data.decode('utf-8').strip()

                if ack_msg == f"ACK:{seq_num}":
                    print(f"SUCESSO: Recebido ACK:{seq_num} em {attempts} tentativa(s)\n")
                    ack_received = True

            except socket.timeout:
                print(f"TIMEOUT: ACK:{seq_num} não recebido. Retransmitindo...")

    def close(self):
        self.socket.close()

def main():
    if len(sys.argv) != 3:
        print("Uso: python3 reliable_udp_sender.py <host> <porta>")
        return

    host = sys.argv[1]
    port = int(sys.argv[2])

    sender = ReliableUdpSender(host, port)

    mensagens = ["Mensagem 1", "Mensagem 2", "Mensagem 3", "Mensagem 4", "Mensagem 5"]

    for seq, msg in enumerate(mensagens):
        sender.send_reliable(seq, msg)

    sender.close()

if __name__ == "__main__":
    main()