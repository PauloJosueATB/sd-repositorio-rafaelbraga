package votacao.cliente;

import java.io.IOException;

import modelo.Perfil;
import votacao.protocolo.Mensagem;
import votacao.protocolo.Protocolo;

/**
 * QUESTÃO 5 - Cliente do ADMINISTRADOR: introduz e remove candidatos e envia notas informativas
 * (o servidor as retransmite por UDP multicast a todos os eleitores).
 *
 * <pre>java votacao.cliente.ClienteAdministrador [host=localhost] [porta=5000]</pre>
 */
public class ClienteAdministrador extends ClienteBase {

    public ClienteAdministrador(String host, int porta) throws IOException {
        super(host, porta);
    }

    @Override
    public void executar() throws IOException {
        if (!autenticar(Perfil.ADMINISTRADOR)) {
            return;
        }
        while (true) {
            System.out.println("\n--- Menu do administrador ---");
            System.out.println("1) Listar candidatos  2) Adicionar candidato  3) Remover candidato");
            System.out.println("4) Enviar nota aos eleitores  5) Ver resultado  6) Tempo restante  0) Sair");
            String opcao = perguntar("Opção: ");
            if (opcao == null || opcao.equals("0")) {
                sair();
                System.out.println("Até logo!");
                return;
            }
            switch (opcao) {
                case "1": listarCandidatos(); break;
                case "2": adicionar(); break;
                case "3": remover(); break;
                case "4": enviarNota(); break;
                case "5": mostrarResultado(); break;
                case "6": mostrarStatus(); break;
                default: System.out.println("Opção inválida.");
            }
        }
    }

    private void adicionar() throws IOException {
        Integer numero = perguntarInt("Número: ");
        String nome = numero == null ? null : perguntar("Nome: ");
        String partido = nome == null ? null : perguntar("Partido: ");
        if (partido == null) {
            return;
        }
        Mensagem r = conexao.enviar(Mensagem.requisicao(Protocolo.OP_ADICIONAR_CANDIDATO)
                .com("numero", numero).com("nome", nome).com("partido", partido));
        System.out.println(r.isOk() ? "Candidato adicionado." : "Erro: " + r.getString("erro"));
    }

    private void remover() throws IOException {
        Integer numero = perguntarInt("Número do candidato a remover: ");
        if (numero == null) {
            return;
        }
        Mensagem r = conexao.enviar(Mensagem.requisicao(Protocolo.OP_REMOVER_CANDIDATO).com("numero", numero));
        System.out.println(r.isOk() ? "Candidato removido." : "Erro: " + r.getString("erro"));
    }

    private void enviarNota() throws IOException {
        String texto = perguntar("Texto da nota: ");
        if (texto == null) {
            return;
        }
        Mensagem r = conexao.enviar(Mensagem.requisicao(Protocolo.OP_ENVIAR_NOTA).com("texto", texto));
        System.out.println(r.isOk() ? "Nota enviada ao grupo multicast." : "Erro: " + r.getString("erro"));
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : Protocolo.PORTA_TCP_PADRAO;
        try (ClienteAdministrador cliente = new ClienteAdministrador(host, porta)) {
            cliente.executar();
        }
    }
}
