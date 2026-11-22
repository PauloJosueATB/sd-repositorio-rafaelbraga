package modelo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * "Data" da arquitetura (Figura 1 do enunciado): guarda, em memória, os usuários,
 * candidatos, votos e notas. Todos os métodos são {@code synchronized} no próprio
 * repositório; os serviços usam {@code synchronized (repositorio)} para tornar
 * atômicas operações compostas (ex.: "verifica se já votou + registra o voto").
 */
public class RepositorioEleicao {
    private final Eleicao eleicao;
    private final Map<String, Usuario> usuarios = new HashMap<>();
    private final Map<Integer, Candidato> candidatos = new TreeMap<>();
    private final List<Voto> votos = new ArrayList<>();
    private final Set<String> quemJaVotou = new HashSet<>();
    private final List<NotaInformativa> notas = new ArrayList<>();

    public RepositorioEleicao(Eleicao eleicao) {
        this.eleicao = eleicao;
    }

    public Eleicao getEleicao() {
        return eleicao;
    }

    // ---------------- usuários ----------------

    public synchronized void adicionarUsuario(Usuario u) {
        usuarios.put(u.getLogin(), u);
    }

    public synchronized Usuario buscarUsuario(String login) {
        return usuarios.get(login);
    }

    // ---------------- candidatos ----------------

    /** @return false se já existir candidato com o mesmo número. */
    public synchronized boolean adicionarCandidato(Candidato c) {
        if (candidatos.containsKey(c.getNumero())) {
            return false;
        }
        candidatos.put(c.getNumero(), copia(c));
        return true;
    }

    /** @return o candidato removido, ou null se não existia. */
    public synchronized Candidato removerCandidato(int numero) {
        return candidatos.remove(numero);
    }

    public synchronized Candidato buscarCandidato(int numero) {
        Candidato c = candidatos.get(numero);
        return c == null ? null : copia(c);
    }

    /** Lista (cópia) ordenada pelo número do candidato. */
    public synchronized List<Candidato> listarCandidatos() {
        List<Candidato> lista = new ArrayList<>();
        for (Candidato c : candidatos.values()) {
            lista.add(copia(c));
        }
        return lista;
    }

    private static Candidato copia(Candidato c) {
        return new Candidato(c.getNumero(), c.getNome(), c.getPartido());
    }

    // ---------------- votos ----------------

    public synchronized boolean jaVotou(String login) {
        return quemJaVotou.contains(login);
    }

    public synchronized void registrarVoto(String login, Voto voto) {
        quemJaVotou.add(login);
        votos.add(voto);
    }

    public synchronized int contarVotos(int numeroCandidato) {
        int n = 0;
        for (Voto v : votos) {
            if (v.getNumeroCandidato() == numeroCandidato) {
                n++;
            }
        }
        return n;
    }

    public synchronized int totalVotos() {
        return votos.size();
    }

    // ---------------- notas ----------------

    public synchronized void registrarNota(NotaInformativa nota) {
        notas.add(nota);
    }

    public synchronized List<NotaInformativa> listarNotas() {
        return new ArrayList<>(notas);
    }
}
