package modelo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import protocolo.ArquivoAtomico;
import protocolo.Json;

/** Armazena a eleição e mantém um registro persistente dos votos aceitos. */
public class RepositorioEleicao {
    private final Eleicao eleicao;
    private final Map<String, Usuario> usuarios = new HashMap<>();
    private final Map<Integer, Candidato> candidatos = new TreeMap<>();
    private final Map<String, Voto> votos = new HashMap<>();
    private Path arquivoEstado;
    private final Set<String> quemJaVotou = new HashSet<>();
    private final List<NotaInformativa> notas = new ArrayList<>();

    public RepositorioEleicao(Eleicao eleicao) {
        this.eleicao = eleicao;
    }

    @SuppressWarnings("unchecked")
    public synchronized void abrirEstado(Path arquivo) throws IOException {
        if (Files.exists(arquivo)) {
            Map<String, Object> estado = (Map<String, Object>) Json.ler(Files.readString(arquivo));
            eleicao.setInicio(((Number) estado.get("inicio")).longValue());
            eleicao.setFim(((Number) estado.get("fim")).longValue());
            quemJaVotou.addAll((List<String>) estado.get("eleitores"));
            for (Map<String, Object> item : (List<Map<String, Object>>) estado.get("votos")) {
                votos.put((String) item.get("requisicao"), new Voto((String) item.get("comprovante"),
                        ((Number) item.get("candidato")).intValue(), ((Number) item.get("instante")).longValue()));
            }
        } else {
            gravarEstado(arquivo, quemJaVotou, votos);
        }
        arquivoEstado = arquivo;
    }

    private void gravarEstado(Path arquivo, Collection<String> eleitores, Map<String, Voto> votos) throws IOException {
        List<Map<String, Object>> registros = new ArrayList<>();
        votos.forEach((id, voto) -> registros.add(Map.of(
                "requisicao", id, "comprovante", voto.getComprovante(),
                "candidato", voto.getNumeroCandidato(), "instante", voto.getInstante())));
        ArquivoAtomico.gravar(arquivo, Json.escrever(Map.of(
                "inicio", eleicao.getInicio(), "fim", eleicao.getFim(),
                "eleitores", new ArrayList<>(new TreeSet<>(eleitores)), "votos", registros)));
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

    public synchronized Voto buscarVotoPorRequisicao(String id) {
        return votos.get(id);
    }

    public synchronized void registrarVoto(String id, String login, Voto voto) throws IOException {
        if (arquivoEstado != null) {
            Set<String> eleitores = new HashSet<>(quemJaVotou);
            eleitores.add(login);
            Map<String, Voto> atualizados = new HashMap<>(votos);
            atualizados.put(id, voto);
            gravarEstado(arquivoEstado, eleitores, atualizados);
        }
        quemJaVotou.add(login);
        votos.put(id, voto);
    }

    public synchronized int contarVotos(int numeroCandidato) {
        int n = 0;
        for (Voto v : votos.values()) {
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
