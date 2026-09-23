package modelo;

import java.util.Locale;

/** Utilitários de apresentação (texto) usados por servidores e clientes. */
public final class Formatadores {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Formatadores() {
    }

    /** 125000 ms -> "02:05". */
    public static String duracao(long ms) {
        long s = Math.max(0, ms) / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    public static String resultado(Resultado r) {
        StringBuilder sb = new StringBuilder();
        sb.append("========== RESULTADO DA VOTAÇÃO ==========\n");
        sb.append("Total de votos: ").append(r.getTotalVotos()).append('\n');
        int pos = 1;
        for (ItemResultado i : r.getItens()) {
            sb.append(String.format(PT_BR, "%2dº %-45s %4d voto(s)  %6.2f%%%n",
                    pos++, i.getCandidato(), i.getVotos(), i.getPercentual()));
        }
        if (r.getVencedores().isEmpty()) {
            sb.append("Nenhum voto registrado: não há candidato vencedor.\n");
        } else if (r.isEmpate()) {
            StringBuilder nomes = new StringBuilder();
            for (Candidato c : r.getVencedores()) {
                nomes.append(nomes.length() == 0 ? "" : " e ").append(c);
            }
            sb.append("EMPATE entre: ").append(nomes).append('\n');
        } else {
            sb.append("VENCEDOR: ").append(r.getVencedores().get(0)).append('\n');
        }
        sb.append("==========================================");
        return sb.toString();
    }
}
