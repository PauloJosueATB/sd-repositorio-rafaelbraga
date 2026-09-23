package votacao.servidor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import modelo.Candidato;
import modelo.Perfil;
import modelo.Resultado;
import modelo.ServicoAdministracao;
import modelo.ServicoException;
import modelo.ServicoVotacao;
import modelo.NotaInformativa;
import modelo.Usuario;
import modelo.Voto;
import votacao.protocolo.Conversores;
import votacao.protocolo.Mensagem;
import votacao.protocolo.Protocolo;

/**
 * Atende UMA conexão TCP de cliente (executada em uma thread própria do servidor).
 * Lê uma requisição JSON por linha, executa a operação nas classes de modelo e
 * responde com uma linha JSON. A sessão (usuário autenticado) vive enquanto a conexão durar.
 */
public class AtendenteCliente implements Runnable {
    private static final int TIMEOUT_OCIOSO_MS = 10 * 60 * 1000;

    private final Socket socket;
    private final ServidorVotacao servidor;
    private final ServicoVotacao votacao;
    private final ServicoAdministracao administracao;
    private Usuario usuario; // null até o login ser feito com sucesso

    public AtendenteCliente(Socket socket, ServidorVotacao servidor) {
        this.socket = socket;
        this.servidor = servidor;
        this.votacao = servidor.getServicoVotacao();
        this.administracao = servidor.getServicoAdministracao();
    }

    @Override
    public void run() {
        String origem = String.valueOf(socket.getRemoteSocketAddress());
        servidor.log("conexão aceita de " + origem);
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8))) {
            s.setSoTimeout(TIMEOUT_OCIOSO_MS);
            String linha;
            while ((linha = in.readLine()) != null) {
                Mensagem resposta;
                boolean sair = false;
                try {
                    Mensagem requisicao = Mensagem.doJson(linha);                 // servidor desempacota o request
                    sair = Protocolo.OP_SAIR.equals(requisicao.getString("op"));
                    resposta = processar(requisicao);
                } catch (IllegalArgumentException e) {
                    resposta = Mensagem.erro("Mensagem inválida: " + e.getMessage());
                }
                out.write(resposta.paraJson());                                     // servidor empacota o reply
                out.write('\n');
                out.flush();
                if (sair) {
                    break;
                }
            }
        } catch (IOException e) {
            servidor.log("conexão com " + origem + " interrompida: " + e.getMessage());
        }
        servidor.log("conexão encerrada: " + origem + (usuario != null ? " (" + usuario.getLogin() + ")" : ""));
    }

    private Mensagem processar(Mensagem req) {
        String op = req.getString("op");
        if (op == null) {
            return Mensagem.erro("Campo 'op' ausente.");
        }
        try {
            if (Protocolo.OP_LOGIN.equals(op)) {
                return login(req);
            }
            if (Protocolo.OP_SAIR.equals(op)) {
                return Mensagem.ok();
            }
            if (usuario == null) {
                return Mensagem.erro("Faça login antes de usar o sistema.");
            }
            switch (op) {
                case Protocolo.OP_LISTAR_CANDIDATOS:
                    return Mensagem.ok().com("candidatos", Conversores.paraLista(votacao.listarCandidatos()));
                case Protocolo.OP_STATUS:
                    return status();
                case Protocolo.OP_VOTAR: {
                    if (!req.tem("candidato")) {
                        return Mensagem.erro("Campo 'candidato' ausente.");
                    }
                    Voto voto = votacao.votar(usuario, req.getInt("candidato", -1));
                    servidor.log("voto registrado de " + usuario.getLogin());   // não registra em quem votou (sigilo)
                    return Mensagem.ok().com("comprovante", voto.getComprovante());
                }
                case Protocolo.OP_RESULTADO: {
                    Resultado r = votacao.apurar();
                    return Mensagem.ok().com("resultado", Conversores.paraMapa(r));
                }
                case Protocolo.OP_ADICIONAR_CANDIDATO: {
                    Candidato c = administracao.adicionarCandidato(usuario, req.getInt("numero", -1),
                            req.getString("nome"), req.getString("partido"));
                    servidor.log(usuario.getLogin() + " adicionou o candidato " + c);
                    return Mensagem.ok().com("candidato", Conversores.paraMapa(c));
                }
                case Protocolo.OP_REMOVER_CANDIDATO: {
                    Candidato c = administracao.removerCandidato(usuario, req.getInt("numero", -1));
                    servidor.log(usuario.getLogin() + " removeu o candidato " + c);
                    return Mensagem.ok();
                }
                case Protocolo.OP_ENVIAR_NOTA:
                    return enviarNota(req);
                default:
                    return Mensagem.erro("Operação desconhecida: " + op);
            }
        } catch (ServicoException e) {
            return Mensagem.erro(e.getMessage());
        }
    }

    private Mensagem login(Mensagem req) throws ServicoException {
        Usuario autenticado = votacao.autenticar(req.getString("login"), req.getString("senha"));
        this.usuario = autenticado;
        servidor.log("login: " + autenticado);
        return status()
                .com("nome", autenticado.getNome())
                .com("perfil", autenticado.getPerfil().name())
                .com("candidatos", Conversores.paraLista(votacao.listarCandidatos()))
                .com("grupoMulticast", servidor.getPublicador().getGrupo())
                .com("portaMulticast", servidor.getPublicador().getPorta());
    }

    private Mensagem status() {
        return Mensagem.ok()
                .com("titulo", votacao.getEleicao().getTitulo())
                .com("votacaoAberta", votacao.votacaoAberta())
                .com("tempoRestanteMs", votacao.tempoRestanteMs());
    }

    /** O administrador envia a nota por TCP; o servidor a valida e a retransmite por UDP multicast. */
    private Mensagem enviarNota(Mensagem req) throws ServicoException {
        NotaInformativa nota = administracao.criarNota(usuario, req.getString("texto"));
        try {
            servidor.getPublicador().publicar(nota);
        } catch (IOException e) {
            return Mensagem.erro("Falha ao enviar a nota por multicast: " + e.getMessage());
        }
        servidor.log("nota multicast de " + usuario.getLogin() + ": " + nota.getTexto());
        return Mensagem.ok();
    }
}
