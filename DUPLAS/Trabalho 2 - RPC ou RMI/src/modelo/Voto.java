package modelo;

/**
 * POJO: voto registrado. Guarda apenas o comprovante, o candidato e o instante;
 * a identidade do eleitor NÃO é associada ao voto (sigilo do voto).
 */
public class Voto {
    private String comprovante;
    private int numeroCandidato;
    private long instante;

    public Voto() {
    }

    public Voto(String comprovante, int numeroCandidato, long instante) {
        this.comprovante = comprovante;
        this.numeroCandidato = numeroCandidato;
        this.instante = instante;
    }

    public String getComprovante() { return comprovante; }
    public void setComprovante(String comprovante) { this.comprovante = comprovante; }

    public int getNumeroCandidato() { return numeroCandidato; }
    public void setNumeroCandidato(int numeroCandidato) { this.numeroCandidato = numeroCandidato; }

    public long getInstante() { return instante; }
    public void setInstante(long instante) { this.instante = instante; }

    @Override
    public String toString() {
        return "Voto{comprovante=" + comprovante + ", candidato=" + numeroCandidato + ", instante=" + instante + "}";
    }
}
