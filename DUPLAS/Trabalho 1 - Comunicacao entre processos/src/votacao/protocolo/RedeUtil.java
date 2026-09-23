package votacao.protocolo;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Escolhe a interface de rede usada pelos sockets multicast (UDP).
 *
 * <p>Ordem de escolha: (1) propriedade {@code -Dmulticast.interface=<nome>} (ex.: eth0, wlan0, en0);
 * (2) primeira interface ativa, com suporte a multicast, não-loopback e com endereço IPv4;
 * (3) {@code null}, deixando a JVM usar a interface padrão.
 */
public final class RedeUtil {
    private RedeUtil() {
    }

    public static NetworkInterface interfaceMulticast() {
        try {
            String nome = System.getProperty("multicast.interface");
            if (nome != null && !nome.isEmpty()) {
                NetworkInterface ni = NetworkInterface.getByName(nome);
                if (ni == null) {
                    System.err.println("Aviso: interface '" + nome + "' não encontrada; usando a padrão.");
                }
                return ni;
            }
            Enumeration<NetworkInterface> todas = NetworkInterface.getNetworkInterfaces();
            if (todas == null) {
                return null;
            }
            for (NetworkInterface ni : Collections.list(todas)) {
                if (ni.isUp() && ni.supportsMulticast() && !ni.isLoopback() && temIPv4(ni)) {
                    return ni;
                }
            }
        } catch (Exception e) {
            System.err.println("Aviso: não foi possível escolher a interface multicast: " + e);
        }
        return null;
    }

    private static boolean temIPv4(NetworkInterface ni) {
        for (InetAddress a : Collections.list(ni.getInetAddresses())) {
            if (a instanceof Inet4Address) {
                return true;
            }
        }
        return false;
    }
}
