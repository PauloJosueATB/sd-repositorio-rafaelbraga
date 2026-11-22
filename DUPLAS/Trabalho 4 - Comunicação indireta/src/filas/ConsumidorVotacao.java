package filas;

import java.nio.file.Path;
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
        Path dados = Paths.get("dados");
        RepositorioEleicao repo = CarregadorDados.carregar(dados, duracao);
        ServicoVotacao servico = new ServicoVotacao(repo);
        ClienteFilas broker = new ClienteFilas(host, porta);

        System.out.println("[" + LocalTime.now().format(HORA) + "] Consumidor ativo. Prazo: " + duracao + " s.");
        while (true) {
            Map<String, Object> comando = new LinkedHashMap<>();
            comando.put("op", "DEQUEUE");
            comando.put("queue", "votacao-requests");
            Map<String, Object> retirada = broker.executar(comando);
            if (Boolean.TRUE.equals(retirada.get("empty"))) {
                Thread.sleep(500);
                continue;
            }
            String id = String.valueOf(retirada.get("id"));
            Map<String, Object> body = (Map<String, Object>) retirada.get("body");
            Map<String, Object> resposta;
            try {
                resposta = processar(body, servico);
            } catch (Exception e) {
                resposta = new LinkedHashMap<>();
                resposta.put("ok", false);
                resposta.put("erro", e.getMessage());
            }
            resposta.put("correlationId", String.valueOf(body.get("correlationId")));

            Map<String, Object> publicar = new LinkedHashMap<>();
            publicar.put("op", "ENQUEUE");
            publicar.put("queue", "votacao-responses");
            publicar.put("body", resposta);
            broker.executar(publicar);

            Map<String, Object> ack = new LinkedHashMap<>();
            ack.put("op", "ACK");
            ack.put("queue", "votacao-requests");
            ack.put("id", id);
            broker.executar(ack);
            System.out.println("[" + LocalTime.now().format(HORA) + "] Processada mensagem " + id
                    + " de ação " + body.get("action"));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> processar(Map<String, Object> body, ServicoVotacao servico) throws Exception {
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
                int numero = ((Number) body.get("numero")).intValue();
                modelo.Voto voto = servico.votar(autenticado, numero);
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
