package streams;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import modelo.Candidato;

/**
 * QUESTÃO 3 - Subclasse de {@link InputStream} que lê os dados gerados por
 * {@link EleicaoOutputStream} (ver o formato descrito naquela classe).
 *
 * <p>A leitura é feita sem buffer próprio: apenas os bytes necessários são consumidos
 * do stream de origem. Assim o mesmo stream pode continuar sendo usado depois
 * (por exemplo, dentro de uma mensagem maior da Questão 4).
 */
public class EleicaoInputStream extends InputStream {
    private static final int MAX_OBJETOS = 1_000_000;
    private static final int MAX_BYTES_POR_OBJETO = 1 << 20;

    private final InputStream origem;

    /** @param origem stream de onde as sequências de bytes serão lidas (System.in, arquivo, socket...) */
    public EleicaoInputStream(InputStream origem) {
        if (origem == null) {
            throw new IllegalArgumentException("O InputStream de origem não pode ser nulo.");
        }
        this.origem = origem;
    }

    /**
     * Lê do stream de origem um conjunto completo de candidatos.
     *
     * @throws java.io.EOFException se o stream terminar antes do fim dos dados
     * @throws IOException          se os bytes lidos não seguirem o formato esperado
     */
    public Candidato[] lerCandidatos() throws IOException {
        DataInputStream entrada = new DataInputStream(origem);
        int quantidade = entrada.readInt();
        if (quantidade < 0 || quantidade > MAX_OBJETOS) {
            throw new IOException("Quantidade de objetos inválida: " + quantidade);
        }
        Candidato[] candidatos = new Candidato[quantidade];
        for (int i = 0; i < quantidade; i++) {
            int tamanho = entrada.readInt();           // nº de bytes do objeto
            if (tamanho < 0 || tamanho > MAX_BYTES_POR_OBJETO) {
                throw new IOException("Tamanho de objeto inválido: " + tamanho);
            }
            byte[] atributos = new byte[tamanho];
            entrada.readFully(atributos);
            candidatos[i] = desempacotar(atributos);
        }
        return candidatos;
    }

    private static Candidato desempacotar(byte[] atributos) throws IOException {
        DataInputStream d = new DataInputStream(new ByteArrayInputStream(atributos));
        int numero = d.readInt();
        String nome = d.readUTF();
        String partido = d.readUTF();
        // Bytes que sobrarem em 'atributos' (campos de versões futuras) são ignorados.
        return new Candidato(numero, nome, partido);
    }

    @Override
    public int read() throws IOException {
        return origem.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return origem.read(b, off, len);
    }

    @Override
    public int available() throws IOException {
        return origem.available();
    }

    @Override
    public void close() throws IOException {
        origem.close();
    }
}
