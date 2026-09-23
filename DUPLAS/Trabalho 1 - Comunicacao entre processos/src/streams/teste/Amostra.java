package streams.teste;

import modelo.Candidato;

/** Dados de exemplo usados pelos testes das Questões 2 e 3. */
public final class Amostra {
    /** Quantidade de objetos enviados nos testes (menor que o tamanho do array, de propósito). */
    public static final int QUANTIDADE_ENVIADA = 3;

    private Amostra() {
    }

    public static Candidato[] candidatos() {
        return new Candidato[] {
                new Candidato(13, "Ada Lovelace", "Partido dos Algoritmos"),
                new Candidato(22, "Alan Turing", "Partido da Computabilidade"),
                new Candidato(45, "Grace Hopper", "Partido dos Compiladores"),
                new Candidato(50, "Edsger Dijkstra", "Partido dos Grafos"),
                new Candidato(77, "Barbara Liskov", "Partido da Abstração")
        };
    }

    public static void imprimir(String prefixo, Candidato[] candidatos) {
        System.err.println(prefixo + candidatos.length + " candidato(s) recebido(s):");
        for (int i = 0; i < candidatos.length; i++) {
            System.err.println("  [" + i + "] " + candidatos[i]);
        }
    }
}
