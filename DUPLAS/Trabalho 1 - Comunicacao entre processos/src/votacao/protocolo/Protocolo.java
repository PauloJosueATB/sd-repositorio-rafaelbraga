package votacao.protocolo;

/**
 * Constantes do protocolo de aplicação do sistema de votação.
 *
 * <h3>TCP (unicast) - uma mensagem JSON por linha, codificação UTF-8</h3>
 * <pre>
 * REQUISIÇÃO                                         RESPOSTA (sempre com "ok": true|false e, se false, "erro")
 * {"op":"LOGIN","login":"..","senha":".."}           {"ok":true,"nome":..,"perfil":..,"titulo":..,"candidatos":[..],
 *                                                     "votacaoAberta":..,"tempoRestanteMs":..,"grupoMulticast":..,"portaMulticast":..}
 * {"op":"LISTAR_CANDIDATOS"}                         {"ok":true,"candidatos":[{"numero":..,"nome":..,"partido":..}]}
 * {"op":"VOTAR","candidato":13}                      {"ok":true,"comprovante":".."}
 * {"op":"STATUS"}                                    {"ok":true,"titulo":..,"votacaoAberta":..,"tempoRestanteMs":..}
 * {"op":"RESULTADO"}                                 {"ok":true,"resultado":{"totalVotos":..,"itens":[..],"vencedores":[..]}}
 * {"op":"ADICIONAR_CANDIDATO","numero":..,"nome":..,"partido":..}   (administrador)   {"ok":true,"candidato":{..}}
 * {"op":"REMOVER_CANDIDATO","numero":..}                            (administrador)   {"ok":true}
 * {"op":"ENVIAR_NOTA","texto":".."}                                 (administrador)   {"ok":true}
 * {"op":"SAIR"}                                      {"ok":true}
 * </pre>
 *
 * <h3>UDP (multicast) - somente notas informativas, um datagrama JSON</h3>
 * <pre>{"tipo":"NOTA","autor":"admin","texto":"..","instante":1700000000000}</pre>
 */
public final class Protocolo {
    public static final String OP_LOGIN = "LOGIN";
    public static final String OP_LISTAR_CANDIDATOS = "LISTAR_CANDIDATOS";
    public static final String OP_VOTAR = "VOTAR";
    public static final String OP_STATUS = "STATUS";
    public static final String OP_RESULTADO = "RESULTADO";
    public static final String OP_ADICIONAR_CANDIDATO = "ADICIONAR_CANDIDATO";
    public static final String OP_REMOVER_CANDIDATO = "REMOVER_CANDIDATO";
    public static final String OP_ENVIAR_NOTA = "ENVIAR_NOTA";
    public static final String OP_SAIR = "SAIR";

    public static final String TIPO_NOTA = "NOTA";

    public static final int PORTA_TCP_PADRAO = 5000;
    public static final String GRUPO_MULTICAST_PADRAO = "230.0.0.1";
    public static final int PORTA_MULTICAST_PADRAO = 4446;

    private Protocolo() {
    }
}
