package modelo;

/**
 * Classe de MODELO que implementa o serviço usado pelos ADMINISTRADORES:
 * introduzir/remover candidatos e criar notas informativas para os eleitores.
 * (O envio da nota por multicast UDP é feito pela camada de rede do servidor.)
 */
public class ServicoAdministracao {
    private static final int TAMANHO_MAXIMO_NOTA = 500;

    private final RepositorioEleicao repo;

    public ServicoAdministracao(RepositorioEleicao repo) {
        this.repo = repo;
    }

    public Candidato adicionarCandidato(Usuario admin, int numero, String nome, String partido)
            throws ServicoException {
        synchronized (repo) {
            exigirAdministrador(admin);
            exigirVotacaoAberta();
            if (numero <= 0) {
                throw new ServicoException("O número do candidato deve ser positivo.");
            }
            if (nome == null || nome.trim().isEmpty()) {
                throw new ServicoException("O nome do candidato é obrigatório.");
            }
            Candidato c = new Candidato(numero, nome.trim(), partido == null ? "" : partido.trim());
            if (!repo.adicionarCandidato(c)) {
                throw new ServicoException("Já existe um candidato com o número " + numero + ".");
            }
            return c;
        }
    }

    /** Não permite remover candidato que já recebeu votos (preserva a apuração). */
    public Candidato removerCandidato(Usuario admin, int numero) throws ServicoException {
        synchronized (repo) {
            exigirAdministrador(admin);
            exigirVotacaoAberta();
            if (repo.contarVotos(numero) > 0) {
                throw new ServicoException("Não é possível remover: o candidato " + numero + " já recebeu votos.");
            }
            Candidato removido = repo.removerCandidato(numero);
            if (removido == null) {
                throw new ServicoException("Candidato inexistente: " + numero + ".");
            }
            return removido;
        }
    }

    public NotaInformativa criarNota(Usuario admin, String texto) throws ServicoException {
        exigirAdministrador(admin);
        if (texto == null || texto.trim().isEmpty()) {
            throw new ServicoException("O texto da nota é obrigatório.");
        }
        if (texto.length() > TAMANHO_MAXIMO_NOTA) {
            throw new ServicoException("A nota excede " + TAMANHO_MAXIMO_NOTA + " caracteres.");
        }
        NotaInformativa nota = new NotaInformativa(admin.getLogin(), texto.trim(), System.currentTimeMillis());
        repo.registrarNota(nota);
        return nota;
    }

    private void exigirAdministrador(Usuario u) throws ServicoException {
        if (u == null) {
            throw new ServicoException("Usuário não autenticado.");
        }
        if (u.getPerfil() != Perfil.ADMINISTRADOR) {
            throw new ServicoException("Operação restrita a administradores.");
        }
    }

    private void exigirVotacaoAberta() throws ServicoException {
        if (System.currentTimeMillis() >= repo.getEleicao().getFim()) {
            throw new ServicoException("A votação foi encerrada: a lista de candidatos não pode mais ser alterada.");
        }
    }
}
