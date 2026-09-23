package rpc;

/** Mensagem de REPLY: identificador da requisição, indicador de sucesso e resultado (ou erro). */
public class Resposta {
    private final int id;
    private final boolean sucesso;
    private final Object resultado;
    private final String erro;

    private Resposta(int id, boolean sucesso, Object resultado, String erro) {
        this.id = id;
        this.sucesso = sucesso;
        this.resultado = resultado;
        this.erro = erro;
    }

    public static Resposta sucesso(int id, Object resultado) {
        return new Resposta(id, true, resultado, null);
    }

    public static Resposta erro(int id, String mensagem) {
        return new Resposta(id, false, null, mensagem);
    }

    public int getId() { return id; }
    public boolean isSucesso() { return sucesso; }
    public Object getResultado() { return resultado; }
    public String getErro() { return erro; }

    @Override
    public String toString() {
        return sucesso ? "Resposta{id=" + id + ", resultado=" + resultado + "}"
                       : "Resposta{id=" + id + ", erro=" + erro + "}";
    }
}
