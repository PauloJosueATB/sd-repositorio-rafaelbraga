package protocolo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelo.Candidato;
import modelo.ItemResultado;
import modelo.Resultado;
import modelo.Usuario;

/**
 * Converte os POJOs do domínio (objetos LOCAIS, executados no servidor) para
 * estruturas JSON (Map/List) e vice-versa. É esta camada que implementa a
 * "representação externa de dados" exigida para a passagem de parâmetros por
 * valor: nenhum POJO trafega como objeto Java serializado nativamente — ele é
 * sempre convertido para um Map/List e depois para texto JSON (classe {@link Json}).
 */
public final class Conversores {
    private Conversores() {
    }

    public static Map<String, Object> deCandidato(Candidato c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("numero", (long) c.getNumero());
        m.put("nome", c.getNome());
        m.put("partido", c.getPartido());
        return m;
    }

    @SuppressWarnings("unchecked")
    public static Candidato paraCandidato(Object o) {
        Map<String, Object> m = (Map<String, Object>) o;
        return new Candidato(numero(m.get("numero")), (String) m.get("nome"), (String) m.get("partido"));
    }

    public static List<Object> deCandidatos(List<Candidato> lista) {
        List<Object> saida = new ArrayList<>();
        for (Candidato c : lista) {
            saida.add(deCandidato(c));
        }
        return saida;
    }

    public static Map<String, Object> deUsuario(Usuario u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("login", u.getLogin());
        m.put("nome", u.getNome());
        m.put("perfil", u.getPerfil().name());
        return m;
    }

    public static Map<String, Object> deResultado(Resultado r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalVotos", (long) r.getTotalVotos());
        List<Object> itens = new ArrayList<>();
        for (ItemResultado i : r.getItens()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("candidato", deCandidato(i.getCandidato()));
            item.put("votos", (long) i.getVotos());
            item.put("percentual", i.getPercentual());
            itens.add(item);
        }
        m.put("itens", itens);
        m.put("vencedores", deCandidatos(r.getVencedores()));
        return m;
    }

    private static int numero(Object valorJson) {
        return ((Number) valorJson).intValue();
    }
}
