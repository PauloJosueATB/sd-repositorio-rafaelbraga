package streams.teste;

import java.net.ServerSocket;
import java.net.Socket;

import modelo.Candidato;
import streams.EleicaoInputStream;

/**
 * QUESTÃO 3 (d) / 2 (b-iii): servidor remoto TCP. Para cada conexão (uma thread por cliente)
 * lê os bytes do socket com {@link EleicaoInputStream} e imprime os candidatos recebidos.
 *
 * <pre>java streams.teste.ServidorEleicaoTCP [porta]     # padrão: 7000</pre>
 */
public class ServidorEleicaoTCP {
    public static final int PORTA_PADRAO = 7000;

    public static void main(String[] args) throws Exception {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : PORTA_PADRAO;
        try (ServerSocket servidor = new ServerSocket(porta)) {
            System.out.println("Servidor de candidatos aguardando conexões na porta " + porta + "...");
            while (true) {
                Socket cliente = servidor.accept();
                new Thread(() -> atender(cliente)).start();
            }
        }
    }

    static void atender(Socket cliente) {
        String origem = cliente.getRemoteSocketAddress().toString();
        try (Socket s = cliente; EleicaoInputStream entrada = new EleicaoInputStream(s.getInputStream())) {
            Candidato[] recebidos = entrada.lerCandidatos();
            System.out.println("Recebidos " + recebidos.length + " candidato(s) de " + origem + ":");
            for (Candidato c : recebidos) {
                System.out.println("  " + c);
            }
        } catch (Exception e) {
            System.err.println("Erro ao atender " + origem + ": " + e);
        }
    }
}
