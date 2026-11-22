package filas;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Map;
import protocolo.Json;

/** Cliente mínimo do protocolo TCP/JSON do broker. */
public final class ClienteFilas {
    private final String host;
    private final int porta;

    public ClienteFilas(String host, int porta) {
        this.host = host;
        this.porta = porta;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> executar(Map<String, Object> comando) throws Exception {
        try (Socket socket = new Socket(host, porta);
             DataOutputStream saida = new DataOutputStream(socket.getOutputStream());
             DataInputStream entrada = new DataInputStream(socket.getInputStream())) {
            saida.writeUTF(Json.escrever(comando));
            saida.flush();
            return (Map<String, Object>) Json.ler(entrada.readUTF());
        }
    }
}
