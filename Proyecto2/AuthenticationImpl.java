import java.io.*;
import java.util.*; 

/**
 * Esta clase actua como la implementacion de la interfaz de la
 * clase asociada al servidor de autenticacion 
 * @author: Carlos Da Silva (10-10175)
 * @author: Patricia Wilthew (09-10910)
 */

public class AuthenticationImpl
    extends 
		java.rmi.server.UnicastRemoteObject
    implements Authentication{

		/** Atributos de la clase **/
		public static final long serialVersionUID = 1L;
		public List<String> user_key = new ArrayList<String>(); //Lista de usuarios, con sus claves

		/**
		* Constructor de la clase
		*/
		public AuthenticationImpl()
			throws java.rmi.RemoteException{
				super();
		}

		/**
		* Metodo que permite agregar un usuario y su clave como par ordenado
		* a la lista de user_key de la clase
		* @param name_key Par nombre:clave a agregar
		*/
		public void add(String name_key)
			throws java.rmi.RemoteException{
				this.user_key.add(name_key);
		}

		/**
		* Metodo que verifica que un par ordenado "nombre,clave" estan en 
		* la lista user_key de la clase
		* @param name_key Par nombre:clave a autenticar
		* @return Entero, 1 si se encuenta el par, -1 en caso contrario
		*/
		public int authenticate(String name_key)
			throws java.rmi.RemoteException{
				int k = this.user_key.indexOf(name_key);
				if (k == -1){
				  return 0;
				}
				return 1;
			}
	}

