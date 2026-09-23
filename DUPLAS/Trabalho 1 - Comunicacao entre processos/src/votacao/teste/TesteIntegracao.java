package votacao.teste;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import modelo.CarregadorDados;
import modelo.Resultado;
import votacao.cliente.ConexaoServidor;
import votacao.cliente.ReceptorMulticast;
import votacao.protocolo.Conversores;
import votacao.protocolo.Mensagem;
import votacao.protocolo.Protocolo;
import votacao.servidor.ServidorVotacao;

/**
 * Teste de integração da Questão 5: sobe o servidor no próprio processo (prazo de 6 s) e exercita,
 * via sockets reais, login, votação concorrente, administração, multicast UDP e apuração.
 *
 * <pre>java votacao.teste.TesteIntegracao</pre>
 */
public class TesteIntegracao {
    private static int falhas = 0;
    private static final String GRUPO = "230.0.0.77";
    private static final int PORTA_MULTICAST = 4477;
    private static final int DURACAO_SEGUNDOS = 6;

    public static void main(String[] args) throws Exception {
        ServidorVotacao servidor = new ServidorVotacao(CarregadorDados.padrao(DURACAO_SEGUNDOS), GRUPO, PORTA_MULTICAST);
        int porta = servidor.iniciar(0);
        long inicio = System.currentTimeMillis();

        // ---------------- multicast: um "eleitor" escutando o grupo ----------------
        List<String> notasRecebidas = new ArrayList<>();
        CountDownLatch recebeuNota = new CountDownLatch(1);
        ReceptorMulticast receptor = new ReceptorMulticast(GRUPO, PORTA_MULTICAST, nota -> {
            synchronized (notasRecebidas) {
                notasRecebidas.add(nota.getAutor() + ": " + nota.getTexto());
            }
            recebeuNota.countDown();
        });
        boolean multicastDisponivel = true;
        try {
            receptor.entrar();
        } catch (IOException | RuntimeException e) {
            multicastDisponivel = false;
            System.out.println("[AVISO] multicast indisponível neste ambiente (" + e.getMessage() + "); testes UDP serão pulados.");
        }

        // ---------------- login ----------------
        try (ConexaoServidor c = new ConexaoServidor("localhost", porta)) {
            verificar(!c.enviar(Mensagem.requisicao(Protocolo.OP_LISTAR_CANDIDATOS)).isOk(), "operações exigem login");
            Mensagem ruim = c.enviar(Mensagem.requisicao(Protocolo.OP_LOGIN).com("login", "eleitor1").com("senha", "errada"));
            verificar(!ruim.isOk() && ruim.getString("erro").contains("inválidos"), "login com senha errada é recusado");
            Mensagem bom = c.enviar(Mensagem.requisicao(Protocolo.OP_LOGIN).com("login", "eleitor1").com("senha", "123"));
            verificar(bom.isOk() && "ELEITOR".equals(bom.getString("perfil")), "login de eleitor");
            verificar(Conversores.candidatos(bom.getLista("candidatos")).size() == 3, "login devolve a lista de candidatos");
            verificar(GRUPO.equals(bom.getString("grupoMulticast")) && bom.getInt("portaMulticast", 0) == PORTA_MULTICAST,
                    "login informa o grupo multicast");
            Mensagem lixo = c.enviar(new Mensagem().com("nada", 1));
            verificar(!lixo.isOk(), "mensagem sem 'op' é recusada");
        }

        // ---------------- administração + nota multicast ----------------
        try (ConexaoServidor admin = new ConexaoServidor("localhost", porta);
             ConexaoServidor eleitor = new ConexaoServidor("localhost", porta)) {
            admin.enviar(Mensagem.requisicao(Protocolo.OP_LOGIN).com("login", "admin").com("senha", "admin123"));
            eleitor.enviar(Mensagem.requisicao(Protocolo.OP_LOGIN).com("login", "eleitor5").com("senha", "123"));

            verificar(!eleitor.enviar(Mensagem.requisicao(Protocolo.OP_ADICIONAR_CANDIDATO)
                    .com("numero", 99).com("nome", "X").com("partido", "Y")).isOk(), "eleitor não pode adicionar candidato");
            verificar(!eleitor.enviar(Mensagem.requisicao(Protocolo.OP_ENVIAR_NOTA).com("texto", "oi")).isOk(),
                    "eleitor não pode enviar nota");
            verificar(!admin.enviar(Mensagem.requisicao(Protocolo.OP_VOTAR).com("candidato", 13)).isOk(),
                    "administrador não pode votar");

            verificar(admin.enviar(Mensagem.requisicao(Protocolo.OP_ADICIONAR_CANDIDATO)
                    .com("numero", 99).com("nome", "Linus Torvalds").com("partido", "Partido do Kernel")).isOk(), "admin adiciona candidato 99");
            verificar(!admin.enviar(Mensagem.requisicao(Protocolo.OP_ADICIONAR_CANDIDATO)
                    .com("numero", 99).com("nome", "Outro").com("partido", "Z")).isOk(), "número de candidato duplicado é recusado");
            verificar(admin.enviar(Mensagem.requisicao(Protocolo.OP_REMOVER_CANDIDATO).com("numero", 99)).isOk(), "admin remove candidato 99");
            verificar(!admin.enviar(Mensagem.requisicao(Protocolo.OP_REMOVER_CANDIDATO).com("numero", 99)).isOk(), "remover candidato inexistente falha");
            verificar(Conversores.candidatos(eleitor.enviar(Mensagem.requisicao(Protocolo.OP_LISTAR_CANDIDATOS))
                    .getLista("candidatos")).size() == 3, "lista volta a ter 3 candidatos");

            Mensagem envio = admin.enviar(Mensagem.requisicao(Protocolo.OP_ENVIAR_NOTA).com("texto", "Atenção: a votação encerra em breve! ção"));
            if (multicastDisponivel) {
                verificar(envio.isOk(), "admin envia nota (TCP -> servidor)");
                boolean chegou = recebeuNota.await(3, TimeUnit.SECONDS);
                verificar(chegou, "nota chegou ao eleitor via UDP multicast");
                if (chegou) {
                    synchronized (notasRecebidas) {
                        verificar(notasRecebidas.get(0).equals("admin: Atenção: a votação encerra em breve! ção"),
                                "conteúdo da nota preservado (acentos)");
                    }
                }
            }
        }

        // ---------------- votação concorrente ----------------
        // eleitor1..eleitor4 votam ao mesmo tempo (2 no 13, 1 no 22, 1 no 45); eleitor5 vota em branco (não vota)
        int[] escolhas = { 13, 13, 22, 45 };
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger aceitos = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < escolhas.length; i++) {
            final int n = i + 1;
            final int escolha = escolhas[i];
            Thread t = new Thread(() -> {
                try (ConexaoServidor c = new ConexaoServidor("localhost", porta)) {
                    c.enviar(Mensagem.requisicao(Protocolo.OP_LOGIN).com("login", "eleitor" + n).com("senha", "123"));
                    largada.await();
                    Mensagem r = c.enviar(Mensagem.requisicao(Protocolo.OP_VOTAR).com("candidato", escolha));
                    if (r.isOk()) {
                        aceitos.incrementAndGet();
                    }
                    // tenta votar de novo: deve ser recusado
                    Mensagem dup = c.enviar(Mensagem.requisicao(Protocolo.OP_VOTAR).com("candidato", escolha));
                    if (dup.isOk()) {
                        aceitos.addAndGet(1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            threads.add(t);
            t.start();
        }
        largada.countDown();
        for (Thread t : threads) {
            t.join(5000);
        }
        verificar(aceitos.get() == 4, "4 eleitores votaram concorrentemente; nenhum voto duplicado foi aceito");

        try (ConexaoServidor c = new ConexaoServidor("localhost", porta)) {
            c.enviar(Mensagem.requisicao(Protocolo.OP_LOGIN).com("login", "eleitor5").com("senha", "123"));
            Mensagem invalido = c.enviar(Mensagem.requisicao(Protocolo.OP_VOTAR).com("candidato", 12345));
            verificar(!invalido.isOk() && invalido.getString("erro").contains("inexistente"), "voto em candidato inexistente é recusado");
            Mensagem antes = c.enviar(Mensagem.requisicao(Protocolo.OP_RESULTADO));
            verificar(!antes.isOk() && antes.getString("erro").contains("ainda está aberta"), "resultado é recusado antes do prazo");
            Mensagem st = c.enviar(Mensagem.requisicao(Protocolo.OP_STATUS));
            verificar(st.getBoolean("votacaoAberta", false) && st.getLong("tempoRestanteMs", 0) > 0, "status: votação aberta");
        }

        // ---------------- prazo ----------------
        long espera = DURACAO_SEGUNDOS * 1000L - (System.currentTimeMillis() - inicio) + 400;
        System.out.println("... aguardando o fim do prazo (" + Math.max(espera, 0) / 1000.0 + " s) ...");
        Thread.sleep(Math.max(espera, 0));

        try (ConexaoServidor c = new ConexaoServidor("localhost", porta)) {
            c.enviar(Mensagem.requisicao(Protocolo.OP_LOGIN).com("login", "eleitor5").com("senha", "123"));
            Mensagem tarde = c.enviar(Mensagem.requisicao(Protocolo.OP_VOTAR).com("candidato", 22));
            verificar(!tarde.isOk() && tarde.getString("erro").contains("encerrada"), "voto após o prazo é recusado");

            Mensagem r = c.enviar(Mensagem.requisicao(Protocolo.OP_RESULTADO));
            verificar(r.isOk(), "resultado liberado após o prazo");
            Resultado res = Conversores.resultado(r.getMapa("resultado"));
            verificar(res.getTotalVotos() == 4, "total de votos = 4 (o voto tardio não conta)");
            verificar(res.getVencedores().size() == 1 && res.getVencedores().get(0).getNumero() == 13, "vencedor = candidato 13");
            verificar(Math.abs(res.getItens().get(0).getPercentual() - 50.0) < 0.001, "candidato 13 tem 50%");
            double soma = 0;
            for (var item : res.getItens()) {
                soma += item.getPercentual();
            }
            verificar(Math.abs(soma - 100.0) < 0.001, "percentuais somam 100%");
            System.out.println();
            System.out.println(modelo.Formatadores.resultado(res));
        }

        receptor.encerrar();
        servidor.parar();
        System.out.println(falhas == 0 ? "\nTodos os testes passaram." : "\n" + falhas + " teste(s) FALHARAM.");
        System.exit(falhas == 0 ? 0 : 1);
    }

    private static void verificar(boolean condicao, String descricao) {
        System.out.println((condicao ? "[OK]   " : "[FALHA] ") + descricao);
        if (!condicao) {
            falhas++;
        }
    }
}
