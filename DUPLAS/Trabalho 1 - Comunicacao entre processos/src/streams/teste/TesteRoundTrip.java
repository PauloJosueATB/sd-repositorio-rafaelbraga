package streams.teste;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import modelo.Candidato;
import streams.EleicaoInputStream;
import streams.EleicaoOutputStream;

/**
 * Teste automatizado (sem framework): escreve com EleicaoOutputStream e lê de volta com
 * EleicaoInputStream usando memória, arquivo e TCP (loopback).
 *
 * <pre>java streams.teste.TesteRoundTrip</pre>
 */
public class TesteRoundTrip {
    private static int falhas = 0;

    public static void main(String[] args) throws Exception {
        Candidato[] todos = Amostra.candidatos();
        Candidato[] esperado = Arrays.copyOf(todos, Amostra.QUANTIDADE_ENVIADA);

        // 1) memória
        ByteArrayOutputStream memoria = new ByteArrayOutputStream();
        EleicaoOutputStream out = new EleicaoOutputStream(todos, Amostra.QUANTIDADE_ENVIADA, memoria);
        verificar(out.getBytesEnviados() == memoria.size(), "getBytesEnviados() coincide com o total de bytes escritos");
        Candidato[] lidos = new EleicaoInputStream(new ByteArrayInputStream(memoria.toByteArray())).lerCandidatos();
        verificar(Arrays.equals(esperado, lidos), "ida e volta em memória (apenas 3 dos 5 objetos)");

        // 2) arquivo
        File tmp = File.createTempFile("candidatos", ".bin");
        tmp.deleteOnExit();
        try (FileOutputStream f = new FileOutputStream(tmp);
             EleicaoOutputStream o = new EleicaoOutputStream(todos, todos.length, f)) {
            o.flush(); // os dados já foram enviados no construtor
        }
        try (EleicaoInputStream in = new EleicaoInputStream(new FileInputStream(tmp))) {
            verificar(Arrays.equals(todos, in.lerCandidatos()), "ida e volta por arquivo (5 objetos)");
        }

        // 3) TCP (loopback)
        try (ServerSocket servidor = new ServerSocket(0)) {
            AtomicReference<Candidato[]> recebido = new AtomicReference<>();
            Thread t = new Thread(() -> {
                try (Socket s = servidor.accept();
                     EleicaoInputStream in = new EleicaoInputStream(s.getInputStream())) {
                    recebido.set(in.lerCandidatos());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.start();
            try (Socket s = new Socket("localhost", servidor.getLocalPort());
                 EleicaoOutputStream o = new EleicaoOutputStream(todos, 4, s.getOutputStream())) {
                o.flush(); // os dados já foram enviados no construtor
            }
            t.join(5000);
            verificar(Arrays.equals(Arrays.copyOf(todos, 4), recebido.get()), "ida e volta por TCP (4 objetos)");
        }

        // 4) quantidade zero e acentuação
        ByteArrayOutputStream m2 = new ByteArrayOutputStream();
        new EleicaoOutputStream(todos, 0, m2);
        verificar(new EleicaoInputStream(new ByteArrayInputStream(m2.toByteArray())).lerCandidatos().length == 0,
                "quantidade = 0");
        Candidato acento = new Candidato(7, "José da Conceição", "União Ação");
        ByteArrayOutputStream m3 = new ByteArrayOutputStream();
        new EleicaoOutputStream(new Candidato[] { acento }, 1, m3);
        verificar(acento.equals(new EleicaoInputStream(new ByteArrayInputStream(m3.toByteArray())).lerCandidatos()[0]),
                "acentos preservados (UTF-8)");

        // 5) parâmetros inválidos
        try {
            new EleicaoOutputStream(todos, 99, new ByteArrayOutputStream());
            verificar(false, "quantidade maior que o array deve falhar");
        } catch (IllegalArgumentException e) {
            verificar(true, "quantidade maior que o array é rejeitada");
        }

        // 6) fluxo truncado
        byte[] inteiro = memoria.toByteArray();
        try {
            new EleicaoInputStream(new ByteArrayInputStream(Arrays.copyOf(inteiro, inteiro.length - 5))).lerCandidatos();
            verificar(false, "fluxo truncado deve lançar EOFException");
        } catch (EOFException e) {
            verificar(true, "fluxo truncado lança EOFException");
        }

        // 7) o prefixo de tamanho permite ignorar atributos extras (compatibilidade)
        ByteArrayOutputStream m4 = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(m4);
        d.writeInt(1);
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        DataOutputStream od = new DataOutputStream(obj);
        od.writeInt(9);
        od.writeUTF("Fulano");
        od.writeUTF("Partido X");
        od.writeInt(12345); // atributo extra desconhecido
        d.writeInt(obj.size());
        d.write(obj.toByteArray());
        Candidato lido = new EleicaoInputStream(new ByteArrayInputStream(m4.toByteArray())).lerCandidatos()[0];
        verificar(lido.equals(new Candidato(9, "Fulano", "Partido X")), "atributos extras são ignorados graças ao tamanho do objeto");

        System.out.println(falhas == 0 ? "\nTodos os testes passaram." : "\n" + falhas + " teste(s) FALHARAM.");
        System.exit(falhas == 0 ? 0 : 1);
    }

    private static void verificar(boolean condicao, String descricao) {
        System.out.println((condicao ? "[OK]   " : "[FALHA] ") + descricao);
        if (!condicao) {
            falhas++;
        }
    }
}
