import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.*;
import java.util.Scanner;
import java.io.*;

/**
 * Esta clase actua como servidor de archivos centralizados. En el, se
 * instancia un objeto de la clase mencionada previamente y se registra
 * en el RMIregistry
 * @author: Carlos Da Silva (10-10175)
 * @author: Patricia Wilthew (09-10910)
 */

public class s_rmifs {

	/**
	* Metodo que obtiene los argumentos con los cuales fue ejecutado el servidor
	* @param args Arreglo de los argumentos obtenidos por consola
	* @return Arreglo con las palabras correspondientes a los argumentos
	*/
	public static String[] findArgs(String[] args){
		String localPort, port, host;
		localPort = "";
		port = "";
		host = "";

		String[] newArgs = new String[3];
		int tam = args.length;

		if (tam < 6){
			System.out.println("Error: Faltan parametros obligatorios");
			System.exit(0);
		}
		else {
			int j = 0;
			while (j < tam-1){
				if (args[j].equals("-l")){
					localPort = args[j+1];
				}
				if (args[j].equals("-r")){
					port = args[j+1];
				}
				if (args[j].equals("-h")){
					host = args[j+1];
				}
				j = j+2;
			}
		}
		
		newArgs[0] = localPort;
		newArgs[1] = port;
		newArgs[2] = host;
		return newArgs;
	}

	/**
	* Metodo que ofrece un servidor centralizado de archivos 
	* @param args Arreglo de argumentos obtenidos por consola
	*/
	public s_rmifs(String[] args) {
		try {
			String[] arguments = new String[3];
			arguments = findArgs(args);
			String localPort = arguments[0];
			String port = arguments[1];
			String host = arguments[2];
			
			if ((localPort.equals("")) || (port.equals("")) || (host.equals(""))){
				System.out.println("Error: Faltan parametros obligatorios");
				System.exit(0);
			}

			LocateRegistry.createRegistry(Integer.parseInt(localPort));

			String par_lookup = "rmi://" + host + ":" + port + "/AutServerService";

			//Creamos el objeto del servidor de autenticacion.
			//Se lo pasamos como parametro al de archivos, para que este
			//pueda utilizarlo siempre en sus metodos.

			Authentication o = (Authentication)Naming.lookup(par_lookup); 

			//Aca este objeto debe recibir como parametro otro objeto que vendria
			//siendo el servidor de autenticacion
			FileManager c = new FileManagerImpl(o);

			//Recordar que aca va el IP o el DNS
			String parRebind = "rmi://127.0.0.1:" + localPort + "/FileManagerService"; 
			Naming.rebind(parRebind, c);
			int opcion;
			
			while (true) {
				String impresion;
				impresion= "Menu del servidor\n Seleccione la opcion correspondiente a lo que desea realizar: \n";
				impresion+="log: Mostrar lista de los ultimos 20 comandos que los clientes han enviado al servidor \n";
				impresion+="sal: Terminar ejecucion del manejador de archivos.\n";
				System.out.println(impresion);
				String request;
				Scanner keyboardRequest = new Scanner(System.in);
				request = keyboardRequest.nextLine();

				if (request.equals("log")){
					System.out.println("Ultimos 20 logs: \n");
					String temp = c.getLogs();
					String [] campos = temp.split("~");
					
					if ((campos.length <= 20) && (campos.length > 1)){
						for ( int j = 0 ; j < campos.length ; j++ ){
							String[] strArray = campos[j].split(",");
							System.out.println(strArray[0] + " by " + strArray[1] + "--");
						}
					}
					else if (campos.length > 20){
						int tam = campos.length;
						int resta = tam-20;
						
						for (int w = resta; w < campos.length;w++){
							System.out.println(campos[w]);
						}
					}
				}
				else if (request.equals("sal")){
					System.exit(0);
				}
				else{
					System.out.println("Comando erroneo. Intente de nuevo.");
				}
			}
		} catch (Exception e) {
		System.out.println("Problema: " + e);
		}
	}

	/**
	* Metodo que ejecuta la clase servidor del manejador de archivos
	* @param args Arreglo de argumentos obtenidos por consola.
	*/
	public static void main(String args[]) {
		new s_rmifs(args);
	}
}

