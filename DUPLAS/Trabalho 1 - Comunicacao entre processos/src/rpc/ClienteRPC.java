package rpc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import modelo.Candidato;
import modelo.Resultado;
import modelo.ServicoException;

/**
 * QUESTÃO 4 - Cliente (proxy/stub) do serviço remoto. Cada método local empacota uma
 * requisição, envia pelo socket TCP, aguarda o quadro de resposta e o desempacota.
 */
public class ClienteRPC implements Closeable {
    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private int proximoId = 1;

    public ClienteRPC(String host, int porta) throws IOException {
        this.socket = new Socket(host, porta);
        this.in = new BufferedInputStream(socket.getInputStream());
        this.out = new BufferedOutputStream(socket.getOutputStream());
    }

    public List<Candidato> listarCandidatos() throws IOException, ServicoException {
        return Arrays.asList((Candidato[]) invocar("listarCandidatos"));
    }

    /** @return comprovante do voto */
    public String votar(String login, String senha, int numeroCandidato) throws IOException, ServicoException {
        return (String) invocar("votar", login, senha, numeroCandidato);
    }

    public Resultado apurar() throws IOException, ServicoException {
        return (Resultado) invocar("apurar");
    }

    public long tempoRestanteMs() throws IOException, ServicoException {
        return (Long) invocar("tempoRestante");
    }

    /** Executa uma chamada remota completa (request -> reply). */
    private synchronized Object invocar(String metodo, Object... argumentos) throws IOException, ServicoException {
        Requisicao requisicao = new Requisicao(proximoId++, metodo, argumentos);
        byte[] dados = Empacotador.empacotarRequisicao(requisicao);      // cliente empacota o request
        Empacotador.escreverQuadro(out, dados);

        byte[] quadro = Empacotador.lerQuadro(in);
        if (quadro == null) {
            throw new EOFException("O servidor encerrou a conexão.");
        }
        Resposta resposta = Empacotador.desempacotarResposta(quadro);    // cliente desempacota o reply
        if (resposta.getId() != requisicao.getId()) {
            throw new IOException("Resposta com id " + resposta.getId() + " para a requisição " + requisicao.getId());
        }
        if (!resposta.isSucesso()) {
            throw new ServicoException(resposta.getErro());
        }
        return resposta.getResultado();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
