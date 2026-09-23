package streams.teste;

import java.net.Socket;

import streams.EleicaoOutputStream;

/**
 * QUESTÃO 2 (b-iii): destino = servidor remoto (TCP). Requer o {@link ServidorEleicaoTCP} em execução.
 *
 * <pre>java streams.teste.TesteSaidaTCP [host] [porta]     # padrão: localhost 7000</pre>
 */
public class TesteSaidaTCP {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : ServidorEleicaoTCP.PORTA_PADRAO;

        try (Socket socket = new Socket(host, porta);
             EleicaoOutputStream stream = new EleicaoOutputStream(
                     Amostra.candidatos(), Amostra.QUANTIDADE_ENVIADA, socket.getOutputStream())) {
            System.out.println("Enviados " + Amostra.QUANTIDADE_ENVIADA + " candidatos ("
                    + stream.getBytesEnviados() + " bytes) para " + host + ":" + porta);
        }
    }
}
