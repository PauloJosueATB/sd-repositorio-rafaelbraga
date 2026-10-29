package rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import modelo.Candidato;
import modelo.Resultado;
import modelo.ServicoAdministracao;
import modelo.ServicoException;
import modelo.ServicoVotacao;
import modelo.Usuario;
import modelo.Voto;
import protocolo.Conversores;

/**
 * Objeto remoto do servidor. Estende {@link UnicastRemoteObject} ("é-um" objeto
 * remoto gerenciado pela Java RMI, que cria o stub/skeleton e cuida do transporte
 * sem que nós abramos nenhum socket) e implementa {@link ComunicadorRemoto}.
 * <p>
 * Faz o papel de DISPATCHER + SKELETON + procedimentos de serviço da Figura 5.4:
 * recebe o {@code methodId} e os argumentos já entregues pelo runtime RMI,
 * decide (dispatch) qual operação do {@link ServicoVotacao}/{@link ServicoAdministracao}
 * chamar, e devolve a resposta empacotada — a própria RMI se encarrega de levá-la
 * de volta ao {@link Requisitor} do cliente.
 * <p>
 * Como o protocolo requisição-resposta descrito no enunciado é sem estado
 * (cada chamada é independente), a autenticação feita em {@link OperacoesRemotas#AUTENTICAR}
 * devolve um token de sessão; as demais operações precisam apresentá-lo. É o
 * servidor — nunca o cliente — quem decide o perfil (eleitor/administrador)
 * associado a cada token, o que impede um cliente malicioso de se autopromover
 * a administrador.
 */
public class Invocador extends UnicastRemoteObject implements ComunicadorRemoto {
    private final ServicoVotacao servicoVotacao;
    private final ServicoAdministracao servicoAdministracao;
    private final Map<String, Usuario> sessoes = new ConcurrentHashMap<>();

    public Invocador(ServicoVotacao servicoVotacao, ServicoAdministracao servicoAdministracao) throws RemoteException {
        super();
        this.servicoVotacao = servicoVotacao;
        this.servicoAdministracao = servicoAdministracao;
    }

    @Override
    public byte[] invocarRemota(int methodId, byte[] argumentos) throws RemoteException {
        byte[] requisicao = getRequest(argumentos);
        byte[] resposta;
        try {
            resposta = despachar(methodId, requisicao);
        } catch (ServicoException e) {
            resposta = Empacotador.empacotarErro(e.getMessage());
        } catch (RuntimeException e) {
            resposta = Empacotador.empacotarErro("Erro inesperado no servidor: " + e.getMessage());
        }
        return sendReply(resposta);
    }

    /**
     * Papel do módulo de comunicação do servidor que "obtém a requisição de um
     * cliente" (Figura 5.4). Na Java RMI esse transporte já aconteceu quando este
     * método é chamado — aqui só explicitamos o papel, sem criar nenhum socket.
     */
    private byte[] getRequest(byte[] argumentos) {
        return argumentos;
    }

    /**
     * Papel do módulo de comunicação que "envia a resposta ao cliente" (Figura 5.4).
     * A Java RMI entrega este valor de retorno ao stub que está bloqueado em
     * {@link Requisitor#doOperation}, sem precisarmos endereçar IP/porta manualmente.
     */
    private byte[] sendReply(byte[] resposta) {
        return resposta;
    }

    // ------------------------------------------------------------ dispatcher

    private byte[] despachar(int methodId, byte[] corpo) throws ServicoException {
        Map<String, Object> args = Empacotador.desempacotar(corpo);
        switch (methodId) {
            case OperacoesRemotas.AUTENTICAR:
                return autenticar(args);
            case OperacoesRemotas.LISTAR_CANDIDATOS:
                return listarCandidatos(args);
            case OperacoesRemotas.VOTAR:
                return votar(args);
            case OperacoesRemotas.APURAR:
                return apurar(args);
            case OperacoesRemotas.TEMPO_RESTANTE_MS:
                return tempoRestante(args);
            case OperacoesRemotas.VOTACAO_ABERTA:
                return votacaoAberta(args);
            case OperacoesRemotas.ADICIONAR_CANDIDATO:
                return adicionarCandidato(args);
            case OperacoesRemotas.REMOVER_CANDIDATO:
                return removerCandidato(args);
            case OperacoesRemotas.SAIR:
                return sair(args);
            default:
                throw new ServicoException("Método remoto desconhecido: " + OperacoesRemotas.nome(methodId));
        }
    }

    // ------------------------------------------------------------ operações

    private byte[] autenticar(Map<String, Object> args) throws ServicoException {
        String login = Empacotador.string(args, "login");
        String senha = Empacotador.string(args, "senha");
        Usuario usuario = servicoVotacao.autenticar(login, senha);
        String token = UUID.randomUUID().toString();
        sessoes.put(token, usuario);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("token", token);
        resp.put("usuario", Conversores.deUsuario(usuario));
        return Empacotador.empacotarOk(resp);
    }

    private byte[] listarCandidatos(Map<String, Object> args) throws ServicoException {
        exigirSessao(args);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("candidatos", Conversores.deCandidatos(servicoVotacao.listarCandidatos()));
        return Empacotador.empacotarOk(resp);
    }

    private byte[] votar(Map<String, Object> args) throws ServicoException {
        Usuario usuario = exigirSessao(args);
        int numero = Empacotador.inteiro(args, "numero");
        Voto voto = servicoVotacao.votar(usuario, numero);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("comprovante", voto.getComprovante());
        return Empacotador.empacotarOk(resp);
    }

    private byte[] apurar(Map<String, Object> args) throws ServicoException {
        exigirSessao(args);
        Resultado r = servicoVotacao.apurar();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("resultado", Conversores.deResultado(r));
        return Empacotador.empacotarOk(resp);
    }

    private byte[] tempoRestante(Map<String, Object> args) throws ServicoException {
        exigirSessao(args);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("tempoRestanteMs", servicoVotacao.tempoRestanteMs());
        return Empacotador.empacotarOk(resp);
    }

    private byte[] votacaoAberta(Map<String, Object> args) throws ServicoException {
        exigirSessao(args);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("aberta", servicoVotacao.votacaoAberta());
        return Empacotador.empacotarOk(resp);
    }

    private byte[] adicionarCandidato(Map<String, Object> args) throws ServicoException {
        Usuario usuario = exigirSessao(args);
        int numero = Empacotador.inteiro(args, "numero");
        String nome = Empacotador.string(args, "nome");
        String partido = Empacotador.string(args, "partido");
        Candidato c = servicoAdministracao.adicionarCandidato(usuario, numero, nome, partido);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("candidato", Conversores.deCandidato(c));
        return Empacotador.empacotarOk(resp);
    }

    private byte[] removerCandidato(Map<String, Object> args) throws ServicoException {
        Usuario usuario = exigirSessao(args);
        int numero = Empacotador.inteiro(args, "numero");
        Candidato removido = servicoAdministracao.removerCandidato(usuario, numero);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("candidato", Conversores.deCandidato(removido));
        return Empacotador.empacotarOk(resp);
    }

    private byte[] sair(Map<String, Object> args) {
        String token = Empacotador.string(args, "token");
        if (token != null) {
            sessoes.remove(token);
        }
        return Empacotador.empacotarOk(new LinkedHashMap<>());
    }

    private Usuario exigirSessao(Map<String, Object> args) throws ServicoException {
        String token = Empacotador.string(args, "token");
        Usuario u = token == null ? null : sessoes.get(token);
        if (u == null) {
            throw new ServicoException("Sessão inválida ou expirada. Faça login novamente.");
        }
        return u;
    }
}
