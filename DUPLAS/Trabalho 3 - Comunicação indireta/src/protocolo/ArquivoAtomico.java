package protocolo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class ArquivoAtomico {
    private ArquivoAtomico() {
    }

    public static void gravar(Path destino, String conteudo) throws IOException {
        Path temporario = Files.createTempFile(destino.toAbsolutePath().getParent(), ".gravacao-", ".tmp");
        try {
            try (FileChannel canal = FileChannel.open(temporario, StandardOpenOption.WRITE)) {
                ByteBuffer dados = StandardCharsets.UTF_8.encode(conteudo);
                while (dados.hasRemaining()) {
                    canal.write(dados);
                }
                canal.force(true);
            }
            Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporario);
        }
    }
}
