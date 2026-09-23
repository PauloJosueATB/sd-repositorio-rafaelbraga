package rpc;

import modelo.Candidato;
import modelo.Formatadores;
import modelo.ServicoException;

/**
 * QUESTÃO 4 - Cliente de demonstração: consulta candidatos, vota, tenta votar de novo
 * e tenta apurar (o servidor recusa enquanto a votação estiver aberta).
 *
 * <pre>java rpc.DemoClienteRPC [host=localhost] [porta=6000] [login=eleitor1] [senha=123] [candidato=13]</pre>
 */
public class DemoClienteRPC {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : ServidorRPC.PORTA_PADRAO;
        String login = args.length > 2 ? args[2] : "eleitor1";
        String senha = args.length > 3 ? args[3] : "123";
        int candidato = args.length > 4 ? Integer.parseInt(args[4]) : 13;

        try (ClienteRPC cliente = new ClienteRPC(host, porta)) {
            System.out.println("Tempo restante: " + Formatadores.duracao(cliente.tempoRestanteMs()));
            System.out.println("Candidatos:");
            for (Candidato c : cliente.listarCandidatos()) {
                System.out.println("  " + c);
            }

            System.out.println("\nVotando em " + candidato + " como " + login + "...");
            System.out.println("Comprovante: " + tentar(() -> cliente.votar(login, senha, candidato)));

            System.out.println("\nTentando votar novamente...");
            System.out.println(tentar(() -> cliente.votar(login, senha, candidato)));

            System.out.println("\nTentando apurar...");
            Object r = tentar(() -> Formatadores.resultado(cliente.apurar()));
            System.out.println(r);
        }
    }

    private interface Chamada {
        Object executar() throws Exception;
    }

    /** Converte erros de regra de negócio em texto para exibição. */
    private static Object tentar(Chamada chamada) throws Exception {
        try {
            return chamada.executar();
        } catch (ServicoException e) {
            return "ERRO DO SERVIDOR: " + e.getMessage();
        }
    }
}
