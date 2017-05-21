import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.*;
import java.util.Scanner;
import java.io.*;
import java.util.*; 

/**
 * Esta clase actua como servidor de autenticacion. Creara y ofrecera un 
 * objeto de la clase asociada al servidor de archivos
 * @author: Carlos Da Silva (10-10175)
 * @author: Patricia Wilthew (09-10910)
 */

public class a_rmifs{

	/**
	* Metodo que obtiene los argumentos con los cuales fue ejecutado el servidor
	* de autenticacion
	* @param args Arreglo de argumentos obtenidos por consola
	* @return 
	*/
	public static String[] findArgs(String[] args){
		String users, port;
		users = "";
		port = "";

		String[] newArgs = new String[2];
		int tam = args.length;

		if (tam < 4){
			System.out.println("Error: Faltan parametros obligatorios");
			System.exit(0);
		}
		else{
			int j = 0;
			while (j < tam-1){
				if (args[j].equals("-f")){
					users = args[j+1];
				}
				if (args[j].equals("-p")){
					port = args[j+1];
				}
				j = j+2;
			}
		}
		
		newArgs[0] = users;
		newArgs[1] = port;
		return newArgs;
	}

	/**
	* Metodo que ofrece un servidor de autenticacion en el puerto 'port'
	* @param args Arreglo de argumentos obtenidos por consola
	*/
	public a_rmifs(String[] args){
		try{
			//  Usar la linea de abajo para no hacer la llamada a rmiregistry
			//     desde la linea de comandos, de esta forma el mismo programa 
			//     servidor crea su propio servicio de nombres antes de publicar 
			//     el objeto remoto c. 

			String[] arguments = new String[4];
			arguments = findArgs(args);
			String users = arguments[0];
			String port = arguments[1];

			LocateRegistry.createRegistry(Integer.parseInt(port));
			Authentication o = new AuthenticationImpl();

			//Parametro para el rebind
			String par_rebind = "rmi://127.0.0.1:" + port + "/AutServerService";
			Naming.rebind(par_rebind, o);

			try{
				// Abrimos el archivo
				FileInputStream fstream = new FileInputStream(users);
				// Creamos el objeto de entrada
				DataInputStream input = new DataInputStream(fstream);
				// Creamos el Buffer de Lectura
				BufferedReader buffer = new BufferedReader(new InputStreamReader(input));
				String strLine;
				
				while ((strLine = buffer.readLine()) != null){
					o.add(strLine);
				}

				input.close();
			} catch (Exception e){ 
			System.err.println("Ocurrio un error: " + e.getMessage());
			}
		  
		} catch (Exception e) {
		  System.out.println("Problema: " + e);
		}
	}

	/**
	* Metodo principal que ejecuta la clase servidor de autenticacion
	* @param args Arreglo de argumentos obtenidos por consola
	*/
	public static void main(String args[]){
		new a_rmifs(args);
	}
}

