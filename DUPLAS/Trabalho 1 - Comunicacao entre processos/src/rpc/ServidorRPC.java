package rpc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import modelo.Candidato;
import modelo.CarregadorDados;
import modelo.RepositorioEleicao;
import modelo.ServicoException;
import modelo.ServicoVotacao;
import modelo.Usuario;
import modelo.Voto;

/**
 * QUESTÃO 4 - Servidor do serviço remoto de votação (sockets TCP, fluxos de bytes).
 * Cada conexão é atendida por uma thread. Métodos remotos disponíveis:
 * <pre>
 *   listarCandidatos()                         -> Candidato[]
 *   votar(String login, String senha, int n)   -> String (comprovante)
 *   apurar()                                   -> Resultado (apenas após o prazo)
 *   tempoRestante()                            -> long (milissegundos)
 * </pre>
 * Execução: {@code java rpc.ServidorRPC [porta=6000] [duracaoEmSegundos=60]}
 */
public class ServidorRPC {
    public static final int PORTA_PADRAO = 6000;

    private final ServicoVotacao servico;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;
    private volatile boolean ativo;

    public ServidorRPC(ServicoVotacao servico) {
        this.servico = servico;
    }

    /** Abre o socket (porta 0 = qualquer porta livre) e começa a aceitar clientes em segundo plano. */
    public int iniciar(int porta) throws IOException {
        serverSocket = new ServerSocket(porta);
        ativo = true;
        Thread aceitador = new Thread(this::aceitar, "rpc-aceitador");
        aceitador.start();
        return serverSocket.getLocalPort();
    }

    public void parar() throws IOException {
        ativo = false;
        serverSocket.close();
        pool.shutdownNow();
    }

    private void aceitar() {
        while (ativo) {
            try {
                Socket cliente = serverSocket.accept();
                pool.execute(() -> atender(cliente));
            } catch (IOException e) {
                if (ativo) {
                    System.err.println("Erro ao aceitar conexão: " + e);
                }
            }
        }
    }

    /** Laço de uma conexão: desempacota request -> executa -> empacota reply. */
    private void atender(Socket socket) {
        String origem = String.valueOf(socket.getRemoteSocketAddress());
        System.out.println("[" + Thread.currentThread().getName() + "] conexão de " + origem);
        try (Socket s = socket) {
            InputStream in = new BufferedInputStream(s.getInputStream());
            OutputStream out = new BufferedOutputStream(s.getOutputStream());
            byte[] quadro;
            while ((quadro = Empacotador.lerQuadro(in)) != null) {
                Requisicao requisicao = Empacotador.desempacotarRequisicao(quadro);   // servidor desempacota o request
                System.out.println("[" + Thread.currentThread().getName() + "] " + requisicao.getMetodo()
                        + " (id=" + requisicao.getId() + ")");
                Resposta resposta = executar(requisicao);
                Empacotador.escreverQuadro(out, Empacotador.empacotarResposta(resposta)); // servidor empacota o reply
            }
        } catch (IOException e) {
            System.err.println("[" + Thread.currentThread().getName() + "] erro com " + origem + ": " + e);
        }
        System.out.println("[" + Thread.currentThread().getName() + "] conexão encerrada: " + origem);
    }

    /** Despacha o método remoto solicitado para a classe de modelo {@link ServicoVotacao}. */
    private Resposta executar(Requisicao req) {
        Object[] a = req.getArgumentos();
        try {
            switch (req.getMetodo()) {
                case "listarCandidatos":
                    return Resposta.sucesso(req.getId(), servico.listarCandidatos().toArray(new Candidato[0]));
                case "votar": {
                    Usuario usuario = servico.autenticar((String) a[0], (String) a[1]);
                    Voto voto = servico.votar(usuario, (Integer) a[2]);
                    return Resposta.sucesso(req.getId(), voto.getComprovante());
                }
                case "apurar":
                    return Resposta.sucesso(req.getId(), servico.apurar());
                case "tempoRestante":
                    return Resposta.sucesso(req.getId(), servico.tempoRestanteMs());
                default:
                    return Resposta.erro(req.getId(), "Método remoto desconhecido: " + req.getMetodo());
            }
        } catch (ServicoException e) {
            return Resposta.erro(req.getId(), e.getMessage());
        } catch (ClassCastException | ArrayIndexOutOfBoundsException e) {
            return Resposta.erro(req.getId(), "Argumentos inválidos para o método " + req.getMetodo());
        }
    }

    public static void main(String[] args) throws Exception {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : PORTA_PADRAO;
        int duracao = args.length > 1 ? Integer.parseInt(args[1]) : 60;
        RepositorioEleicao repo;
        try {
            repo = CarregadorDados.carregar(Paths.get("dados"), duracao);
        } catch (IOException e) {
            System.err.println("Erro ao carregar a pasta 'dados': " + e.getMessage());
            System.exit(1);
            return;
        }
        ServidorRPC servidor = new ServidorRPC(new ServicoVotacao(repo));
        servidor.iniciar(porta);
        System.out.println("Servidor RPC de votação na porta " + porta + "; votação aberta por " + duracao + " s.");
    }
}
