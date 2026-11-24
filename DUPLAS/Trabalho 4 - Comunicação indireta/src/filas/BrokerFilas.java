package filas;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import protocolo.ArquivoAtomico;
import protocolo.Json;

/** Broker de filas persistente em disco. Não conhece produtor nem consumidor específicos. */
public final class BrokerFilas {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final long PRAZO_ACK = TimeUnit.SECONDS.toNanos(5);
    private final Path raiz;
    private final Map<Path, Entrega> entregas = new HashMap<>();

    private static final class Entrega {
        private final String recibo = UUID.randomUUID().toString();
        private final long expiraEm = System.nanoTime() + PRAZO_ACK;
    }

    public BrokerFilas(Path raiz) {
        this.raiz = raiz;
    }

    public static void main(String[] args) throws Exception {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 7001;
        Path raiz = args.length > 1 ? Paths.get(args[1]) : Paths.get("dados-filas");
        BrokerFilas broker = new BrokerFilas(raiz);
        Files.createDirectories(raiz);
        try (FileChannel trava = FileChannel.open(raiz.resolve("broker.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock exclusivo = trava.tryLock()) {
            if (exclusivo == null) {
                throw new IllegalStateException("Já existe um broker usando " + raiz);
            }
            broker.recuperarEmVoo();
            broker.executar(porta);
        }
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
        try (java.util.stream.Stream<Path> filas = Files.list(raiz)) {
            for (Path fila : filas.filter(Files::isDirectory).toArray(Path[]::new)) {
                try (java.util.stream.Stream<Path> arquivos = Files.list(fila)) {
                    for (Path processamento : arquivos.filter(p -> p.getFileName().toString()
                            .endsWith(".processing")).toArray(Path[]::new)) {
                        devolver(processamento);
                    }
                }
            }
        }
    }

    private synchronized Map<String, Object> enfileirar(Map<String, Object> comando) throws Exception {
        String fila = texto(comando, "queue");
        Object body = comando.get("body");
        String id = fila.equals("votacao-responses") && comando.containsKey("requestId")
                ? UUID.fromString(texto(comando, "requestId")).toString()
                : UUID.randomUUID().toString();
        Path pasta = pastaFila(fila);
        Files.createDirectories(pasta);
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("id", id);
        envelope.put("body", body);
        Path destino = pasta.resolve(id + ".pending");
        if (!Files.exists(destino)) {
            ArquivoAtomico.gravar(destino, Json.escrever(envelope));
        }
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("ok", true);
        resposta.put("id", id);
        return resposta;
    }

    @SuppressWarnings("unchecked")
    private synchronized Map<String, Object> desenfileirar(Map<String, Object> comando) throws Exception {
        devolverExpiradas();
        String fila = texto(comando, "queue");
        Path pasta = pastaFila(fila);
        Files.createDirectories(pasta);
        try (java.util.stream.Stream<Path> arquivos = Files.list(pasta)) {
            List<Path> pendentes = new ArrayList<>();
            arquivos.filter(p -> p.getFileName().toString().endsWith(".pending")).forEach(pendentes::add);
            pendentes.sort(Comparator.comparingLong((Path p) -> p.toFile().lastModified())
                    .thenComparing(p -> p.getFileName().toString()));
            if (pendentes.isEmpty()) {
                Map<String, Object> vazio = new LinkedHashMap<>();
                vazio.put("ok", true);
                vazio.put("empty", true);
                return vazio;
            }
            Path origem = pendentes.get(0);
            String nome = origem.getFileName().toString().replace(".pending", "");
            Path destino = origem.resolveSibling(nome + ".processing");
            Map<String, Object> envelope = (Map<String, Object>) Json.ler(Files.readString(origem, StandardCharsets.UTF_8));
            Files.move(origem, destino, StandardCopyOption.ATOMIC_MOVE);
            Entrega entrega = new Entrega();
            entregas.put(destino, entrega);
            Map<String, Object> resposta = new LinkedHashMap<>();
            resposta.put("ok", true);
            resposta.put("empty", false);
            resposta.put("id", envelope.get("id"));
            resposta.put("receipt", entrega.recibo);
            resposta.put("body", envelope.get("body"));
            return resposta;
        }
    }

    private void devolverExpiradas() throws Exception {
        var iterador = entregas.entrySet().iterator();
        while (iterador.hasNext()) {
            var entrada = iterador.next();
            if (System.nanoTime() - entrada.getValue().expiraEm >= 0) {
                devolver(entrada.getKey());
                iterador.remove();
            }
        }
    }

    private void devolver(Path processamento) throws Exception {
        String nome = processamento.getFileName().toString().replace(".processing", ".pending");
        Files.move(processamento, processamento.resolveSibling(nome), StandardCopyOption.ATOMIC_MOVE);
    }

    private synchronized Map<String, Object> finalizar(Map<String, Object> comando, boolean confirmar) throws Exception {
        devolverExpiradas();
        Path pasta = pastaFila(texto(comando, "queue"));
        String id = UUID.fromString(texto(comando, "id")).toString();
        Path processamento = pasta.resolve(id + ".processing");
        Entrega entrega = entregas.get(processamento);
        if (entrega == null || !entrega.recibo.equals(texto(comando, "receipt"))) {
            return Map.of("ok", false);
        }
        if (confirmar) {
            Files.delete(processamento);
        } else {
            devolver(processamento);
        }
        entregas.remove(processamento);
        return Map.of("ok", true);
    }

    private Path pastaFila(String fila) {
        if (!fila.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException("Nome de fila inválido.");
        }
        return raiz.resolve(fila);
    }

    @SuppressWarnings("unchecked")
    private synchronized Map<String, Object> buscarResposta(Map<String, Object> comando) throws Exception {
        String fila = texto(comando, "queue");
        String correlationId = texto(comando, "correlationId");
        Path pasta = pastaFila(fila);
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

    @SuppressWarnings("unchecked")
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
                case "ACK": resposta = finalizar(comando, true); break;
                case "NACK": resposta = finalizar(comando, false); break;
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
