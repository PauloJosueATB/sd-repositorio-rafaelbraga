package streams.teste;

import modelo.Candidato;
import streams.EleicaoInputStream;

/**
 * QUESTÃO 3 (b): origem = entrada padrão (System.in). Os bytes devem vir de um pipe ou redirecionamento:
 *
 * <pre>
 *   java streams.teste.TesteSaidaPadrao | java streams.teste.TesteEntradaPadrao
 *   java streams.teste.TesteEntradaPadrao &lt; candidatos.bin
 * </pre>
 */
public class TesteEntradaPadrao {
    public static void main(String[] args) throws Exception {
        EleicaoInputStream stream = new EleicaoInputStream(System.in);
        Candidato[] recebidos = stream.lerCandidatos();
        System.out.println("Lidos da entrada padrão: " + recebidos.length + " candidato(s)");
        for (Candidato c : recebidos) {
            System.out.println("  " + c);
        }
    }
}
