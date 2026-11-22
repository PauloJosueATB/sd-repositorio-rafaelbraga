package filas;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import protocolo.Json;

/** Broker de filas persistente em disco. Não conhece produtor nem consumidor específicos. */
public final class BrokerFilas {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Path raiz;

    public BrokerFilas(Path raiz) {
        this.raiz = raiz;
    }

    public static void main(String[] args) throws Exception {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 7001;
        Path raiz = args.length > 1 ? Paths.get(args[1]) : Paths.get("dados-filas");
        BrokerFilas broker = new BrokerFilas(raiz);
        broker.recuperarEmVoo();
        broker.executar(porta);
    }

    private void executar(int porta) throws Exception {
        try (ServerSocket servidor = new ServerSocket(porta)) {
            System.out.println("[" + LocalTime.now().format(HORA) + "] Broker de filas na porta " + porta);
            System.out.println("Persistência: " + raiz.toAbsolutePath());
            System.out.println("Pressione Ctrl+C para encerrar.");
            while (true) {
                Socket cliente = servidor.accept();
                new Thread(() -> atender(cliente), "broker-cliente").start();
            }
        }
    }

    private void recuperarEmVoo() throws Exception {
        Files.createDirectories(raiz);
        if (!Files.isDirectory(raiz)) return;
        try (java.util.stream.Stream<Path> filas = Files.list(raiz)) {
            for (Path fila : filas.filter(Files::isDirectory).toArray(Path[]::new)) {
                try (java.util.stream.Stream<Path> arquivos = Files.list(fila)) {
                    arquivos.filter(p -> p.getFileName().toString().endsWith(".processing")).forEach(p -> {
                        try {
                            Path destino = p.resolveSibling(p.getFileName().toString().replace(".processing", ".pending"));
                            Files.move(p, destino, StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
    }

    private synchronized Map<String, Object> enfileirar(Map<String, Object> comando) throws Exception {
        String fila = texto(comando, "queue");
        Object body = comando.get("body");
        String id = UUID.randomUUID().toString();
        Path pasta = raiz.resolve(fila);
        Files.createDirectories(pasta);
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("id", id);
        envelope.put("body", body);
        Files.write(pasta.resolve(id + ".pending"), Json.escrever(envelope).getBytes(StandardCharsets.UTF_8));
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("ok", true);
        resposta.put("id", id);
        return resposta;
    }

    private synchronized Map<String, Object> desenfileirar(Map<String, Object> comando) throws Exception {
        String fila = texto(comando, "queue");
        Path pasta = raiz.resolve(fila);
        Files.createDirectories(pasta);
        try (java.util.stream.Stream<Path> arquivos = Files.list(pasta)) {
            List<Path> pendentes = new ArrayList<>();
            arquivos.filter(p -> p.getFileName().toString().endsWith(".pending")).forEach(pendentes::add);
            pendentes.sort(Comparator.comparing(p -> p.getFileName().toString()));
            if (pendentes.isEmpty()) {
                Map<String, Object> vazio = new LinkedHashMap<>();
                vazio.put("ok", true);
                vazio.put("empty", true);
                return vazio;
            }
            Path origem = pendentes.get(0);
            String nome = origem.getFileName().toString().replace(".pending", "");
            Path destino = origem.resolveSibling(nome + ".processing");
            Files.move(origem, destino, StandardCopyOption.REPLACE_EXISTING);
            Map<String, Object> envelope = (Map<String, Object>) Json.ler(new String(Files.readAllBytes(destino), StandardCharsets.UTF_8));
            Map<String, Object> resposta = new LinkedHashMap<>();
            resposta.put("ok", true);
            resposta.put("empty", false);
            resposta.put("id", envelope.get("id"));
            resposta.put("body", envelope.get("body"));
            return resposta;
        }
    }

    private synchronized Map<String, Object> confirmar(Map<String, Object> comando) throws Exception {
        String fila = texto(comando, "queue");
        String id = texto(comando, "id");
        Path processamento = raiz.resolve(fila).resolve(id + ".processing");
        boolean removido = Files.deleteIfExists(processamento);
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("ok", removido);
        return resposta;
    }

    private synchronized Map<String, Object> negar(Map<String, Object> comando) throws Exception {
        String fila = texto(comando, "queue");
        String id = texto(comando, "id");
        Path processamento = raiz.resolve(fila).resolve(id + ".processing");
        Path pendente = raiz.resolve(fila).resolve(id + ".pending");
        if (Files.exists(processamento)) {
            Files.move(processamento, pendente, StandardCopyOption.REPLACE_EXISTING);
        }
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("ok", true);
        return resposta;
    }

    private synchronized Map<String, Object> buscarResposta(Map<String, Object> comando) throws Exception {
        String fila = texto(comando, "queue");
        String correlationId = texto(comando, "correlationId");
        Path pasta = raiz.resolve(fila);
        Files.createDirectories(pasta);
        try (java.util.stream.Stream<Path> arquivos = Files.list(pasta)) {
            List<Path> pendentes = new ArrayList<>();
            arquivos.filter(p -> p.getFileName().toString().endsWith(".pending")).forEach(pendentes::add);
            pendentes.sort(Comparator.comparing(p -> p.getFileName().toString()));
            for (Path pendente : pendentes) {
                Map<String, Object> envelope = (Map<String, Object>) Json.ler(new String(Files.readAllBytes(pendente), StandardCharsets.UTF_8));
                Object body = envelope.get("body");
                if (body instanceof Map) {
                    Map<String, Object> b = (Map<String, Object>) body;
                    if (correlationId.equals(String.valueOf(b.get("correlationId")))) {
                        Files.delete(pendente);
                        Map<String, Object> resposta = new LinkedHashMap<>();
                        resposta.put("ok", true);
                        resposta.put("found", true);
                        resposta.put("body", body);
                        return resposta;
                    }
                }
            }
        }
        Map<String, Object> vazio = new LinkedHashMap<>();
        vazio.put("ok", true);
        vazio.put("found", false);
        return vazio;
    }

    private void atender(Socket socket) {
        try (Socket s = socket;
             DataInputStream entrada = new DataInputStream(s.getInputStream());
             DataOutputStream saida = new DataOutputStream(s.getOutputStream())) {
            String textoJson = entrada.readUTF();
            Map<String, Object> comando = (Map<String, Object>) Json.ler(textoJson);
            String op = texto(comando, "op");
            Map<String, Object> resposta;
            switch (op) {
                case "ENQUEUE": resposta = enfileirar(comando); break;
                case "DEQUEUE": resposta = desenfileirar(comando); break;
                case "ACK": resposta = confirmar(comando); break;
                case "NACK": resposta = negar(comando); break;
                case "POLL_RESPONSE": resposta = buscarResposta(comando); break;
                default:
                    resposta = new LinkedHashMap<>(); resposta.put("ok", false); resposta.put("erro", "Operação desconhecida");
            }
            saida.writeUTF(Json.escrever(resposta));
            saida.flush();
        } catch (Exception e) {
            System.err.println("Erro no cliente do broker: " + e.getMessage());
        }
    }

    private static String texto(Map<String, Object> mapa, String chave) {
        Object v = mapa.get(chave);
        return v == null ? "" : String.valueOf(v);
    }
}
