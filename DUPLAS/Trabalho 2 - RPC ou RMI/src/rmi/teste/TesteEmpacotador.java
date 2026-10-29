package rmi.teste;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelo.Candidato;
import modelo.ItemResultado;
import modelo.Resultado;
import protocolo.Conversores;
import rmi.Empacotador;

/** <pre>java rmi.teste.TesteEmpacotador</pre> */
public class TesteEmpacotador {
    private static int falhas = 0;

    public static void main(String[] args) {
        // ida e volta de argumentos simples
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("login", "eleitor1");
        entrada.put("numero", 13);
        byte[] bytes = Empacotador.empacotar(entrada);
        Map<String, Object> de_volta = Empacotador.desempacotar(bytes);
        verificar("eleitor1".equals(de_volta.get("login")), "string ida e volta");
        verificar(((Number) de_volta.get("numero")).intValue() == 13, "inteiro ida e volta");

        // resposta de sucesso/erro
        Map<String, Object> ok = Empacotador.desempacotar(Empacotador.empacotarOk(Map.of("x", 1)));
        verificar(Boolean.TRUE.equals(ok.get("ok")), "empacotarOk() marca ok=true");
        Map<String, Object> erro = Empacotador.desempacotar(Empacotador.empacotarErro("deu ruim"));
        verificar(Boolean.FALSE.equals(erro.get("ok")) && "deu ruim".equals(erro.get("erro")), "empacotarErro() marca ok=false e a mensagem");

        // candidato (com acentos) e lista de candidatos
        Candidato c = new Candidato(45, "Grace Hopper", "Partido \"Ação\" & Cia");
        Candidato c2 = Conversores.paraCandidato(Conversores.deCandidato(c));
        verificar(c.equals(c2) && c2.getPartido().equals(c.getPartido()), "Candidato ida e volta preserva acentos e aspas");

        // resultado completo (itens + vencedores)
        Resultado r = new Resultado(10,
                List.of(new ItemResultado(c, 6, 60.0), new ItemResultado(new Candidato(13, "Ada Lovelace", "P1"), 4, 40.0)),
                List.of(c));
        Map<String, Object> rJson = Conversores.deResultado(r);
        byte[] rBytes = Empacotador.empacotar(rJson);
        Map<String, Object> rDeVolta = Empacotador.desempacotar(rBytes);
        verificar(((Number) rDeVolta.get("totalVotos")).intValue() == 10, "Resultado.totalVotos ida e volta");
        @SuppressWarnings("unchecked")
        List<Object> itens = (List<Object>) rDeVolta.get("itens");
        verificar(itens.size() == 2, "Resultado.itens ida e volta (quantidade)");

        // JSON malformado deve ser rejeitado
        verificar(falhouAoDesempacotar("{ isto nao é json"), "JSON malformado lança exceção");
        verificar(falhouAoDesempacotar("[1,2,3]"), "JSON que não é objeto é rejeitado");

        if (falhas == 0) {
            System.out.println("Todos os testes passaram.");
        } else {
            System.out.println(falhas + " teste(s) falharam.");
            System.exit(1);
        }
    }

    private static boolean falhouAoDesempacotar(String json) {
        try {
            Empacotador.desempacotar(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return false;
        } catch (RuntimeException e) {
            return true;
        }
    }

    private static void verificar(boolean condicao, String descricao) {
        if (condicao) {
            System.out.println("[OK]   " + descricao);
        } else {
            System.out.println("[FALHOU] " + descricao);
            falhas++;
        }
    }
}
