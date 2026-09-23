package votacao.cliente;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import modelo.NotaInformativa;
import votacao.protocolo.Conversores;
import votacao.protocolo.Mensagem;
import votacao.protocolo.Protocolo;
import votacao.protocolo.RedeUtil;

/**
 * Thread que participa do grupo multicast (UDP) e entrega cada nota informativa
 * recebida ao ouvinte informado. Vários clientes na mesma máquina podem escutar a mesma porta.
 */
public final class ReceptorMulticast extends Thread {
    private final String grupo;
    private final int porta;
    private final Consumer<NotaInformativa> ouvinte;
    private MulticastSocket socket;
    private InetSocketAddress endereco;
    private NetworkInterface interfaceRede;
    private volatile boolean ativo = true;

    public ReceptorMulticast(String grupo, int porta, Consumer<NotaInformativa> ouvinte) {
        super("receptor-multicast");
        setDaemon(true);
        this.grupo = grupo;
        this.porta = porta;
        this.ouvinte = ouvinte;
    }

    /** Entra no grupo multicast e começa a escutar. Lança IOException se a rede não permitir. */
    public void entrar() throws IOException {
        endereco = new InetSocketAddress(InetAddress.getByName(grupo), porta);
        interfaceRede = RedeUtil.interfaceMulticast();
        socket = new MulticastSocket(porta);
        socket.joinGroup(endereco, interfaceRede);
        start();
    }

    @Override
    public void run() {
        byte[] buffer = new byte[16 * 1024];
        while (ativo) {
            try {
                DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                socket.receive(pacote);
                String json = new String(pacote.getData(), 0, pacote.getLength(), StandardCharsets.UTF_8);
                Mensagem m = Mensagem.doJson(json);                      // desempacota o datagrama
                if (Protocolo.TIPO_NOTA.equals(m.getString("tipo"))) {
                    ouvinte.accept(Conversores.nota(m));
                }
            } catch (IOException e) {
                if (ativo) {
                    System.err.println("Receptor multicast interrompido: " + e.getMessage());
                }
                return;
            } catch (IllegalArgumentException ignorado) {
                // datagrama que não é JSON válido: descarta
            }
        }
    }

    public void encerrar() {
        ativo = false;
        if (socket != null) {
            try {
                socket.leaveGroup(endereco, interfaceRede);
            } catch (IOException ignorado) {
                // encerrando
            }
            socket.close();
        }
    }
}
