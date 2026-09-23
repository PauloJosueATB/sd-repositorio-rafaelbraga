package streams.teste;

import java.io.OutputStream;

import streams.EleicaoOutputStream;

/**
 * QUESTÃO 2 (b-i): destino = saída padrão (System.out).
 *
 * <pre>
 *   java streams.teste.TesteSaidaPadrao            # bytes brutos (o terminal mostrará "lixo")
 *   java streams.teste.TesteSaidaPadrao --hex      # bytes em hexadecimal (legível)
 *   java streams.teste.TesteSaidaPadrao | java streams.teste.TesteEntradaPadrao   # Questão 3 (b)
 * </pre>
 * Mensagens de diagnóstico vão para System.err para não misturar com os bytes do stream.
 */
public class TesteSaidaPadrao {
    public static void main(String[] args) throws Exception {
        boolean hex = args.length > 0 && args[0].equals("--hex");
        OutputStream destino = hex ? new HexOutputStream(System.out) : System.out;

        EleicaoOutputStream stream = new EleicaoOutputStream(
                Amostra.candidatos(), Amostra.QUANTIDADE_ENVIADA, destino);
        stream.flush(); // não fechamos System.out
        System.err.println("Enviados " + Amostra.QUANTIDADE_ENVIADA + " de " + Amostra.candidatos().length
                + " candidatos (" + stream.getBytesEnviados() + " bytes) para System.out.");
    }
}
