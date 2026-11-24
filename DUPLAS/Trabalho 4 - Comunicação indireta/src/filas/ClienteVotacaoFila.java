package filas;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import protocolo.Json;

/** Produtor: conversa somente com o broker, nunca com o consumidor. */
public final class ClienteVotacaoFila {
    private static final String COMANDOS = "[enfileirar] listar | votar LOGIN SENHA NUMERO | apurar | tempo"
            + " | consultar CORRELATION_ID";

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : 7001;
        ClienteFilas broker = new ClienteFilas(host, porta);

        if (args.length > 2) {
            executar(broker, Arrays.copyOfRange(args, 2, args.length));
            return;
        }

        System.out.println("Cliente ligado ao broker " + host + ":" + porta);
        System.out.println("Comandos: " + COMANDOS + " | sair");
        try (Scanner entrada = new Scanner(System.in, "UTF-8")) {
            while (true) {
                System.out.print("fila> ");
                if (!entrada.hasNextLine()) break;
                String linha = entrada.nextLine().trim();
                if (linha.isEmpty()) continue;
                if (linha.equalsIgnoreCase("sair")) break;
                try {
                    executar(broker, linha.split("\\s+"));
                } catch (Exception e) {
                    System.out.println("Erro: " + e.getMessage());
                }
            }
        }
    }

    private static void executar(ClienteFilas broker, String[] args) throws Exception {
        if (args[0].equalsIgnoreCase("consultar") && args.length == 2) {
            if (!consultar(broker, args[1])) {
                System.out.println("Resposta ainda indisponível para " + args[1]);
            }
            return;
        }

        boolean aguardar = !args[0].equalsIgnoreCase("enfileirar");
        int inicio = aguardar ? 0 : 1;
        if (inicio == args.length) {
            throw new IllegalArgumentException("Uso: " + COMANDOS);
        }

        String comando = args[inicio].toLowerCase(Locale.ROOT);
        String acao;
        switch (comando) {
            case "listar": acao = "LISTAR_CANDIDATOS"; break;
            case "votar": acao = "VOTAR"; break;
            case "apurar": acao = "APURAR"; break;
            case "tempo": acao = "TEMPO_RESTANTE"; break;
            default: throw new IllegalArgumentException("Uso: " + COMANDOS);
        }
        int argumentos = comando.equals("votar") ? 4 : 1;
        if (args.length - inicio != argumentos) {
            throw new IllegalArgumentException("Uso: " + COMANDOS);
        }

        String correlationId = UUID.randomUUID().toString();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", acao);
        body.put("correlationId", correlationId);
        if (comando.equals("votar")) {
            body.put("login", args[inicio + 1]);
            body.put("senha", args[inicio + 2]);
            body.put("numero", Integer.parseInt(args[inicio + 3]));
        }

        Map<String, Object> postado = broker.executar(Map.of("op", "ENQUEUE",
                "queue", "votacao-requests", "body", body));
        if (!Boolean.TRUE.equals(postado.get("ok"))) {
            throw new IllegalStateException("Falha ao postar mensagem");
        }
        System.out.println("Postado na fila (correlationId=" + correlationId + ", messageId=" + postado.get("id") + ")");
        if (aguardar) {
            long prazo = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            while (System.nanoTime() - prazo < 0) {
                if (consultar(broker, correlationId)) return;
                Thread.sleep(300);
            }
            System.out.println("Resposta ainda indisponível. Consulte depois com: consultar " + correlationId);
        }
    }

    private static boolean consultar(ClienteFilas broker, String correlationId) throws Exception {
        Map<String, Object> resultado = broker.executar(Map.of("op", "POLL_RESPONSE",
                "queue", "votacao-responses", "correlationId", correlationId));
        if (!Boolean.TRUE.equals(resultado.get("found"))) {
            return false;
        }
        System.out.println(Json.escrever(resultado.get("body")));
        return true;
    }
}
