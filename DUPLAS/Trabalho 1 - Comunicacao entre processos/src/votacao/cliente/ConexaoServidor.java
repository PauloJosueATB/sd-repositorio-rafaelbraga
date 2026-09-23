package votacao.cliente;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import votacao.protocolo.Mensagem;

/** Conexão TCP (unicast) com o servidor: envia uma requisição JSON por linha e aguarda a resposta. */
public class ConexaoServidor implements Closeable {
    private final Socket socket;
    private final BufferedReader entrada;
    private final BufferedWriter saida;

    public ConexaoServidor(String host, int porta) throws IOException {
        this.socket = new Socket(host, porta);
        this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.saida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    /** Empacota a requisição, envia, e desempacota a resposta do servidor. */
    public synchronized Mensagem enviar(Mensagem requisicao) throws IOException {
        saida.write(requisicao.paraJson());
        saida.write('\n');
        saida.flush();
        String linha = entrada.readLine();
        if (linha == null) {
            throw new EOFException("O servidor encerrou a conexão.");
        }
        try {
            return Mensagem.doJson(linha);
        } catch (IllegalArgumentException e) {
            throw new IOException("Resposta inválida do servidor: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
