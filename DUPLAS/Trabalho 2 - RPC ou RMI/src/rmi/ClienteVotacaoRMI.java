package rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

import modelo.Candidato;
import modelo.Resultado;
import modelo.ServicoException;

/**
 * Processo cliente em modo texto. Uso: {@code ClienteVotacaoRMI [host] [porta] [nomeServico]}.
 * Localiza o objeto remoto com {@code Naming}/{@code Registry.lookup} — a referência
 * devolvida (o stub) é usada por todas as chamadas seguintes, o que concretiza a
 * "passagem por referência para objetos remotos" exigida no enunciado: nenhuma cópia
 * do objeto de serviço é feita, o cliente sempre fala com a mesma instância do servidor.
 */
public final class ClienteVotacaoRMI {
    private ClienteVotacaoRMI() {
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : 1099;
        String nomeServico = args.length > 2 ? args[2] : "ServicoVotacao";

        Registry registro = LocateRegistry.getRegistry(host, porta);
        ComunicadorRemoto stub = (ComunicadorRemoto) registro.lookup(nomeServico);
        RemoteObjectRef referencia = new RemoteObjectRef(nomeServico, stub);
        Requisitor requisitor = new Requisitor();

        System.out.println("Conectado ao serviço \"" + nomeServico + "\" em " + host + ":" + porta + ".");
        try (Scanner teclado = new Scanner(System.in, "UTF-8")) {
            if (!autenticar(referencia, requisitor, teclado)) {
                return;
            }
            System.out.println("Bem-vindo(a), " + requisitor.getNomeAutenticado()
                    + " (" + requisitor.getPerfilAutenticado() + ").");

            boolean administrador = "ADMINISTRADOR".equals(requisitor.getPerfilAutenticado());
            boolean sair = false;
            while (!sair) {
                imprimirMenu(administrador);
                String opcao = teclado.nextLine().trim();
                try {
                    switch (opcao) {
                        case "1":
                            listarCandidatos(referencia, requisitor);
                            break;
                        case "2":
                            if (administrador) {
                                adicionarCandidato(referencia, requisitor, teclado);
                            } else {
                                votar(referencia, requisitor, teclado);
                            }
                            break;
                        case "3":
                            if (administrador) {
                                removerCandidato(referencia, requisitor, teclado);
                            } else {
                                verResultado(referencia, requisitor);
                            }
                            break;
                        case "4":
                            if (administrador) {
                                verResultado(referencia, requisitor);
                            } else {
                                tempoRestante(referencia, requisitor);
                            }
                            break;
                        case "5":
                            tempoRestante(referencia, requisitor);
                            break;
                        case "0":
                            requisitor.sair(referencia);
                            sair = true;
                            break;
                        default:
                            System.out.println("Opção inválida.");
                    }
                } catch (ServicoException e) {
                    System.out.println("[Erro] " + e.getMessage());
                }
            }
        }
        System.out.println("Encerrado.");
    }

    private static boolean autenticar(RemoteObjectRef ref, Requisitor requisitor, Scanner teclado) throws Exception {
        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            System.out.print("Login: ");
            String login = teclado.nextLine().trim();
            System.out.print("Senha: ");
            String senha = teclado.nextLine().trim();
            try {
                requisitor.autenticar(ref, login, senha);
                return true;
            } catch (ServicoException e) {
                System.out.println("[Erro] " + e.getMessage() + " (tentativa " + tentativa + " de 3)");
            }
        }
        System.out.println("Número máximo de tentativas de login excedido.");
        return false;
    }

    private static void imprimirMenu(boolean administrador) {
        System.out.println();
        System.out.println("==== Menu " + (administrador ? "(administrador)" : "(eleitor)") + " ====");
        System.out.println("1 - Listar candidatos");
        if (administrador) {
            System.out.println("2 - Adicionar candidato");
            System.out.println("3 - Remover candidato");
            System.out.println("4 - Ver resultado (após o prazo)");
            System.out.println("5 - Tempo restante");
        } else {
            System.out.println("2 - Votar");
            System.out.println("3 - Ver resultado (após o prazo)");
            System.out.println("4 - Tempo restante");
        }
        System.out.println("0 - Sair");
        System.out.print("Opção: ");
    }

    private static void listarCandidatos(RemoteObjectRef ref, Requisitor requisitor) throws Exception {
        List<Candidato> candidatos = requisitor.listarCandidatos(ref);
        System.out.println("Candidatos:");
        for (Candidato c : candidatos) {
            System.out.println("  " + c.getNumero() + " - " + c.getNome() + " (" + c.getPartido() + ")");
        }
    }

    private static void votar(RemoteObjectRef ref, Requisitor requisitor, Scanner teclado) throws Exception {
        System.out.print("Número do candidato: ");
        int numero = Integer.parseInt(teclado.nextLine().trim());
        String comprovante = requisitor.votar(ref, numero);
        System.out.println("Voto registrado! Comprovante: " + comprovante);
    }

    private static void adicionarCandidato(RemoteObjectRef ref, Requisitor requisitor, Scanner teclado) throws Exception {
        System.out.print("Número: ");
        int numero = Integer.parseInt(teclado.nextLine().trim());
        System.out.print("Nome: ");
        String nome = teclado.nextLine().trim();
        System.out.print("Partido: ");
        String partido = teclado.nextLine().trim();
        Candidato c = requisitor.adicionarCandidato(ref, numero, nome, partido);
        System.out.println("Candidato adicionado: " + c.getNumero() + " - " + c.getNome());
    }

    private static void removerCandidato(RemoteObjectRef ref, Requisitor requisitor, Scanner teclado) throws Exception {
        System.out.print("Número do candidato a remover: ");
        int numero = Integer.parseInt(teclado.nextLine().trim());
        Candidato c = requisitor.removerCandidato(ref, numero);
        System.out.println("Candidato removido: " + c.getNumero() + " - " + c.getNome());
    }

    private static void verResultado(RemoteObjectRef ref, Requisitor requisitor) throws Exception {
        Resultado r = requisitor.apurar(ref);
        System.out.println("Total de votos: " + r.getTotalVotos());
        r.getItens().forEach(i -> System.out.printf("  %d - %s: %d votos (%.1f%%)%n",
                i.getCandidato().getNumero(), i.getCandidato().getNome(), i.getVotos(), i.getPercentual()));
        System.out.println(r.isEmpate() ? "Resultado: empate." : "Vencedor: " + r.getVencedores().get(0).getNome());
    }

    private static void tempoRestante(RemoteObjectRef ref, Requisitor requisitor) throws Exception {
        long ms = requisitor.tempoRestanteMs(ref);
        System.out.println(requisitor.votacaoAberta(ref)
                ? "Tempo restante: " + (ms / 1000) + " s."
                : "A votação já foi encerrada.");
    }
}
