package votacao.servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import modelo.CarregadorDados;
import modelo.Formatadores;
import modelo.RepositorioEleicao;
import modelo.ServicoAdministracao;
import modelo.ServicoException;
import modelo.ServicoVotacao;
import votacao.protocolo.Protocolo;

/**
 * QUESTÃO 5 - Servidor multi-threaded do sistema de votações.
 * <ul>
 *   <li>Unicast: sockets TCP (login, lista de candidatos, votos, administração). Uma thread por cliente.</li>
 *   <li>Multicast: sockets UDP, exclusivamente para as notas informativas dos administradores.</li>
 *   <li>Prazo: ao término do tempo o servidor deixa de aceitar votos e imprime a apuração.</li>
 * </ul>
 *
 * <pre>java votacao.servidor.ServidorVotacao [portaTcp=5000] [duracaoSeg=120] [grupo=230.0.0.1] [portaMulticast=4446]</pre>
 */
public class ServidorVotacao {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final RepositorioEleicao repositorio;
    private final ServicoVotacao servicoVotacao;
    private final ServicoAdministracao servicoAdministracao;
    private final PublicadorMulticast publicador;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final ScheduledExecutorService agendador = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "apuracao");
        t.setDaemon(true);
        return t;
    });
    private ServerSocket serverSocket;
    private volatile boolean ativo;

    public ServidorVotacao(RepositorioEleicao repositorio, String grupoMulticast, int portaMulticast) throws IOException {
        this.repositorio = repositorio;
        this.servicoVotacao = new ServicoVotacao(repositorio);
        this.servicoAdministracao = new ServicoAdministracao(repositorio);
        this.publicador = new PublicadorMulticast(grupoMulticast, portaMulticast);
    }

    public ServicoVotacao getServicoVotacao() { return servicoVotacao; }
    public ServicoAdministracao getServicoAdministracao() { return servicoAdministracao; }
    public PublicadorMulticast getPublicador() { return publicador; }

    /** Abre a porta TCP (0 = qualquer livre) e passa a aceitar clientes. Devolve a porta efetiva. */
    public int iniciar(int portaTcp) throws IOException {
        serverSocket = new ServerSocket(portaTcp);
        ativo = true;
        agendarApuracao();
        Thread aceitador = new Thread(this::aceitar, "aceitador");
        aceitador.start();
        return serverSocket.getLocalPort();
    }

    public void parar() {
        ativo = false;
        try {
            serverSocket.close();
        } catch (IOException ignorado) {
            // encerrando
        }
        pool.shutdownNow();
        agendador.shutdownNow();
        publicador.close();
    }

    private void aceitar() {
        while (ativo) {
            try {
                Socket cliente = serverSocket.accept();
                pool.execute(new AtendenteCliente(cliente, this));   // uma thread por cliente
            } catch (IOException e) {
                if (ativo) {
                    log("erro ao aceitar conexão: " + e);
                }
            }
        }
    }

    /** No fim do prazo, calcula e imprime total de votos, percentagens e vencedor. */
    private void agendarApuracao() {
        long atraso = servicoVotacao.tempoRestanteMs() + 100; // pequena folga para o relógio passar do prazo
        agendador.schedule(() -> {
            try {
                log("*** PRAZO ENCERRADO: votos não são mais aceitos ***");
                System.out.println(Formatadores.resultado(servicoVotacao.apurar()));
            } catch (ServicoException e) {
                log("não foi possível apurar: " + e.getMessage());
            }
        }, atraso, TimeUnit.MILLISECONDS);
    }

    public synchronized void log(String mensagem) {
        System.out.println("[" + LocalTime.now().format(HORA) + "] [" + Thread.currentThread().getName() + "] " + mensagem);
    }

    public static void main(String[] args) throws Exception {
        int portaTcp = args.length > 0 ? Integer.parseInt(args[0]) : Protocolo.PORTA_TCP_PADRAO;
        int duracao = args.length > 1 ? Integer.parseInt(args[1]) : 120;
        String grupo = args.length > 2 ? args[2] : Protocolo.GRUPO_MULTICAST_PADRAO;
        int portaMulticast = args.length > 3 ? Integer.parseInt(args[3]) : Protocolo.PORTA_MULTICAST_PADRAO;

        RepositorioEleicao repo;
        try {
            repo = CarregadorDados.carregar(Paths.get("dados"), duracao);
        } catch (IOException e) {
            System.err.println("Erro ao carregar a pasta 'dados': " + e.getMessage());
            System.exit(1);
            return;
        }
        ServidorVotacao servidor = new ServidorVotacao(repo, grupo, portaMulticast);
        servidor.iniciar(portaTcp);

        System.out.println("=== " + repo.getEleicao().getTitulo() + " ===");
        System.out.println("TCP (unicast)     : porta " + portaTcp);
        System.out.println("UDP (multicast)   : " + grupo + ":" + portaMulticast + " (somente notas dos administradores)");
        System.out.println("Prazo para votar  : " + Formatadores.duracao(duracao * 1000L) + " a partir de agora");
        System.out.println("Candidatos        : " + repo.listarCandidatos());
    }
}
