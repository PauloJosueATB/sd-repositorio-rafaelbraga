package modelo;

/** POJO: linha da apuração (votos e percentual de um candidato). */
public class ItemResultado {
    private Candidato candidato;
    private int votos;
    private double percentual;

    public ItemResultado() {
    }

    public ItemResultado(Candidato candidato, int votos, double percentual) {
        this.candidato = candidato;
        this.votos = votos;
        this.percentual = percentual;
    }

    public Candidato getCandidato() { return candidato; }
    public void setCandidato(Candidato candidato) { this.candidato = candidato; }

    public int getVotos() { return votos; }
    public void setVotos(int votos) { this.votos = votos; }

    public double getPercentual() { return percentual; }
    public void setPercentual(double percentual) { this.percentual = percentual; }

    @Override
    public String toString() {
        return candidato + " -> " + votos + " voto(s), " + String.format("%.2f", percentual) + "%";
    }
}
