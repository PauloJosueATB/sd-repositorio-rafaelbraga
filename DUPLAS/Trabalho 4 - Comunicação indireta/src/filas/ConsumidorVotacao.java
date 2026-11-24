package filas;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import modelo.Candidato;
import modelo.CarregadorDados;
import modelo.RepositorioEleicao;
import modelo.ServicoException;
import modelo.ServicoVotacao;
import modelo.Usuario;
import modelo.Resultado;
import modelo.ItemResultado;

/** Consumidor assíncrono: processa a fila de votação e publica respostas. */
public final class ConsumidorVotacao {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : 7001;
        int duracao = args.length > 2 ? Integer.parseInt(args[2]) : 120;
        Path estado = Paths.get(args.length > 3 ? args[3] : "estado-eleicao");
        Files.createDirectories(estado);
        try (FileChannel trava = FileChannel.open(estado.resolve("consumidor.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock exclusivo = trava.tryLock()) {
            if (exclusivo == null) {
                throw new IllegalStateException("Já existe um consumidor usando " + estado);
            }
            RepositorioEleicao repo = CarregadorDados.carregar(Paths.get("dados"), duracao);
            repo.abrirEstado(estado.resolve("eleicao.json"));
            ServicoVotacao servico = new ServicoVotacao(repo);
            System.out.println("[" + LocalTime.now().format(HORA) + "] Consumidor ativo. Prazo restante: "
                    + servico.tempoRestanteMs() + " ms.");
            consumir(new ClienteFilas(host, porta), servico);
        }
    }

    @SuppressWarnings("unchecked")
    private static void consumir(ClienteFilas broker, ServicoVotacao servico) throws Exception {
        while (true) {
            try {
                Map<String, Object> retirada = broker.executar(Map.of("op", "DEQUEUE", "queue", "votacao-requests"));
                if (Boolean.TRUE.equals(retirada.get("empty"))) {
                    Thread.sleep(500);
                    continue;
                }

                String id = (String) retirada.get("id");
                Map<String, Object> body = (Map<String, Object>) retirada.get("body");
                Map<String, Object> resposta = new LinkedHashMap<>();
                try {
                    resposta.putAll(processar(body, servico, id));
                } catch (ServicoException | IllegalArgumentException e) {
                    resposta.put("ok", false);
                    resposta.put("erro", e.getMessage());
                }
                resposta.put("correlationId", String.valueOf(body.get("correlationId")));
                Map<String, Object> publicada = broker.executar(Map.of("op", "ENQUEUE",
                        "queue", "votacao-responses", "requestId", id, "body", resposta));
                if (!Boolean.TRUE.equals(publicada.get("ok"))) {
                    throw new IOException("O broker não confirmou a resposta de " + id);
                }
                Map<String, Object> ack = broker.executar(Map.of("op", "ACK", "queue", "votacao-requests",
                        "id", id, "receipt", retirada.get("receipt")));
                if (Boolean.TRUE.equals(ack.get("ok"))) {
                    System.out.println("[" + LocalTime.now().format(HORA) + "] Processada mensagem " + id
                            + " de ação " + body.get("action"));
                }
            } catch (IOException e) {
                System.err.println("Falha temporária no consumidor: " + e.getMessage());
                Thread.sleep(500);
            }
        }
    }

    private static Map<String, Object> processar(Map<String, Object> body, ServicoVotacao servico,
            String id) throws ServicoException, IOException {
        String acao = String.valueOf(body.get("action"));
        Map<String, Object> out = new LinkedHashMap<>();
        switch (acao) {
            case "LISTAR_CANDIDATOS":
                List<Map<String, Object>> candidatos = new ArrayList<>();
                for (Candidato c : servico.listarCandidatos()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("numero", c.getNumero()); m.put("nome", c.getNome()); m.put("partido", c.getPartido());
                    candidatos.add(m);
                }
                out.put("ok", true); out.put("candidatos", candidatos); break;
            case "VOTAR":
                Usuario autenticado = autenticar(body, servico);
                if (!(body.get("numero") instanceof Number)) {
                    throw new ServicoException("Número do candidato é obrigatório.");
                }
                int numero = ((Number) body.get("numero")).intValue();
                modelo.Voto voto = servico.votar(autenticado, numero, id);
                out.put("ok", true); out.put("comprovante", voto.getComprovante()); break;
            case "APURAR":
                Resultado r = servico.apurar();
                out.put("ok", true); out.put("totalVotos", r.getTotalVotos());
                List<Map<String, Object>> itens = new ArrayList<>();
                for (ItemResultado i : r.getItens()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("numero", i.getCandidato().getNumero()); m.put("nome", i.getCandidato().getNome());
                    m.put("votos", i.getVotos()); m.put("percentual", i.getPercentual()); itens.add(m);
                }
                out.put("itens", itens); break;
            case "TEMPO_RESTANTE":
                out.put("ok", true); out.put("tempoRestanteMs", servico.tempoRestanteMs()); break;
            default:
                throw new ServicoException("Ação desconhecida: " + acao);
        }
        return out;
    }

    private static Usuario autenticar(Map<String, Object> body, ServicoVotacao servico) throws ServicoException {
        String login = String.valueOf(body.get("login"));
        String senha = String.valueOf(body.get("senha"));
        Usuario u = servico.autenticar(login, senha);
        if (u.getPerfil() != modelo.Perfil.ELEITOR) {
            throw new ServicoException("Apenas eleitores podem votar.");
        }
        return u;
    }
}
