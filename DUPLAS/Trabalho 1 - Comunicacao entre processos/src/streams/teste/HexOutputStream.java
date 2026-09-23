package streams.teste;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Decorator que escreve os bytes recebidos como texto hexadecimal (16 por linha).
 * Serve para "enxergar" o conteúdo binário quando o destino é a saída padrão.
 */
public class HexOutputStream extends OutputStream {
    private final PrintStream saida;
    private int coluna;

    public HexOutputStream(PrintStream saida) {
        this.saida = saida;
    }

    @Override
    public void write(int b) {
        saida.printf("%02X ", b & 0xFF);
        if (++coluna == 16) {
            saida.println();
            coluna = 0;
        }
    }

    @Override
    public void flush() {
        if (coluna > 0) {
            saida.println();
            coluna = 0;
        }
        saida.flush();
    }
}
