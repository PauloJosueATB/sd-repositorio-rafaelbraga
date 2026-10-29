package modelo;

import java.util.ArrayList;
import java.util.List;

/** POJO: apuração final (total de votos, percentuais por candidato e vencedor(es)). */
public class Resultado {
    private int totalVotos;
    private List<ItemResultado> itens = new ArrayList<>();
    private List<Candidato> vencedores = new ArrayList<>();

    public Resultado() {
    }

    public Resultado(int totalVotos, List<ItemResultado> itens, List<Candidato> vencedores) {
        this.totalVotos = totalVotos;
        this.itens = itens;
        this.vencedores = vencedores;
    }

    public int getTotalVotos() { return totalVotos; }
    public void setTotalVotos(int totalVotos) { this.totalVotos = totalVotos; }

    public List<ItemResultado> getItens() { return itens; }
    public void setItens(List<ItemResultado> itens) { this.itens = itens; }

    public List<Candidato> getVencedores() { return vencedores; }
    public void setVencedores(List<Candidato> vencedores) { this.vencedores = vencedores; }

    public boolean isEmpate() {
        return vencedores.size() > 1;
    }

    @Override
    public String toString() {
        return "Resultado{total=" + totalVotos + ", itens=" + itens + ", vencedores=" + vencedores + "}";
    }
}
