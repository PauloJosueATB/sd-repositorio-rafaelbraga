package votacao.cliente;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

import modelo.Candidato;
import modelo.Formatadores;
import modelo.NotaInformativa;
import modelo.Perfil;
import votacao.protocolo.Conversores;
import votacao.protocolo.Mensagem;
import votacao.protocolo.Protocolo;

/** Funcionalidades comuns aos clientes de terminal (eleitor e administrador). */
public abstract class ClienteBase implements Closeable {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    protected final ConexaoServidor conexao;
    protected final Scanner teclado = new Scanner(System.in, "UTF-8");
    private ReceptorMulticast receptor;

    protected ClienteBase(String host, int porta) throws IOException {
        this.conexao = new ConexaoServidor(host, porta);
    }

    /** Roda o cliente completo: login + menu. */
    public abstract void executar() throws IOException;

    /** Login com até 3 tentativas. Só aceita usuários do perfil esperado. */
    protected boolean autenticar(Perfil esperado) throws IOException {
        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            String login = perguntar("Login: ");
            String senha = login == null ? null : perguntar("Senha: ");
            if (senha == null) {
                return false;
            }
            Mensagem resposta = conexao.enviar(Mensagem.requisicao(Protocolo.OP_LOGIN)
                    .com("login", login).com("senha", senha));
            if (!resposta.isOk()) {
                System.out.println("Falha no login: " + resposta.getString("erro"));
                continue;
            }
            Perfil perfil = Perfil.valueOf(resposta.getString("perfil"));
            if (perfil != esperado) {
                System.out.println("Este programa é para o perfil " + esperado + ", mas '" + login + "' é " + perfil + ".");
                return false;
            }
            System.out.println("\nBem-vindo(a), " + resposta.getString("nome") + "!");
            System.out.println(resposta.getString("titulo"));
            mostrarTempo(resposta);
            System.out.println("Candidatos em votação:");
            mostrarCandidatos(Conversores.candidatos(resposta.getLista("candidatos")));
            entrarNoGrupoMulticast(resposta.getString("grupoMulticast"), resposta.getInt("portaMulticast", 0));
            return true;
        }
        System.out.println("Número máximo de tentativas excedido.");
        return false;
    }

    private void entrarNoGrupoMulticast(String grupo, int porta) {
        receptor = new ReceptorMulticast(grupo, porta, this::mostrarNota);
        try {
            receptor.entrar();
            System.out.println("(Ouvindo notas informativas no grupo multicast " + grupo + ":" + porta + ")");
        } catch (IOException | RuntimeException e) {
            receptor = null;
            System.out.println("Aviso: não foi possível entrar no grupo multicast (" + e.getMessage()
                    + "). Você não receberá as notas informativas.");
        }
    }

    private void mostrarNota(NotaInformativa nota) {
        System.out.println("\n>>> NOTA INFORMATIVA de " + nota.getAutor() + " ("
                + HORA.format(Instant.ofEpochMilli(nota.getInstante())) + "): " + nota.getTexto());
    }

    protected void listarCandidatos() throws IOException {
        Mensagem r = conexao.enviar(Mensagem.requisicao(Protocolo.OP_LISTAR_CANDIDATOS));
        if (r.isOk()) {
            mostrarCandidatos(Conversores.candidatos(r.getLista("candidatos")));
        } else {
            System.out.println("Erro: " + r.getString("erro"));
        }
    }

    protected void mostrarCandidatos(List<Candidato> candidatos) {
        if (candidatos.isEmpty()) {
            System.out.println("  (nenhum candidato cadastrado)");
        }
        for (Candidato c : candidatos) {
            System.out.println("  " + c);
        }
    }

    protected void mostrarStatus() throws IOException {
        Mensagem r = conexao.enviar(Mensagem.requisicao(Protocolo.OP_STATUS));
        if (r.isOk()) {
            mostrarTempo(r);
        } else {
            System.out.println("Erro: " + r.getString("erro"));
        }
    }

    private void mostrarTempo(Mensagem r) {
        if (r.getBoolean("votacaoAberta", false)) {
            System.out.println("Votação ABERTA. Tempo restante: " + Formatadores.duracao(r.getLong("tempoRestanteMs", 0)));
        } else {
            System.out.println("Votação ENCERRADA.");
        }
    }

    protected void mostrarResultado() throws IOException {
        Mensagem r = conexao.enviar(Mensagem.requisicao(Protocolo.OP_RESULTADO));
        if (r.isOk()) {
            System.out.println(Formatadores.resultado(Conversores.resultado(r.getMapa("resultado"))));
        } else {
            System.out.println("Erro: " + r.getString("erro"));
        }
    }

    /** Exibe o prompt e lê uma linha; devolve null se a entrada terminou (EOF). */
    protected String perguntar(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        return teclado.hasNextLine() ? teclado.nextLine().trim() : null;
    }

    /** Lê um inteiro; devolve null se a entrada terminou e -1 se o texto não for um número. */
    protected Integer perguntarInt(String prompt) {
        String texto = perguntar(prompt);
        if (texto == null) {
            return null;
        }
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    protected void sair() {
        try {
            conexao.enviar(Mensagem.requisicao(Protocolo.OP_SAIR));
        } catch (IOException ignorado) {
            // o servidor já pode ter fechado a conexão
        }
    }

    @Override
    public void close() throws IOException {
        if (receptor != null) {
            receptor.encerrar();
        }
        conexao.close();
    }
}
