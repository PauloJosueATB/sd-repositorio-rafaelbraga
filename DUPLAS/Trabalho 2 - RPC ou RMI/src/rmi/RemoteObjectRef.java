package rmi;

/**
 * Referência a um objeto remoto, no papel do {@code RemoteObjectRef} da Figura 5.4
 * do livro-texto: identifica QUEM vai atender a chamada (o stub RMI obtido no
 * registro) e COM QUE NOME o objeto foi publicado. É este objeto — e não o
 * conteúdo do serviço — que é passado por referência entre cliente e servidor: o
 * stub nunca é copiado, ele sempre aponta de volta para o único objeto remoto
 * que vive no servidor (ver {@link Invocador}).
 */
public final class RemoteObjectRef {
    private final String nomeObjeto;
    private final ComunicadorRemoto stub;

    public RemoteObjectRef(String nomeObjeto, ComunicadorRemoto stub) {
        this.nomeObjeto = nomeObjeto;
        this.stub = stub;
    }

    public String getNomeObjeto() {
        return nomeObjeto;
    }

    public ComunicadorRemoto getStub() {
        return stub;
    }
}
