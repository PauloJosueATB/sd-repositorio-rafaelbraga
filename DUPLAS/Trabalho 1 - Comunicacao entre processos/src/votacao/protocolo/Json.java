package votacao.protocolo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Codificador/decodificador JSON mínimo (RFC 8259), sem dependências externas.
 * <p>Mapeamento Java &lt;-&gt; JSON:
 * <ul>
 *   <li>objeto JSON &lt;-&gt; {@code Map<String,Object>} (LinkedHashMap, preserva a ordem)</li>
 *   <li>array JSON &lt;-&gt; {@code List<Object>}</li>
 *   <li>string &lt;-&gt; String; número inteiro &lt;-&gt; Long; número real &lt;-&gt; Double</li>
 *   <li>true/false &lt;-&gt; Boolean; null &lt;-&gt; null</li>
 * </ul>
 * Erros de sintaxe lançam {@link IllegalArgumentException}.
 */
public final class Json {
    private static final int PROFUNDIDADE_MAXIMA = 64;

    private Json() {
    }

    // ------------------------------------------------------------------ escrita

    public static String escrever(Object valor) {
        StringBuilder sb = new StringBuilder();
        escrever(valor, sb);
        return sb.toString();
    }

    private static void escrever(Object v, StringBuilder sb) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            escreverString((String) v, sb);
        } else if (v instanceof Boolean) {
            sb.append(v.toString());
        } else if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            sb.append(Double.isNaN(d) || Double.isInfinite(d) ? "null" : Double.toString(d));
        } else if (v instanceof Number) {
            sb.append(v.toString());
        } else if (v instanceof Map) {
            sb.append('{');
            boolean primeiro = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
                if (!primeiro) {
                    sb.append(',');
                }
                primeiro = false;
                escreverString(String.valueOf(e.getKey()), sb);
                sb.append(':');
                escrever(e.getValue(), sb);
            }
            sb.append('}');
        } else if (v instanceof Iterable) {
            sb.append('[');
            boolean primeiro = true;
            for (Object item : (Iterable<?>) v) {
                if (!primeiro) {
                    sb.append(',');
                }
                primeiro = false;
                escrever(item, sb);
            }
            sb.append(']');
        } else if (v instanceof Object[]) {
            sb.append('[');
            Object[] vetor = (Object[]) v;
            for (int i = 0; i < vetor.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                escrever(vetor[i], sb);
            }
            sb.append(']');
        } else {
            throw new IllegalArgumentException("Tipo não suportado em JSON: " + v.getClass().getName());
        }
    }

    private static void escreverString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------------ leitura

    public static Object ler(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("JSON nulo");
        }
        Leitor leitor = new Leitor(texto);
        Object valor = leitor.lerValor(0);
        leitor.pularEspacos();
        if (leitor.pos != texto.length()) {
            throw leitor.erro("conteúdo inesperado após o fim do JSON");
        }
        return valor;
    }

    private static final class Leitor {
        private final String s;
        private int pos;

        Leitor(String s) {
            this.s = s;
        }

        IllegalArgumentException erro(String mensagem) {
            return new IllegalArgumentException("JSON inválido na posição " + pos + ": " + mensagem);
        }

        void pularEspacos() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        Object lerValor(int profundidade) {
            if (profundidade > PROFUNDIDADE_MAXIMA) {
                throw erro("aninhamento excessivo");
            }
            pularEspacos();
            if (pos >= s.length()) {
                throw erro("fim inesperado");
            }
            char c = s.charAt(pos);
            switch (c) {
                case '{': return lerObjeto(profundidade);
                case '[': return lerArray(profundidade);
                case '"': return lerString();
                case 't': literal("true"); return Boolean.TRUE;
                case 'f': literal("false"); return Boolean.FALSE;
                case 'n': literal("null"); return null;
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return lerNumero();
                    }
                    throw erro("caractere inesperado '" + c + "'");
            }
        }

        private void literal(String esperado) {
            if (!s.startsWith(esperado, pos)) {
                throw erro("esperado '" + esperado + "'");
            }
            pos += esperado.length();
        }

        private Map<String, Object> lerObjeto(int profundidade) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            pos++; // '{'
            pularEspacos();
            if (pos < s.length() && s.charAt(pos) == '}') {
                pos++;
                return mapa;
            }
            while (true) {
                pularEspacos();
                if (pos >= s.length() || s.charAt(pos) != '"') {
                    throw erro("esperada chave (string)");
                }
                String chave = lerString();
                pularEspacos();
                if (pos >= s.length() || s.charAt(pos) != ':') {
                    throw erro("esperado ':'");
                }
                pos++;
                mapa.put(chave, lerValor(profundidade + 1));
                pularEspacos();
                if (pos >= s.length()) {
                    throw erro("objeto não terminado");
                }
                char c = s.charAt(pos++);
                if (c == '}') {
                    return mapa;
                }
                if (c != ',') {
                    pos--;
                    throw erro("esperado ',' ou '}'");
                }
            }
        }

        private List<Object> lerArray(int profundidade) {
            List<Object> lista = new ArrayList<>();
            pos++; // '['
            pularEspacos();
            if (pos < s.length() && s.charAt(pos) == ']') {
                pos++;
                return lista;
            }
            while (true) {
                lista.add(lerValor(profundidade + 1));
                pularEspacos();
                if (pos >= s.length()) {
                    throw erro("array não terminado");
                }
                char c = s.charAt(pos++);
                if (c == ']') {
                    return lista;
                }
                if (c != ',') {
                    pos--;
                    throw erro("esperado ',' ou ']'");
                }
            }
        }

        private String lerString() {
            pos++; // '"'
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (pos >= s.length()) {
                    throw erro("string não terminada");
                }
                char c = s.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c < 0x20) {
                    pos--;
                    throw erro("caractere de controle sem escape na string");
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (pos >= s.length()) {
                    throw erro("escape incompleto");
                }
                char e = s.charAt(pos++);
                switch (e) {
                    case '"':  sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/'); break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case 'u':
                        if (pos + 4 > s.length()) {
                            throw erro("escape \\u incompleto");
                        }
                        try {
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw erro("escape \\u inválido");
                        }
                        pos += 4;
                        break;
                    default:
                        pos--;
                        throw erro("escape inválido '\\" + e + "'");
                }
            }
        }

        private Object lerNumero() {
            int inicio = pos;
            boolean real = false;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '.' || c == 'e' || c == 'E') {
                    real = true;
                    pos++;
                } else if ((c >= '0' && c <= '9') || c == '-' || c == '+') {
                    pos++;
                } else {
                    break;
                }
            }
            String texto = s.substring(inicio, pos);
            try {
                return real ? (Object) Double.valueOf(texto) : (Object) Long.valueOf(texto);
            } catch (NumberFormatException e) {
                pos = inicio;
                throw erro("número inválido '" + texto + "'");
            }
        }
    }
}
