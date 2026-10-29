package rmi;

/**
 * Identificadores das operações remotas (o "methodID" da Figura 5.4 do livro-texto).
 * Cada chamada ao serviço remoto informa um destes números em vez do nome do método
 * Java, exatamente como no protocolo requisição-resposta descrito na Seção 5.2.
 */
public final class OperacoesRemotas {
    private OperacoesRemotas() {
    }

    public static final int AUTENTICAR = 1;
    public static final int LISTAR_CANDIDATOS = 2;
    public static final int VOTAR = 3;
    public static final int APURAR = 4;
    public static final int TEMPO_RESTANTE_MS = 5;
    public static final int VOTACAO_ABERTA = 6;
    public static final int ADICIONAR_CANDIDATO = 7;
    public static final int REMOVER_CANDIDATO = 8;
    public static final int SAIR = 9;

    /** Nome legível, só para mensagens de log/erro. */
    public static String nome(int methodId) {
        switch (methodId) {
            case AUTENTICAR: return "AUTENTICAR";
            case LISTAR_CANDIDATOS: return "LISTAR_CANDIDATOS";
            case VOTAR: return "VOTAR";
            case APURAR: return "APURAR";
            case TEMPO_RESTANTE_MS: return "TEMPO_RESTANTE_MS";
            case VOTACAO_ABERTA: return "VOTACAO_ABERTA";
            case ADICIONAR_CANDIDATO: return "ADICIONAR_CANDIDATO";
            case REMOVER_CANDIDATO: return "REMOVER_CANDIDATO";
            case SAIR: return "SAIR";
            default: return "DESCONHECIDA(" + methodId + ")";
        }
    }
}
