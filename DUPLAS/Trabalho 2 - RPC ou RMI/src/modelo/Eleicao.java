package modelo;

/**
 * POJO: eleição (votação) em andamento. O atributo {@code fim} é o prazo máximo
 * (epoch em milissegundos) para o envio de votos.
 */
public class Eleicao {
    private int id;
    private String titulo;
    private long inicio;
    private long fim;

    public Eleicao() {
    }

    public Eleicao(int id, String titulo, long inicio, long fim) {
        this.id = id;
        this.titulo = titulo;
        this.inicio = inicio;
        this.fim = fim;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public long getInicio() { return inicio; }
    public void setInicio(long inicio) { this.inicio = inicio; }

    public long getFim() { return fim; }
    public void setFim(long fim) { this.fim = fim; }

    @Override
    public String toString() {
        return "Eleicao{id=" + id + ", titulo=" + titulo + ", inicio=" + inicio + ", fim=" + fim + "}";
    }
}
