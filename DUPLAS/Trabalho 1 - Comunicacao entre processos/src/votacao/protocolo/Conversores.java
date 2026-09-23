package votacao.protocolo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelo.Candidato;
import modelo.ItemResultado;
import modelo.NotaInformativa;
import modelo.Resultado;

/** Conversão entre os POJOs do modelo e as estruturas JSON (Map/List) usadas nas mensagens. */
public final class Conversores {
    private Conversores() {
    }

    // ---------------------------------------------------------------- Candidato

    public static Map<String, Object> paraMapa(Candidato c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("numero", c.getNumero());
        m.put("nome", c.getNome());
        m.put("partido", c.getPartido());
        return m;
    }

    public static List<Object> paraLista(List<Candidato> candidatos) {
        List<Object> lista = new ArrayList<>();
        for (Candidato c : candidatos) {
            lista.add(paraMapa(c));
        }
        return lista;
    }

    public static Candidato candidato(Map<String, Object> m) {
        return new Candidato(numero(m.get("numero")), texto(m.get("nome")), texto(m.get("partido")));
    }

    @SuppressWarnings("unchecked")
    public static List<Candidato> candidatos(List<Object> lista) {
        List<Candidato> resultado = new ArrayList<>();
        for (Object o : lista) {
            resultado.add(candidato((Map<String, Object>) o));
        }
        return resultado;
    }

    // ---------------------------------------------------------------- Resultado

    public static Map<String, Object> paraMapa(Resultado r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalVotos", r.getTotalVotos());
        List<Object> itens = new ArrayList<>();
        for (ItemResultado i : r.getItens()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("candidato", paraMapa(i.getCandidato()));
            item.put("votos", i.getVotos());
            item.put("percentual", i.getPercentual());
            itens.add(item);
        }
        m.put("itens", itens);
        m.put("vencedores", paraLista(r.getVencedores()));
        return m;
    }

    @SuppressWarnings("unchecked")
    public static Resultado resultado(Map<String, Object> m) {
        List<ItemResultado> itens = new ArrayList<>();
        Object listaItens = m.get("itens");
        if (listaItens instanceof List) {
            for (Object o : (List<Object>) listaItens) {
                Map<String, Object> item = (Map<String, Object>) o;
                Number percentual = (Number) item.get("percentual");
                itens.add(new ItemResultado(candidato((Map<String, Object>) item.get("candidato")),
                        numero(item.get("votos")), percentual == null ? 0.0 : percentual.doubleValue()));
            }
        }
        List<Candidato> vencedores = new ArrayList<>();
        Object listaVencedores = m.get("vencedores");
        if (listaVencedores instanceof List) {
            vencedores = candidatos((List<Object>) listaVencedores);
        }
        return new Resultado(numero(m.get("totalVotos")), itens, vencedores);
    }

    // ---------------------------------------------------------------- Nota (multicast)

    public static Mensagem paraMensagem(NotaInformativa n) {
        return new Mensagem()
                .com("tipo", Protocolo.TIPO_NOTA)
                .com("autor", n.getAutor())
                .com("texto", n.getTexto())
                .com("instante", n.getInstante());
    }

    public static NotaInformativa nota(Mensagem m) {
        return new NotaInformativa(m.getString("autor"), m.getString("texto"), m.getLong("instante", 0L));
    }

    // ---------------------------------------------------------------- auxiliares

    private static int numero(Object o) {
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }

    private static String texto(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
