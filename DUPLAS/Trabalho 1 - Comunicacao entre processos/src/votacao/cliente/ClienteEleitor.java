package votacao.cliente;

import java.io.IOException;

import modelo.Perfil;
import votacao.protocolo.Mensagem;
import votacao.protocolo.Protocolo;

/**
 * QUESTÃO 5 - Cliente do ELEITOR: faz login (TCP), recebe a lista de candidatos, vota (TCP)
 * e recebe as notas informativas dos administradores (UDP multicast).
 *
 * <pre>java votacao.cliente.ClienteEleitor [host=localhost] [porta=5000]</pre>
 */
public class ClienteEleitor extends ClienteBase {

    public ClienteEleitor(String host, int porta) throws IOException {
        super(host, porta);
    }

    @Override
    public void executar() throws IOException {
        if (!autenticar(Perfil.ELEITOR)) {
            return;
        }
        while (true) {
            System.out.println("\n--- Menu do eleitor ---");
            System.out.println("1) Ver candidatos   2) Votar   3) Ver resultado   4) Tempo restante   0) Sair");
            String opcao = perguntar("Opção: ");
            if (opcao == null || opcao.equals("0")) {
                sair();
                System.out.println("Até logo!");
                return;
            }
            switch (opcao) {
                case "1": listarCandidatos(); break;
                case "2": votar(); break;
                case "3": mostrarResultado(); break;
                case "4": mostrarStatus(); break;
                default: System.out.println("Opção inválida.");
            }
        }
    }

    private void votar() throws IOException {
        Integer numero = perguntarInt("Número do candidato: ");
        if (numero == null) {
            return;
        }
        Mensagem r = conexao.enviar(Mensagem.requisicao(Protocolo.OP_VOTAR).com("candidato", numero));
        if (r.isOk()) {
            System.out.println("Voto registrado com sucesso! Comprovante: " + r.getString("comprovante"));
        } else {
            System.out.println("Voto NÃO registrado: " + r.getString("erro"));
        }
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : Protocolo.PORTA_TCP_PADRAO;
        try (ClienteEleitor cliente = new ClienteEleitor(host, porta)) {
            cliente.executar();
        }
    }
}
