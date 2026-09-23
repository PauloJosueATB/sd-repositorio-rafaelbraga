package modelo;

import java.util.Objects;

/** POJO: candidato em disputa na votação. */
public class Candidato {
    private int numero;
    private String nome;
    private String partido;

    public Candidato() {
    }

    public Candidato(int numero, String nome, String partido) {
        this.numero = numero;
        this.nome = nome;
        this.partido = partido;
    }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getPartido() { return partido; }
    public void setPartido(String partido) { this.partido = partido; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Candidato)) return false;
        Candidato c = (Candidato) o;
        return numero == c.numero && Objects.equals(nome, c.nome) && Objects.equals(partido, c.partido);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero, nome, partido);
    }

    @Override
    public String toString() {
        return "[" + numero + "] " + nome + " (" + partido + ")";
    }
}
