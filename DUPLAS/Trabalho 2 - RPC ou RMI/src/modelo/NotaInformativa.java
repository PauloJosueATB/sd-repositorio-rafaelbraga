package modelo;

/** POJO: nota informativa enviada por um administrador para os eleitores (via multicast). */
public class NotaInformativa {
    private String autor;
    private String texto;
    private long instante;

    public NotaInformativa() {
    }

    public NotaInformativa(String autor, String texto, long instante) {
        this.autor = autor;
        this.texto = texto;
        this.instante = instante;
    }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public long getInstante() { return instante; }
    public void setInstante(long instante) { this.instante = instante; }

    @Override
    public String toString() {
        return "Nota de " + autor + ": " + texto;
    }
}
