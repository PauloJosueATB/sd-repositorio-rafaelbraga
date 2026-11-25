package modelo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Monta o {@link RepositorioEleicao} a partir de arquivos CSV (separador ';'):
 * <pre>
 *   candidatos.csv : numero;nome;partido
 *   usuarios.csv   : login;senha;nome;perfil   (perfil = ELEITOR | ADMINISTRADOR)
 * </pre>
 * Se os arquivos não existirem, usa dados padrão embutidos. Linhas vazias e
 * iniciadas por '#' são ignoradas.
 */
public final class CarregadorDados {
    private CarregadorDados() {
    }

    public static RepositorioEleicao carregar(Path pasta, int duracaoSegundos) throws IOException {
        long agora = System.currentTimeMillis();
        Eleicao eleicao = new Eleicao(1, "Eleição de Representante - Sistemas Distribuídos",
                agora, agora + duracaoSegundos * 1000L);
        RepositorioEleicao repo = new RepositorioEleicao(eleicao);

        Path arqUsuarios = pasta.resolve("usuarios.csv");
        if (Files.isRegularFile(arqUsuarios)) {
            lerUsuarios(arqUsuarios, repo);
        } else {
            usuariosPadrao(repo);
        }

        Path arqCandidatos = pasta.resolve("candidatos.csv");
        if (Files.isRegularFile(arqCandidatos)) {
            lerCandidatos(arqCandidatos, repo);
        } else {
            candidatosPadrao(repo);
        }
        return repo;
    }

    /** Dados padrão, sem ler arquivos (útil em testes automatizados). */
    public static RepositorioEleicao padrao(int duracaoSegundos) {
        long agora = System.currentTimeMillis();
        RepositorioEleicao repo = new RepositorioEleicao(new Eleicao(1,
                "Eleição de Representante - Sistemas Distribuídos", agora, agora + duracaoSegundos * 1000L));
        usuariosPadrao(repo);
        candidatosPadrao(repo);
        return repo;
    }

    private static void usuariosPadrao(RepositorioEleicao repo) {
        repo.adicionarUsuario(new Usuario("admin", "admin123", "Administrador do Sistema", Perfil.ADMINISTRADOR));
        for (int i = 1; i <= 5; i++) {
            repo.adicionarUsuario(new Usuario("eleitor" + i, "123", "Eleitor " + i, Perfil.ELEITOR));
        }
    }

    private static void candidatosPadrao(RepositorioEleicao repo) {
        repo.adicionarCandidato(new Candidato(13, "Ada Lovelace", "Partido dos Algoritmos"));
        repo.adicionarCandidato(new Candidato(22, "Alan Turing", "Partido da Computabilidade"));
        repo.adicionarCandidato(new Candidato(45, "Grace Hopper", "Partido dos Compiladores"));
    }

    private static void lerUsuarios(Path arq, RepositorioEleicao repo) throws IOException {
        int n = 0;
        for (String[] c : linhas(arq)) {
            n++;
            if (c.length < 4) {
                throw new IOException(arq.getFileName() + ": linha inválida (esperado login;senha;nome;perfil): "
                        + String.join(";", c));
            }
            try {
                repo.adicionarUsuario(new Usuario(c[0], c[1], c[2], Perfil.valueOf(c[3].toUpperCase())));
            } catch (IllegalArgumentException e) {
                throw new IOException(arq.getFileName() + ": perfil inválido '" + c[3] + "'");
            }
        }
        if (n == 0) {
            throw new IOException(arq.getFileName() + " está vazio.");
        }
    }

    private static void lerCandidatos(Path arq, RepositorioEleicao repo) throws IOException {
        for (String[] c : linhas(arq)) {
            if (c.length < 3) {
                throw new IOException(arq.getFileName() + ": linha inválida (esperado numero;nome;partido): "
                        + String.join(";", c));
            }
            try {
                repo.adicionarCandidato(new Candidato(Integer.parseInt(c[0]), c[1], c[2]));
            } catch (NumberFormatException e) {
                throw new IOException(arq.getFileName() + ": número inválido '" + c[0] + "'");
            }
        }
    }

    private static List<String[]> linhas(Path arq) throws IOException {
        List<String[]> resultado = new java.util.ArrayList<>();
        boolean primeira = true;
        for (String linha : Files.readAllLines(arq, StandardCharsets.UTF_8)) {
            if (primeira && linha.startsWith("\uFEFF")) {
                linha = linha.substring(1); // remove BOM (editores do Windows)
            }
            primeira = false;
            linha = linha.trim();
            if (linha.isEmpty() || linha.startsWith("#")) {
                continue;
            }
            String[] campos = linha.split(";", -1);
            for (int i = 0; i < campos.length; i++) {
                campos[i] = campos[i].trim();
            }
            resultado.add(campos);
        }
        return resultado;
    }
}
