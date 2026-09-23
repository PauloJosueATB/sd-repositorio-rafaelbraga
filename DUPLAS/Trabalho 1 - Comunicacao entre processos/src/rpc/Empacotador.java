package rpc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import modelo.Candidato;
import modelo.ItemResultado;
import modelo.Resultado;
import streams.EleicaoInputStream;
import streams.EleicaoOutputStream;

/**
 * QUESTÃO 4 - Serialização: empacotamento (marshalling) e desempacotamento (unmarshalling)
 * das mensagens de request e reply em vetores de bytes.
 *
 * <ul>
 *   <li>o CLIENTE empacota a requisição  -> {@link #empacotarRequisicao}</li>
 *   <li>o SERVIDOR desempacota a requisição -> {@link #desempacotarRequisicao}</li>
 *   <li>o SERVIDOR empacota a resposta -> {@link #empacotarResposta}</li>
 *   <li>o CLIENTE desempacota a resposta -> {@link #desempacotarResposta}</li>
 * </ul>
 *
 * <h3>Representação externa dos dados (big-endian)</h3>
 * <pre>
 *   Requisição: byte tipo(=1) | int id | UTF metodo | int nArgs | valor[nArgs]
 *   Resposta  : byte tipo(=2) | int id | boolean sucesso | (sucesso ? valor : UTF erro)
 *   valor     : byte tag + conteúdo
 *       0 nulo | 1 int | 2 long | 3 String (UTF) | 4 boolean
 *       5 Candidato[]  (formato da EleicaoOutputStream da Questão 2)
 *       6 Resultado    (total, itens com candidato/votos/percentual, vencedores)
 * </pre>
 * Cada mensagem trafega no socket dentro de um "quadro": int tamanho + bytes.
 */
public final class Empacotador {
    private static final byte TIPO_REQUISICAO = 1;
    private static final byte TIPO_RESPOSTA = 2;

    private static final byte NULO = 0;
    private static final byte INT = 1;
    private static final byte LONG = 2;
    private static final byte STRING = 3;
    private static final byte BOOLEANO = 4;
    private static final byte CANDIDATOS = 5;
    private static final byte RESULTADO = 6;

    private static final int MAX_QUADRO = 1 << 20;
    private static final int MAX_ARGUMENTOS = 64;

    private Empacotador() {
    }

    // ------------------------------------------------------------------ requisição

    public static byte[] empacotarRequisicao(Requisicao r) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(TIPO_REQUISICAO);
        out.writeInt(r.getId());
        out.writeUTF(r.getMetodo());
        out.writeInt(r.getArgumentos().length);
        for (Object argumento : r.getArgumentos()) {
            escreverValor(out, argumento);
        }
        out.flush();
        return bytes.toByteArray();
    }

    public static Requisicao desempacotarRequisicao(byte[] dados) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(dados));
        byte tipo = in.readByte();
        if (tipo != TIPO_REQUISICAO) {
            throw new IOException("Mensagem não é uma requisição (tipo=" + tipo + ")");
        }
        int id = in.readInt();
        String metodo = in.readUTF();
        int n = in.readInt();
        if (n < 0 || n > MAX_ARGUMENTOS) {
            throw new IOException("Número de argumentos inválido: " + n);
        }
        Object[] argumentos = new Object[n];
        for (int i = 0; i < n; i++) {
            argumentos[i] = lerValor(in);
        }
        return new Requisicao(id, metodo, argumentos);
    }

    // ------------------------------------------------------------------ resposta

    public static byte[] empacotarResposta(Resposta r) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(TIPO_RESPOSTA);
        out.writeInt(r.getId());
        out.writeBoolean(r.isSucesso());
        if (r.isSucesso()) {
            escreverValor(out, r.getResultado());
        } else {
            out.writeUTF(r.getErro() == null ? "" : r.getErro());
        }
        out.flush();
        return bytes.toByteArray();
    }

    public static Resposta desempacotarResposta(byte[] dados) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(dados));
        byte tipo = in.readByte();
        if (tipo != TIPO_RESPOSTA) {
            throw new IOException("Mensagem não é uma resposta (tipo=" + tipo + ")");
        }
        int id = in.readInt();
        boolean sucesso = in.readBoolean();
        return sucesso ? Resposta.sucesso(id, lerValor(in)) : Resposta.erro(id, in.readUTF());
    }

    // ------------------------------------------------------------------ valores

    private static void escreverValor(DataOutputStream out, Object v) throws IOException {
        if (v == null) {
            out.writeByte(NULO);
        } else if (v instanceof Integer) {
            out.writeByte(INT);
            out.writeInt((Integer) v);
        } else if (v instanceof Long) {
            out.writeByte(LONG);
            out.writeLong((Long) v);
        } else if (v instanceof String) {
            out.writeByte(STRING);
            out.writeUTF((String) v);
        } else if (v instanceof Boolean) {
            out.writeByte(BOOLEANO);
            out.writeBoolean((Boolean) v);
        } else if (v instanceof Candidato[]) {
            Candidato[] vetor = (Candidato[]) v;
            out.writeByte(CANDIDATOS);
            // Reaproveita o stream da Questão 2: envia quantidade + (tamanho, atributos) de cada candidato
            new EleicaoOutputStream(vetor, vetor.length, out);
        } else if (v instanceof Resultado) {
            out.writeByte(RESULTADO);
            escreverResultado(out, (Resultado) v);
        } else {
            throw new IllegalArgumentException("Tipo não suportado no empacotamento: " + v.getClass().getName());
        }
    }

    private static Object lerValor(DataInputStream in) throws IOException {
        byte tag = in.readByte();
        switch (tag) {
            case NULO:       return null;
            case INT:        return in.readInt();
            case LONG:       return in.readLong();
            case STRING:     return in.readUTF();
            case BOOLEANO:   return in.readBoolean();
            case CANDIDATOS: return new EleicaoInputStream(in).lerCandidatos();
            case RESULTADO:  return lerResultado(in);
            default:         throw new IOException("Tag de tipo desconhecida: " + tag);
        }
    }

    private static void escreverResultado(DataOutputStream out, Resultado r) throws IOException {
        out.writeInt(r.getTotalVotos());
        out.writeInt(r.getItens().size());
        for (ItemResultado i : r.getItens()) {
            out.writeInt(i.getCandidato().getNumero());
            out.writeUTF(i.getCandidato().getNome());
            out.writeUTF(i.getCandidato().getPartido());
            out.writeInt(i.getVotos());
            out.writeDouble(i.getPercentual());
        }
        out.writeInt(r.getVencedores().size());
        for (Candidato c : r.getVencedores()) {
            out.writeInt(c.getNumero());
        }
    }

    private static Resultado lerResultado(DataInputStream in) throws IOException {
        int total = in.readInt();
        int nItens = in.readInt();
        if (nItens < 0 || nItens > 100_000) {
            throw new IOException("Quantidade de itens inválida: " + nItens);
        }
        List<ItemResultado> itens = new ArrayList<>();
        for (int i = 0; i < nItens; i++) {
            Candidato c = new Candidato(in.readInt(), in.readUTF(), in.readUTF());
            itens.add(new ItemResultado(c, in.readInt(), in.readDouble()));
        }
        int nVencedores = in.readInt();
        if (nVencedores < 0 || nVencedores > nItens) {
            throw new IOException("Quantidade de vencedores inválida: " + nVencedores);
        }
        List<Candidato> vencedores = new ArrayList<>();
        for (int i = 0; i < nVencedores; i++) {
            int numero = in.readInt();
            for (ItemResultado item : itens) {
                if (item.getCandidato().getNumero() == numero) {
                    vencedores.add(item.getCandidato());
                }
            }
        }
        return new Resultado(total, itens, vencedores);
    }

    // ------------------------------------------------------------------ quadros no socket

    /** Escreve [int tamanho][bytes] no stream (delimita a mensagem dentro do fluxo TCP). */
    public static void escreverQuadro(OutputStream out, byte[] dados) throws IOException {
        DataOutputStream d = new DataOutputStream(out);
        d.writeInt(dados.length);
        d.write(dados);
        d.flush();
    }

    /** Lê um quadro. Devolve {@code null} se a conexão foi encerrada limpamente entre duas mensagens. */
    public static byte[] lerQuadro(InputStream in) throws IOException {
        int primeiro = in.read();
        if (primeiro < 0) {
            return null;
        }
        DataInputStream d = new DataInputStream(in);
        int tamanho = (primeiro << 24) | (d.readUnsignedByte() << 16) | (d.readUnsignedByte() << 8) | d.readUnsignedByte();
        if (tamanho <= 0 || tamanho > MAX_QUADRO) {
            throw new IOException("Tamanho de mensagem inválido: " + tamanho);
        }
        byte[] dados = new byte[tamanho];
        try {
            d.readFully(dados);
        } catch (EOFException e) {
            throw new EOFException("Conexão encerrada no meio de uma mensagem.");
        }
        return dados;
    }
}
