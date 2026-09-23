package modelo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Classe de MODELO que implementa o serviço usado pelos ELEITORES:
 * autenticar, listar candidatos, votar e apurar o resultado.
 * É usada tanto pelo serviço remoto da Questão 4 quanto pelo servidor da Questão 5.
 */
public class ServicoVotacao {
    private final RepositorioEleicao repo;

    public ServicoVotacao(RepositorioEleicao repo) {
        this.repo = repo;
    }

    public Eleicao getEleicao() {
        return repo.getEleicao();
    }

    /** Valida login/senha. Devolve uma cópia do usuário SEM a senha. */
    public Usuario autenticar(String login, String senha) throws ServicoException {
        if (login == null || senha == null || login.trim().isEmpty()) {
            throw new ServicoException("Login e senha são obrigatórios.");
        }
        Usuario u = repo.buscarUsuario(login.trim());
        boolean ok = u != null && MessageDigest.isEqual(
                u.getSenha().getBytes(StandardCharsets.UTF_8), senha.getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            throw new ServicoException("Login ou senha inválidos.");
        }
        return new Usuario(u.getLogin(), "", u.getNome(), u.getPerfil());
    }

    public boolean votacaoAberta() {
        return System.currentTimeMillis() < repo.getEleicao().getFim();
    }

    public long tempoRestanteMs() {
        return Math.max(0L, repo.getEleicao().getFim() - System.currentTimeMillis());
    }

    public List<Candidato> listarCandidatos() {
        return repo.listarCandidatos();
    }

    /**
     * Registra o voto de um eleitor. Regras: apenas ELEITOR vota; somente antes do
     * prazo; o candidato precisa existir; cada eleitor vota uma única vez.
     */
    public Voto votar(Usuario usuario, int numeroCandidato) throws ServicoException {
        synchronized (repo) {
            if (usuario == null) {
                throw new ServicoException("Usuário não autenticado.");
            }
            if (usuario.getPerfil() != Perfil.ELEITOR) {
                throw new ServicoException("Apenas eleitores podem votar.");
            }
            if (!votacaoAberta()) {
                throw new ServicoException("A votação foi encerrada: votos não são mais aceitos.");
            }
            if (repo.buscarCandidato(numeroCandidato) == null) {
                throw new ServicoException("Candidato inexistente: " + numeroCandidato + ".");
            }
            if (repo.jaVotou(usuario.getLogin())) {
                throw new ServicoException("Você já votou nesta eleição.");
            }
            Voto voto = new Voto(UUID.randomUUID().toString(), numeroCandidato, System.currentTimeMillis());
            repo.registrarVoto(usuario.getLogin(), voto);
            return voto;
        }
    }

    /**
     * Calcula total de votos, percentual por candidato e vencedor(es).
     * Só é liberada depois de encerrado o prazo.
     */
    public Resultado apurar() throws ServicoException {
        synchronized (repo) {
            if (votacaoAberta()) {
                throw new ServicoException("A votação ainda está aberta (restam "
                        + Formatadores.duracao(tempoRestanteMs()) + "). O resultado só é liberado após o prazo.");
            }
            int total = repo.totalVotos();
            List<ItemResultado> itens = new ArrayList<>();
            for (Candidato c : repo.listarCandidatos()) {
                int votos = repo.contarVotos(c.getNumero());
                double percentual = total == 0 ? 0.0 : votos * 100.0 / total;
                itens.add(new ItemResultado(c, votos, percentual));
            }
            itens.sort(Comparator.comparingInt(ItemResultado::getVotos).reversed()
                    .thenComparingInt(i -> i.getCandidato().getNumero()));

            List<Candidato> vencedores = new ArrayList<>();
            if (!itens.isEmpty() && itens.get(0).getVotos() > 0) {
                int maximo = itens.get(0).getVotos();
                for (ItemResultado i : itens) {
                    if (i.getVotos() == maximo) {
                        vencedores.add(i.getCandidato());
                    }
                }
            }
            return new Resultado(total, itens, vencedores);
        }
    }
}
