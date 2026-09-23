package votacao.protocolo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mensagem do protocolo de votação: um objeto JSON com campos nomeados.
 * Serve tanto para requisições/respostas TCP quanto para as notas multicast UDP.
 */
public class Mensagem {
    private final Map<String, Object> campos = new LinkedHashMap<>();

    public Mensagem() {
    }

    /** Desempacota (parse) um texto JSON. O JSON deve ser um objeto. */
    @SuppressWarnings("unchecked")
    public static Mensagem doJson(String json) {
        Object o = Json.ler(json);
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("A mensagem deve ser um objeto JSON.");
        }
        Mensagem m = new Mensagem();
        m.campos.putAll((Map<String, Object>) o);
        return m;
    }

    /** Empacota (serializa) a mensagem em uma linha de texto JSON. */
    public String paraJson() {
        return Json.escrever(campos);
    }

    public static Mensagem requisicao(String operacao) {
        return new Mensagem().com("op", operacao);
    }

    public static Mensagem ok() {
        return new Mensagem().com("ok", true);
    }

    public static Mensagem erro(String motivo) {
        return new Mensagem().com("ok", false).com("erro", motivo);
    }

    public Mensagem com(String chave, Object valor) {
        campos.put(chave, valor);
        return this;
    }

    public boolean tem(String chave) {
        return campos.containsKey(chave);
    }

    public boolean isOk() {
        return getBoolean("ok", false);
    }

    public String getString(String chave) {
        Object v = campos.get(chave);
        return v == null ? null : String.valueOf(v);
    }

    public int getInt(String chave, int padrao) {
        Object v = campos.get(chave);
        return v instanceof Number ? ((Number) v).intValue() : padrao;
    }

    public long getLong(String chave, long padrao) {
        Object v = campos.get(chave);
        return v instanceof Number ? ((Number) v).longValue() : padrao;
    }

    public boolean getBoolean(String chave, boolean padrao) {
        Object v = campos.get(chave);
        return v instanceof Boolean ? (Boolean) v : padrao;
    }

    @SuppressWarnings("unchecked")
    public List<Object> getLista(String chave) {
        Object v = campos.get(chave);
        return v instanceof List ? (List<Object>) v : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMapa(String chave) {
        Object v = campos.get(chave);
        return v instanceof Map ? (Map<String, Object>) v : new LinkedHashMap<>();
    }

    @Override
    public String toString() {
        return paraJson();
    }
}
