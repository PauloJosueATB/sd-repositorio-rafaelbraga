package modelo;

/** POJO: usuário do sistema (eleitor ou administrador). */
public class Usuario {
    private String login;
    private String senha;
    private String nome;
    private Perfil perfil;

    public Usuario() {
    }

    public Usuario(String login, String senha, String nome, Perfil perfil) {
        this.login = login;
        this.senha = senha;
        this.nome = nome;
        this.perfil = perfil;
    }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }

    /** A senha nunca é impressa. */
    @Override
    public String toString() {
        return nome + " <" + login + ", " + perfil + ">";
    }
}
