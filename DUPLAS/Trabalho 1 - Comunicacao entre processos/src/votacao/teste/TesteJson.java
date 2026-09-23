package votacao.teste;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import votacao.protocolo.Json;
import votacao.protocolo.Mensagem;

/** Teste automatizado do codificador/decodificador JSON: {@code java votacao.teste.TesteJson}. */
public class TesteJson {
    private static int falhas = 0;

    public static void main(String[] args) {
        // escrita
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("texto", "aspas \" barra \\ nova\nlinha\ttab");
        m.put("n", 42);
        m.put("d", 66.5);
        m.put("ok", true);
        m.put("nulo", null);
        List<Object> lista = new ArrayList<>();
        lista.add(1);
        lista.add("dois");
        m.put("lista", lista);
        String json = Json.escrever(m);
        verificar(json.equals("{\"texto\":\"aspas \\\" barra \\\\ nova\\nlinha\\ttab\",\"n\":42,\"d\":66.5,\"ok\":true,\"nulo\":null,\"lista\":[1,\"dois\"]}"),
                "escrita de objeto com escapes");
        verificar(!json.contains("\n"), "JSON gerado ocupa uma única linha (necessário para o protocolo por linhas)");

        // ida e volta
        Object volta = Json.ler(json);
        verificar(Json.escrever(volta).equals(json), "ida e volta (escrever -> ler -> escrever)");

        // leitura
        Mensagem msg = Mensagem.doJson("  { \"op\" : \"VOTAR\", \"candidato\": 13, \"x\": [1, 2.5e1, {\"a\": null}], \"u\": \"\\u00e7\\u00e3o\" } ");
        verificar("VOTAR".equals(msg.getString("op")), "leitura de string");
        verificar(msg.getInt("candidato", -1) == 13, "leitura de inteiro");
        verificar(msg.getLista("x").size() == 3 && msg.getLista("x").get(1).equals(25.0), "leitura de array e número real");
        verificar("ção".equals(msg.getString("u")), "escape \\uXXXX (acentos)");

        String acentos = "José da Conceição — “aspas” 😀";
        verificar(acentos.equals(Mensagem.doJson(new Mensagem().com("t", acentos).paraJson()).getString("t")),
                "acentos, aspas tipográficas e emoji (surrogates) preservados");

        // entradas inválidas
        String[] invalidos = { "", "{", "{\"a\"}", "{\"a\":}", "[1,2", "{\"a\":1} lixo", "tru", "\"sem fim",
                "{\"a\":\"\\x\"}", "[1 2]", "{'a':1}", "-", "\"tab\tliteral\"" };
        for (String s : invalidos) {
            try {
                Json.ler(s);
                verificar(false, "deveria rejeitar: " + s);
            } catch (IllegalArgumentException e) {
                verificar(true, "rejeita JSON inválido: " + s.replace("\t", "\\t"));
            }
        }

        StringBuilder fundo = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            fundo.append('[');
        }
        try {
            Json.ler(fundo.toString());
            verificar(false, "aninhamento excessivo deveria ser rejeitado");
        } catch (IllegalArgumentException e) {
            verificar(true, "aninhamento excessivo é rejeitado (sem StackOverflow)");
        }

        try {
            Mensagem.doJson("[1,2,3]");
            verificar(false, "mensagem que não é objeto deveria ser rejeitada");
        } catch (IllegalArgumentException e) {
            verificar(true, "mensagem que não é objeto é rejeitada");
        }

        System.out.println(falhas == 0 ? "\nTodos os testes passaram." : "\n" + falhas + " teste(s) FALHARAM.");
        System.exit(falhas == 0 ? 0 : 1);
    }

    private static void verificar(boolean condicao, String descricao) {
        System.out.println((condicao ? "[OK]   " : "[FALHA] ") + descricao);
        if (!condicao) {
            falhas++;
        }
    }
}
