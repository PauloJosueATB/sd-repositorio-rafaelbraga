package rmi;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import protocolo.Json;

/**
 * Converte os argumentos e resultados (sempre um {@code Map<String,Object>}) para
 * {@code byte[]} e de volta, usando JSON como representação externa de dados —
 * o mesmo papel do "marshalling" citado na Seção 4.3 / Figura 5.4 do livro-texto.
 * Toda informação que atravessa a rede passa por aqui: nunca um POJO (Candidato,
 * Resultado etc.) é serializado nativamente pelo Java.
 */
public final class Empacotador {
    private Empacotador() {
    }

    public static byte[] empacotar(Map<String, Object> dados) {
        return Json.escrever(dados).getBytes(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> desempacotar(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new LinkedHashMap<>();
        }
        Object valor = Json.ler(new String(bytes, StandardCharsets.UTF_8));
        if (!(valor instanceof Map)) {
            throw new IllegalArgumentException("Mensagem RMI malformada: esperava um objeto JSON.");
        }
        return (Map<String, Object>) valor;
    }

    /** Resposta de sucesso: {"ok":true, ...dados}. */
    public static byte[] empacotarOk(Map<String, Object> dados) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", Boolean.TRUE);
        m.putAll(dados);
        return empacotar(m);
    }

    /** Resposta de erro: {"ok":false, "erro":"..."}. */
    public static byte[] empacotarErro(String mensagem) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", Boolean.FALSE);
        m.put("erro", mensagem);
        return empacotar(m);
    }

    public static String string(Map<String, Object> m, String chave) {
        Object v = m.get(chave);
        return v == null ? null : String.valueOf(v);
    }

    public static int inteiro(Map<String, Object> m, String chave) {
        Object v = m.get(chave);
        if (v == null) {
            throw new IllegalArgumentException("Campo obrigatório ausente: " + chave);
        }
        return ((Number) v).intValue();
    }
}
