package rpc;

import java.util.Arrays;
import java.util.List;

import modelo.Candidato;
import modelo.CarregadorDados;
import modelo.Resultado;
import modelo.ServicoException;
import modelo.ServicoVotacao;

/**
 * Teste automatizado do serviço remoto (cliente e servidor no mesmo processo, via TCP em loopback).
 * <pre>java rpc.TesteRPC</pre>
 */
public class TesteRPC {
    private static int falhas = 0;

    public static void main(String[] args) throws Exception {
        ServicoVotacao servico = new ServicoVotacao(CarregadorDados.padrao(3)); // votação de 3 s
        ServidorRPC servidor = new ServidorRPC(servico);
        int porta = servidor.iniciar(0);

        try (ClienteRPC c = new ClienteRPC("localhost", porta)) {
            List<Candidato> lista = c.listarCandidatos();
            verificar(lista.size() == 3 && lista.get(0).getNome().equals("Ada Lovelace"), "listarCandidatos()");
            verificar(c.tempoRestanteMs() > 0, "tempoRestante() > 0 enquanto aberta");

            String comprovante = c.votar("eleitor1", "123", 13);
            verificar(comprovante != null && comprovante.length() > 10, "votar() devolve comprovante");
            verificar(erro(() -> c.votar("eleitor1", "123", 22)).contains("já votou"), "voto duplicado é recusado");
            verificar(erro(() -> c.votar("eleitor2", "errada", 22)).contains("inválidos"), "senha errada é recusada");
            verificar(erro(() -> c.votar("eleitor2", "123", 999)).contains("inexistente"), "candidato inexistente é recusado");
            verificar(erro(() -> c.votar("admin", "admin123", 13)).contains("eleitores"), "administrador não vota");
            verificar(erro(() -> c.apurar()).contains("ainda está aberta"), "apurar() recusado antes do prazo");

            c.votar("eleitor2", "123", 13);
            c.votar("eleitor3", "123", 22);

            Thread.sleep(3300); // aguarda o prazo
            verificar(erro(() -> c.votar("eleitor4", "123", 45)).contains("encerrada"), "voto após o prazo é recusado");

            Resultado r = c.apurar();
            verificar(r.getTotalVotos() == 3, "total de votos = 3");
            verificar(r.getVencedores().size() == 1 && r.getVencedores().get(0).getNumero() == 13, "vencedor = candidato 13");
            verificar(Math.abs(r.getItens().get(0).getPercentual() - 66.6667) < 0.01, "percentual do vencedor ≈ 66,67%");
            verificar(r.getItens().size() == 3 && r.getItens().get(2).getVotos() == 0, "todos os candidatos aparecem na apuração");
        }

        // Empacotamento isolado
        Requisicao req = new Requisicao(7, "votar", "a", "b", 5, null, 10L, true, new Candidato[] { new Candidato(1, "X", "Y") });
        Requisicao volta = Empacotador.desempacotarRequisicao(Empacotador.empacotarRequisicao(req));
        verificar(volta.getId() == 7 && volta.getMetodo().equals("votar")
                && Arrays.deepEquals(volta.getArgumentos(), req.getArgumentos()), "ida e volta do empacotamento de Requisicao");

        servidor.parar();
        System.out.println(falhas == 0 ? "\nTodos os testes passaram." : "\n" + falhas + " teste(s) FALHARAM.");
        System.exit(falhas == 0 ? 0 : 1);
    }

    private interface Chamada {
        void executar() throws Exception;
    }

    /** Executa a chamada esperando um ServicoException e devolve sua mensagem. */
    private static String erro(Chamada chamada) throws Exception {
        try {
            chamada.executar();
            return "(sem erro)";
        } catch (ServicoException e) {
            return e.getMessage();
        }
    }

    private static void verificar(boolean condicao, String descricao) {
        System.out.println((condicao ? "[OK]   " : "[FALHA] ") + descricao);
        if (!condicao) {
            falhas++;
        }
    }
}
