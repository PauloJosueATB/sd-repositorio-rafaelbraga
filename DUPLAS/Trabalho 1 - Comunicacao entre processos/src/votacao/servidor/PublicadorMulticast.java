package votacao.servidor;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;

import modelo.NotaInformativa;
import votacao.protocolo.Conversores;
import votacao.protocolo.RedeUtil;

/**
 * Envia as notas informativas dos administradores ao grupo multicast usando sockets UDP.
 * É o ÚNICO ponto do sistema que usa multicast.
 */
public class PublicadorMulticast implements Closeable {
    private final InetAddress grupo;
    private final int porta;
    private final MulticastSocket socket;

    public PublicadorMulticast(String enderecoGrupo, int porta) throws IOException {
        this.grupo = InetAddress.getByName(enderecoGrupo);
        if (!grupo.isMulticastAddress()) {
            throw new IOException(enderecoGrupo + " não é um endereço multicast (use 224.0.0.0 a 239.255.255.255).");
        }
        this.porta = porta;
        this.socket = new MulticastSocket();
        this.socket.setTimeToLive(4);
        NetworkInterface ni = RedeUtil.interfaceMulticast();
        if (ni != null) {
            this.socket.setNetworkInterface(ni);
        }
    }

    public String getGrupo() {
        return grupo.getHostAddress();
    }

    public int getPorta() {
        return porta;
    }

    /** Empacota a nota em JSON e a envia em um único datagrama UDP para o grupo. */
    public synchronized void publicar(NotaInformativa nota) throws IOException {
        byte[] dados = Conversores.paraMensagem(nota).paraJson().getBytes(StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(dados, dados.length, grupo, porta));
    }

    @Override
    public void close() {
        socket.close();
    }
}
