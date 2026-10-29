package rmi;

import java.rmi.RemoteException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelo.Candidato;
import modelo.Resultado;
import modelo.ServicoException;
import protocolo.Conversores;

/**
 * Lado cliente do protocolo requisição-resposta: faz o papel do "client stub"
 * + módulo de comunicação da Figura 5.4. {@link #doOperation} é o método
 * citado no enunciado (assinatura adaptada: methodId continua {@code int},
 * mas a referência remota é o nosso {@link RemoteObjectRef} e o resultado é
 * devolvido diretamente, já que a Java RMI é síncrona). Os demais métodos são
 * conveniências tipadas construídas sobre {@code doOperation}, para que quem
 * usa {@code Requisitor} não precise montar JSON na mão.
 */
public class Requisitor {
    private String token;
    private String nomeAutenticado;
    private String perfilAutenticado;

    public String getNomeAutenticado() {
        return nomeAutenticado;
    }

    public String getPerfilAutenticado() {
        return perfilAutenticado;
    }

    /**
     * Empacota a chamada e invoca o objeto remoto, devolvendo a resposta já
     * desempacotada (ver {@link Empacotador}). Lança {@link ServicoException}
     * quando o servidor responde com erro de regra de negócio.
     */
    public byte[] doOperation(RemoteObjectRef o, int methodId, byte[] arguments) throws RemoteException {
        return o.getStub().invocarRemota(methodId, arguments);
    }

    private Map<String, Object> chamar(RemoteObjectRef o, int methodId, Map<String, Object> args) throws ServicoException, RemoteException {
        byte[] resposta = doOperation(o, methodId, Empacotador.empacotar(args));
        Map<String, Object> m = Empacotador.desempacotar(resposta);
        if (!Boolean.TRUE.equals(m.get("ok"))) {
            throw new ServicoException(String.valueOf(m.get("erro")));
        }
        return m;
    }

    private Map<String, Object> comToken(Map<String, Object> args) {
        args.put("token", token);
        return args;
    }

    // ------------------------------------------------------------ operações

    public void autenticar(RemoteObjectRef o, String login, String senha) throws ServicoException, RemoteException {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("login", login);
        args.put("senha", senha);
        Map<String, Object> resp = chamar(o, OperacoesRemotas.AUTENTICAR, args);
        this.token = (String) resp.get("token");
        @SuppressWarnings("unchecked")
        Map<String, Object> usuario = (Map<String, Object>) resp.get("usuario");
        this.nomeAutenticado = (String) usuario.get("nome");
        this.perfilAutenticado = (String) usuario.get("perfil");
    }

    @SuppressWarnings("unchecked")
    public List<Candidato> listarCandidatos(RemoteObjectRef o) throws ServicoException, RemoteException {
        Map<String, Object> resp = chamar(o, OperacoesRemotas.LISTAR_CANDIDATOS, comToken(new LinkedHashMap<>()));
        List<Object> lista = (List<Object>) resp.get("candidatos");
        return lista.stream().map(Conversores::paraCandidato).collect(java.util.stream.Collectors.toList());
    }

    public String votar(RemoteObjectRef o, int numeroCandidato) throws ServicoException, RemoteException {
        Map<String, Object> args = comToken(new LinkedHashMap<>());
        args.put("numero", numeroCandidato);
        return (String) chamar(o, OperacoesRemotas.VOTAR, args).get("comprovante");
    }

    public boolean votacaoAberta(RemoteObjectRef o) throws ServicoException, RemoteException {
        return Boolean.TRUE.equals(chamar(o, OperacoesRemotas.VOTACAO_ABERTA, comToken(new LinkedHashMap<>())).get("aberta"));
    }

    public long tempoRestanteMs(RemoteObjectRef o) throws ServicoException, RemoteException {
        return ((Number) chamar(o, OperacoesRemotas.TEMPO_RESTANTE_MS, comToken(new LinkedHashMap<>())).get("tempoRestanteMs")).longValue();
    }

    public Resultado apurar(RemoteObjectRef o) throws ServicoException, RemoteException {
        Map<String, Object> resp = chamar(o, OperacoesRemotas.APURAR, comToken(new LinkedHashMap<>()));
        return paraResultado((Map<String, Object>) resp.get("resultado"));
    }

    public Candidato adicionarCandidato(RemoteObjectRef o, int numero, String nome, String partido) throws ServicoException, RemoteException {
        Map<String, Object> args = comToken(new LinkedHashMap<>());
        args.put("numero", numero);
        args.put("nome", nome);
        args.put("partido", partido);
        Map<String, Object> resp = chamar(o, OperacoesRemotas.ADICIONAR_CANDIDATO, args);
        return Conversores.paraCandidato(resp.get("candidato"));
    }

    public Candidato removerCandidato(RemoteObjectRef o, int numero) throws ServicoException, RemoteException {
        Map<String, Object> args = comToken(new LinkedHashMap<>());
        args.put("numero", numero);
        Map<String, Object> resp = chamar(o, OperacoesRemotas.REMOVER_CANDIDATO, args);
        return Conversores.paraCandidato(resp.get("candidato"));
    }

    public void sair(RemoteObjectRef o) throws RemoteException {
        try {
            chamar(o, OperacoesRemotas.SAIR, comToken(new LinkedHashMap<>()));
        } catch (ServicoException ignorado) {
            // encerrar sessão nunca deve travar o cliente
        }
    }

    @SuppressWarnings("unchecked")
    private static Resultado paraResultado(Map<String, Object> m) {
        List<Object> itensJson = (List<Object>) m.get("itens");
        List<modelo.ItemResultado> itens = new java.util.ArrayList<>();
        for (Object o : itensJson) {
            Map<String, Object> item = (Map<String, Object>) o;
            Candidato c = Conversores.paraCandidato(item.get("candidato"));
            int votos = ((Number) item.get("votos")).intValue();
            double percentual = ((Number) item.get("percentual")).doubleValue();
            itens.add(new modelo.ItemResultado(c, votos, percentual));
        }
        List<Object> vencedoresJson = (List<Object>) m.get("vencedores");
        List<Candidato> vencedores = new java.util.ArrayList<>();
        for (Object o : vencedoresJson) {
            vencedores.add(Conversores.paraCandidato(o));
        }
        int total = ((Number) m.get("totalVotos")).intValue();
        return new Resultado(total, itens, vencedores);
    }
}
