package modelo;

/** Erro de regra de negócio (login inválido, votação encerrada, voto duplicado etc.). */
public class ServicoException extends Exception {
    private static final long serialVersionUID = 1L;

    public ServicoException(String mensagem) {
        super(mensagem);
    }
}
