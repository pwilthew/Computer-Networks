/**
 * Esta clase actua como interfaz de la clase de la cual
 * el servidor ofrecera un objeto remoto
 * @author: Carlos Da Silva (10-10175)
 * @author: Patricia Wilthew (09-10910)
 */

public interface FileManager 
          extends java.rmi.Remote {

    public String info(String username, String key)
        throws java.rmi.RemoteException;

    public String rls(String username, String key)
		throws java.rmi.RemoteException;

    public String deleteFile(String filename,String username, String key)
		throws java.rmi.RemoteException;

    public byte[] downloadFile(String fileName,String username, String key)
		throws java.rmi.RemoteException;

    public String getLogs()
		throws java.rmi.RemoteException;

    public String uploadFile(String fileName, byte[] buffer,String username, String key)
		throws java.rmi.RemoteException;

    public int initialAuth(String name, String key)
		throws java.rmi.RemoteException;
}

