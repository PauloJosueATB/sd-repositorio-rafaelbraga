package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface remota do objeto de serviço, no papel do "remote object" da
 * Figura 5.4 do livro-texto (Coulouris). Ela expõe UM único método físico,
 * {@link #invocarRemota}, que recebe o identificador do método lógico
 * (methodId) e os argumentos já empacotados — o mesmo papel que o
 * par (dispatcher + skeleton) desempenha na figura: recebe a requisição que
 * o módulo de comunicação da Java RMI já entregou, decide qual procedimento
 * de serviço chamar e devolve a resposta, que a própria RMI se encarrega de
 * transportar de volta ao cliente. Não há nenhum socket criado por nós: toda
 * a entrega da requisição e do retorno é feita pelo runtime da Java RMI.
 */
public interface ComunicadorRemoto extends Remote {

    /**
     * @param methodId  identificador do método lógico a invocar (ver {@link OperacoesRemotas})
     * @param argumentos argumentos já empacotados (representação externa JSON, ver {@link Empacotador})
     * @return resposta empacotada (representação externa JSON)
     */
    byte[] invocarRemota(int methodId, byte[] argumentos) throws RemoteException;
}
