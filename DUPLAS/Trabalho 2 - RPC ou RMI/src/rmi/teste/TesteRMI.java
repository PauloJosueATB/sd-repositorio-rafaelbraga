package rmi.teste;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.Callable;

import modelo.Candidato;
import modelo.CarregadorDados;
import modelo.Resultado;
import modelo.ServicoAdministracao;
import modelo.ServicoException;
import modelo.ServicoVotacao;
import rmi.ComunicadorRemoto;
import rmi.Invocador;
import rmi.RemoteObjectRef;
import rmi.Requisitor;

/**
 * Teste de integração: cria um registro RMI real (loopback), publica o
 * {@link Invocador} e exercita o serviço inteiro através do {@link Requisitor},
 * exatamente como fariam o servidor e o cliente em processos separados.
 * <pre>java rmi.teste.TesteRMI</pre>
 */
public class TesteRMI {
    private static int falhas = 0;

    public static void main(String[] args) throws Exception {
        int porta = 17099 + (int) (System.nanoTime() % 300);
        int duracaoSegundos = 4;

        modelo.RepositorioEleicao repo = CarregadorDados.padrao(duracaoSegundos);
        ServicoVotacao servicoVotacao = new ServicoVotacao(repo);
        ServicoAdministracao servicoAdministracao = new ServicoAdministracao(repo);
        Invocador invocador = new Invocador(servicoVotacao, servicoAdministracao);

        Registry registro = LocateRegistry.createRegistry(porta);
        registro.rebind("ServicoVotacao", invocador);
        ComunicadorRemoto stub = (ComunicadorRemoto) registro.lookup("ServicoVotacao");
        RemoteObjectRef ref = new RemoteObjectRef("ServicoVotacao", stub);

        // ---- sem login, nada funciona ----
        Requisitor semLogin = new Requisitor();
        verificar(erro(() -> semLogin.listarCandidatos(ref)).contains("Sessão"), "operação sem login é recusada");

        // ---- login errado e certo ----
        Requisitor eleitor1 = new Requisitor();
        verificar(erro(() -> { eleitor1.autenticar(ref, "eleitor1", "senhaErrada"); return null; }).contains("inválidos"), "login com senha errada é recusado");
        eleitor1.autenticar(ref, "eleitor1", "123");
        verificar("ELEITOR".equals(eleitor1.getPerfilAutenticado()), "login correto devolve o perfil ELEITOR");

        // ---- listar candidatos (3 candidatos padrão) ----
        List<Candidato> candidatos = eleitor1.listarCandidatos(ref);
        verificar(candidatos.size() == 3, "listarCandidatos() devolve os 3 candidatos padrão");

        // ---- votar, voto duplicado, candidato inexistente ----
        String comprovante = eleitor1.votar(ref, 13);
        verificar(comprovante != null && comprovante.length() > 10, "votar() devolve um comprovante");
        verificar(erro(() -> eleitor1.votar(ref, 22)).contains("já votou"), "voto duplicado é recusado");

        Requisitor eleitor2 = new Requisitor();
        eleitor2.autenticar(ref, "eleitor2", "123");
        verificar(erro(() -> eleitor2.votar(ref, 999)).contains("inexistente"), "voto em candidato inexistente é recusado");
        eleitor2.votar(ref, 13);

        // ---- controle de perfil ----
        Requisitor admin = new Requisitor();
        admin.autenticar(ref, "admin", "admin123");
        verificar("ADMINISTRADOR".equals(admin.getPerfilAutenticado()), "login do administrador devolve o perfil ADMINISTRADOR");
        verificar(erro(() -> admin.votar(ref, 13)).contains("eleitores"), "administrador não pode votar");
        verificar(erro(() -> eleitor1.adicionarCandidato(ref, 99, "Intruso", "X")).contains("administradores"), "eleitor não pode administrar candidatos");

        // ---- administração de candidatos ----
        Candidato novo = admin.adicionarCandidato(ref, 77, "Katherine Johnson", "Independente");
        verificar(novo.getNumero() == 77, "adicionarCandidato() cadastra o novo candidato");
        verificar(eleitor2.listarCandidatos(ref).size() == 4, "o novo candidato aparece para os eleitores");
        verificar(erro(() -> admin.removerCandidato(ref, 13)).contains("já recebeu votos"), "não é possível remover candidato que já recebeu votos");
        admin.removerCandidato(ref, 77);
        verificar(eleitor2.listarCandidatos(ref).size() == 3, "removerCandidato() tira o candidato da lista");

        // ---- apuração bloqueada antes do prazo ----
        verificar(erro(() -> eleitor1.apurar(ref)).contains("ainda está aberta"), "apurar() é recusado antes do prazo");
        verificar(eleitor1.votacaoAberta(ref), "votacaoAberta() = true antes do prazo");

        Thread.sleep((duracaoSegundos * 1000L) + 400);

        // ---- após o prazo ----
        verificar(!eleitor1.votacaoAberta(ref), "votacaoAberta() = false após o prazo");
        verificar(erro(() -> eleitor1.votar(ref, 13)).contains("encerrada"), "voto tardio é recusado");
        Resultado r = admin.apurar(ref);
        verificar(r.getTotalVotos() == 2, "apuração: total de votos correto");
        double somaPercentuais = r.getItens().stream().mapToDouble(i -> i.getPercentual()).sum();
        verificar(Math.abs(somaPercentuais - 100.0) < 0.001 || r.getTotalVotos() == 0, "apuração: percentuais somam 100%");

        // ---- methodId desconhecido / chamada direta ao objeto remoto ----
        byte[] resp = stub.invocarRemota(-1, rmi.Empacotador.empacotar(java.util.Map.of("token", "x")));
        verificar(new String(resp, java.nio.charset.StandardCharsets.UTF_8).contains("DESCONHECIDA"), "methodId desconhecido devolve erro, não exceção de transporte");

        if (falhas == 0) {
            System.out.println("Todos os testes passaram.");
        } else {
            System.out.println(falhas + " teste(s) falharam.");
            System.exit(1);
        }
        System.exit(0);
    }

    private static String erro(Callable<?> chamada) {
        try {
            chamada.call();
            return "";
        } catch (ServicoException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "EXCEÇÃO INESPERADA: " + e;
        }
    }

    private static void verificar(boolean condicao, String descricao) {
        if (condicao) {
            System.out.println("[OK]   " + descricao);
        } else {
            System.out.println("[FALHOU] " + descricao);
            falhas++;
        }
    }
}
