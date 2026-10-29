package rmi;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import modelo.CarregadorDados;
import modelo.RepositorioEleicao;
import modelo.ServicoAdministracao;
import modelo.ServicoVotacao;

/**
 * Processo servidor. Uso: {@code ServidorVotacaoRMI [porta] [duracaoSegundos] [nomeServico]}.
 * Cria o registro RMI (equivalente ao "localizador de serviços") e publica o
 * {@link Invocador} com {@code Registry.rebind}. Não há nenhuma criação de
 * {@code ServerSocket}/{@code Socket} neste código: todo o transporte é feito
 * pela Java RMI.
 */
public final class ServidorVotacaoRMI {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private ServidorVotacaoRMI() {
    }

    public static void main(String[] args) throws Exception {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 1099;
        int duracaoSegundos = args.length > 1 ? Integer.parseInt(args[1]) : 120;
        String nomeServico = args.length > 2 ? args[2] : "ServicoVotacao";

        Path pasta = Paths.get("dados");
        RepositorioEleicao repo;
        try {
            repo = CarregadorDados.carregar(pasta, duracaoSegundos);
        } catch (Exception e) {
            System.err.println("Não foi possível carregar os dados em '" + pasta.toAbsolutePath() + "': " + e.getMessage());
            System.exit(1);
            return;
        }
        ServicoVotacao servicoVotacao = new ServicoVotacao(repo);
        ServicoAdministracao servicoAdministracao = new ServicoAdministracao(repo);
        Invocador invocador = new Invocador(servicoVotacao, servicoAdministracao);

        Registry registro = LocateRegistry.createRegistry(porta);
        registro.rebind(nomeServico, invocador);

        System.out.println("[" + LocalTime.now().format(HORA) + "] Servidor RMI no ar.");
        System.out.println("  Registro RMI na porta " + porta + ", serviço publicado como \"" + nomeServico + "\".");
        System.out.println("  Eleição: " + repo.getEleicao().getTitulo());
        System.out.println("  Duração da votação: " + duracaoSegundos + " s.");
        System.out.println("  Pressione Ctrl+C para encerrar.");
    }
}
