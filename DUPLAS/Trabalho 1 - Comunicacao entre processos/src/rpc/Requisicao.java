package rpc;

import java.util.Arrays;

/** Mensagem de REQUEST: identificador, nome do método remoto e argumentos. */
public class Requisicao {
    private final int id;
    private final String metodo;
    private final Object[] argumentos;

    public Requisicao(int id, String metodo, Object... argumentos) {
        this.id = id;
        this.metodo = metodo;
        this.argumentos = argumentos == null ? new Object[0] : argumentos;
    }

    public int getId() { return id; }
    public String getMetodo() { return metodo; }
    public Object[] getArgumentos() { return argumentos; }

    @Override
    public String toString() {
        return "Requisicao{id=" + id + ", metodo=" + metodo + ", args=" + Arrays.toString(argumentos) + "}";
    }
}
