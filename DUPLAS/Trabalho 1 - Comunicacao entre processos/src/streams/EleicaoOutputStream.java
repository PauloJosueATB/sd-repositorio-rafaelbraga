package streams;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import modelo.Candidato;

/**
 * QUESTÃO 2 - Subclasse de {@link OutputStream} derivada do POJO {@code Eleicao}.
 * Envia ao stream de destino os dados de um conjunto (array) de outro POJO ({@link Candidato}).
 *
 * <h3>Formato dos bytes enviados (big-endian, via DataOutputStream)</h3>
 * <pre>
 *   int   quantidade                 número de objetos que serão enviados
 *   para cada objeto:
 *     int   tamanho                  número de bytes usados para gravar os atributos
 *     byte[tamanho]                  atributos do objeto:
 *         int    numero
 *         UTF    nome                (2 bytes de tamanho + bytes UTF-8)
 *         UTF    partido             (2 bytes de tamanho + bytes UTF-8)
 * </pre>
 * O prefixo {@code tamanho} permite ao receptor delimitar cada objeto (e até ignorar
 * atributos extras de versões futuras).
 *
 * <p>Os dados são enviados já no construtor. Depois disso o objeto ainda pode ser usado
 * como um {@link OutputStream} comum: {@link #write(int)} apenas repassa bytes ao destino.
 */
public class EleicaoOutputStream extends OutputStream {
    private final OutputStream destino;
    private long bytesEnviados;

    /**
     * @param candidatos array com os objetos a transmitir
     * @param quantidade quantos objetos do array serão enviados (0 ... candidatos.length)
     * @param destino    stream de destino (System.out, FileOutputStream, socket TCP etc.)
     */
    public EleicaoOutputStream(Candidato[] candidatos, int quantidade, OutputStream destino) throws IOException {
        if (candidatos == null) {
            throw new IllegalArgumentException("O array de candidatos não pode ser nulo.");
        }
        if (destino == null) {
            throw new IllegalArgumentException("O OutputStream de destino não pode ser nulo.");
        }
        if (quantidade < 0 || quantidade > candidatos.length) {
            throw new IllegalArgumentException(
                    "quantidade deve estar entre 0 e " + candidatos.length + " (recebido: " + quantidade + ")");
        }
        this.destino = destino;
        enviar(candidatos, quantidade);
    }

    private void enviar(Candidato[] candidatos, int quantidade) throws IOException {
        DataOutputStream saida = new DataOutputStream(destino);
        saida.writeInt(quantidade);
        bytesEnviados += Integer.BYTES;
        for (int i = 0; i < quantidade; i++) {
            byte[] atributos = empacotar(candidatos[i]);
            saida.writeInt(atributos.length);          // nº de bytes usados para gravar o objeto
            saida.write(atributos);                    // os atributos propriamente ditos
            bytesEnviados += Integer.BYTES + atributos.length;
        }
        saida.flush();
    }

    /** Grava os atributos do candidato (número, nome e partido) em um vetor de bytes. */
    private static byte[] empacotar(Candidato c) throws IOException {
        if (c == null) {
            throw new IllegalArgumentException("O array contém candidato nulo.");
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(buffer);
        d.writeInt(c.getNumero());
        d.writeUTF(c.getNome() == null ? "" : c.getNome());
        d.writeUTF(c.getPartido() == null ? "" : c.getPartido());
        d.flush();
        return buffer.toByteArray();
    }

    /** Total de bytes escritos no destino por este stream (cabeçalho + objetos + repasses). */
    public long getBytesEnviados() {
        return bytesEnviados;
    }

    @Override
    public void write(int b) throws IOException {
        destino.write(b);
        bytesEnviados++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        destino.write(b, off, len);
        bytesEnviados += len;
    }

    @Override
    public void flush() throws IOException {
        destino.flush();
    }

    @Override
    public void close() throws IOException {
        destino.close();
    }
}
