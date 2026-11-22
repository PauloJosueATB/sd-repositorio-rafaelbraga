package filas;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import protocolo.Json;

/** Produtor: conversa somente com o broker, nunca com o consumidor. */
public final class ClienteVotacaoFila {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : 7001;
        ClienteFilas broker = new ClienteFilas(host, porta);

        if (args.length > 2) {
            executarComArgumentos(broker, args);
            return;
        }

        Scanner entrada = new Scanner(System.in, "UTF-8");
        System.out.println("Cliente assíncrono ligado ao broker " + host + ":" + porta);
        System.out.println("Comandos: listar | votar LOGIN SENHA NUMERO | apurar | tempo | sair");
        while (true) {
            System.out.print("fila> ");
            if (!entrada.hasNextLine()) break;
            String[] p = entrada.nextLine().trim().split("\\s+");
            if (p.length == 0 || p[0].isEmpty()) continue;
            if (p[0].equalsIgnoreCase("sair")) break;
            try {
                if (p[0].equalsIgnoreCase("listar")) enviar(broker, "LISTAR_CANDIDATOS", null, null, null, 0);
                else if (p[0].equalsIgnoreCase("apurar")) enviar(broker, "APURAR", null, null, null, 0);
                else if (p[0].equalsIgnoreCase("tempo")) enviar(broker, "TEMPO_RESTANTE", null, null, null, 0);
                else if (p[0].equalsIgnoreCase("votar") && p.length == 4)
                    enviar(broker, "VOTAR", p[1], p[2], null, Integer.parseInt(p[3]));
                else System.out.println("Comando inválido.");
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }

    private static void executarComArgumentos(ClienteFilas broker, String[] args) throws Exception {
        String comando = args[2].toLowerCase();
        if ("listar".equals(comando)) enviar(broker, "LISTAR_CANDIDATOS", null, null, null, 0);
        else if ("apurar".equals(comando)) enviar(broker, "APURAR", null, null, null, 0);
        else if ("tempo".equals(comando)) enviar(broker, "TEMPO_RESTANTE", null, null, null, 0);
        else if ("votar".equals(comando) && args.length >= 6)
            enviar(broker, "VOTAR", args[3], args[4], null, Integer.parseInt(args[5]));
        else throw new IllegalArgumentException("Uso: ClienteVotacaoFila [host] [porta] listar|apurar|tempo|votar LOGIN SENHA NUMERO");
    }

    private static void enviar(ClienteFilas broker, String acao, String login, String senha, String ignorado, int numero) throws Exception {
        String correlationId = UUID.randomUUID().toString();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", acao);
        body.put("correlationId", correlationId);
        if (login != null) body.put("login", login);
        if (senha != null) body.put("senha", senha);
        if (acao.equals("VOTAR")) body.put("numero", numero);

        Map<String, Object> comando = new LinkedHashMap<>();
        comando.put("op", "ENQUEUE");
        comando.put("queue", "votacao-requests");
        comando.put("body", body);
        Map<String, Object> postado = broker.executar(comando);
        if (!Boolean.TRUE.equals(postado.get("ok"))) throw new RuntimeException("Falha ao postar mensagem");
        System.out.println("Postado na fila (correlationId=" + correlationId + ", messageId=" + postado.get("id") + ")");

        long prazo = System.currentTimeMillis() + 30000;
        while (System.currentTimeMillis() < prazo) {
            Map<String, Object> poll = new LinkedHashMap<>();
            poll.put("op", "POLL_RESPONSE");
            poll.put("queue", "votacao-responses");
            poll.put("correlationId", correlationId);
            Map<String, Object> resultado = broker.executar(poll);
            if (Boolean.TRUE.equals(resultado.get("found"))) {
                System.out.println(Json.escrever(resultado.get("body")));
                return;
            }
            Thread.sleep(300);
        }
        System.out.println("Resposta ainda não disponível. Ela pode ser recebida ao enviar o mesmo correlationId novamente? "
                + "Use o mesmo cliente/execução; o broker mantém a resposta persistida.");
    }
}
