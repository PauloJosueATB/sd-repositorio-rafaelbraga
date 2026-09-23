package streams.teste;

import java.io.FileOutputStream;

import streams.EleicaoOutputStream;

/**
 * QUESTÃO 2 (b-ii): destino = arquivo (FileOutputStream).
 *
 * <pre>java streams.teste.TesteSaidaArquivo [arquivo]     # padrão: candidatos.bin</pre>
 */
public class TesteSaidaArquivo {
    public static void main(String[] args) throws Exception {
        String caminho = args.length > 0 ? args[0] : "candidatos.bin";
        try (FileOutputStream arquivo = new FileOutputStream(caminho);
             EleicaoOutputStream stream = new EleicaoOutputStream(
                     Amostra.candidatos(), Amostra.QUANTIDADE_ENVIADA, arquivo)) {
            System.out.println("Gravados " + Amostra.QUANTIDADE_ENVIADA + " candidatos ("
                    + stream.getBytesEnviados() + " bytes) em " + caminho);
        }
    }
}
