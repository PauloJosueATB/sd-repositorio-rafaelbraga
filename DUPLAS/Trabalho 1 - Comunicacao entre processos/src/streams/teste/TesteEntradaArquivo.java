package streams.teste;

import java.io.FileInputStream;

import modelo.Candidato;
import streams.EleicaoInputStream;

/**
 * QUESTÃO 3 (c): origem = arquivo (FileInputStream), gerado por {@link TesteSaidaArquivo}.
 *
 * <pre>java streams.teste.TesteEntradaArquivo [arquivo]     # padrão: candidatos.bin</pre>
 */
public class TesteEntradaArquivo {
    public static void main(String[] args) throws Exception {
        String caminho = args.length > 0 ? args[0] : "candidatos.bin";
        try (EleicaoInputStream stream = new EleicaoInputStream(new FileInputStream(caminho))) {
            Candidato[] recebidos = stream.lerCandidatos();
            System.out.println("Lidos de " + caminho + ": " + recebidos.length + " candidato(s)");
            for (Candidato c : recebidos) {
                System.out.println("  " + c);
            }
        }
    }
}
