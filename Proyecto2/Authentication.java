/**
 * Esta clase actua como interfaz de la clase de la cual el
 * servidor de autenticacion ofrecera un objeto
 * @author: Carlos Da Silva (10-10175)
 * @author: Patricia Wilthew (09-10910)
 */

public interface Authentication 
          extends java.rmi.Remote {

    public int authenticate(String nombre_clave)
	throws java.rmi.RemoteException;

    public void add(String nombre_clave)
	throws java.rmi.RemoteException;
}

