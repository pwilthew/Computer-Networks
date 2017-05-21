import java.rmi.Naming;
import java.rmi.RemoteException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.io.*;
import java.util.Scanner;

/**
 * Esta clase actua como programa cliente del sistema. Hace uso del objeto
 * remoto ofrecido por el servidor de archivos. Ofrece un menu para el usuario,
 * y ejecuta los comandos del archivo pasados como parametros 
 * @author: Carlos Da Silva (10-10175)
 * @author: Patricia Wilthew (09-10910)
 */

public class c_rmifs{

	/**
	* Metodo que obtiene los argumentos con los cuales fue ejecutado el cliente
	* @param args Arreglo de los argumentos obtenidos por consola
	* @return Arreglo con las palabras correspondientes a los argumentos
	*/
	public static String[] findArgs(String[] args){
		String server, port, user, commands;
		server = null;
		port = null;
		user = null;
		commands = null;

		String[] newArgs= new String[4];
		int tam = args.length;

		if (tam < 4){
			System.out.println("Error: Faltan parametros obligatorios");
			System.exit(0);
		}
		else {
		  int j=0;
		  while (j < tam-1){
		if (args[j].equals("-f")){
			user = args[j+1];
		}
		if (args[j].equals("-m")){
			server = args[j+1];
		}
		if (args[j].equals("-p")){
			port = args[j+1];
		}
		if (args[j].equals("-c")){
			commands = args[j+1];
		}
		j = j+2;
		  }
		}

		newArgs[0] = port;
		newArgs[1] = server;
		newArgs[2] = user;
		newArgs[3] = commands;

		return newArgs;
	}

	/**
	* Metodo que imprime en pantalla una lista de los archivos locales de un path
	*/
	public static void lls(){
		String path = "."; 
		String localfiles ="";

		String files;
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles(); 

		for (int i = 0; i < listOfFiles.length; i++){
			if (listOfFiles[i].isFile()){
				files = listOfFiles[i].getName();
				localfiles += files;
				localfiles += "\n";
			}
		}
		System.out.println(localfiles);
	}

	/**
	* Metodo que busca si un archivo existe en un path
	* @param filename Nombre del archivo al cual se le verificara existencia
	* @return Devuelve 1 en caso de que archivo se encuentre en directorio actual
	*/
	public static int existsFile(String filename){
		//Obtenemos lista con todos los archivos en el directorio
		String path = "."; 
		int search = 0;

		String files;
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles(); 

		for (int i = 0; i < listOfFiles.length; i++){
			if (listOfFiles[i].isFile()){
				if(listOfFiles[i].getName().equals(filename)){
					search=1;
				}
			}
		}
		return search;
	}

	/**
	* Metodo que lee el archivo de usuarios/claves. Devuelve un arreglo en cuyas
	* posiciones estan: el nombre, la clave y el numero de pares del archivo.
	* @param user Nombre del archivo de texto a analizar
	*/
	public static String[] leerArchivoUser(String user){
		try {
			String[] retorno=new String[3];
			// Abrimos el archivo
			FileInputStream fstream = new FileInputStream(user);
			// Creamos el objeto de entrada
			DataInputStream input = new DataInputStream(fstream);
			// Creamos el Buffer de Lectura
			BufferedReader buffer = new BufferedReader(new InputStreamReader(input));
			String strLine;

			strLine = buffer.readLine();
			String[] nombre_clave=strLine.split(":");
			retorno[0] = nombre_clave[0];
			retorno[1] = nombre_clave[1];
			retorno[2] = "1";

			if (buffer.readLine() != null){
				retorno[2]="2";
			}
			return retorno;
		}
		catch (Exception e){ 
			System.err.println("Ocurrio un error: " + e.getMessage());
			return (null);
		}
	}

	/**
	* Metodo que ejecuta un comando (request) que obtiene como parametro de entrada
	* @param name Nombre de usuario
	* @param key Clave de dicho usuario
	* @param c Objeto de la clase ofrecida por el servidor de archivos
	* @param request String que contiene la solicitud del cliente
	*/
	public static void executeRequest(FileManager c, String request, String username, String key){
	  try{
		  String prefix = null;
		  String fileName = null;
		  String[] requestArray = request.split(" ");

		  // Si el request tiene dos Strings, es de la forma "comando archivo"
		  if (requestArray.length == 2){
			  prefix = requestArray[0];
			  fileName = requestArray[1];

			  if (prefix.equals("bor")){
			    System.out.println( c.deleteFile(fileName, username, key));
			  }
			  else if (prefix.equals("sub")){
				  try {
					  if (existsFile(fileName) == 1){
					    File file = new File(fileName);
					    byte buffer[] = new byte[(int)file.length()];
					    BufferedInputStream input = new
					    BufferedInputStream(new FileInputStream(file.getName()));
					    input.read(buffer,0,buffer.length);
					    input.close();
					    System.out.println(c.uploadFile(fileName, buffer, username, key));
					  }
					  else {
					    System.out.println("Ha intentado subir un archivo que no existe");
					  }
				  } 
				  catch(Exception e){
					  System.err.println("FileServer exception: "+ e.getMessage());
					  e.printStackTrace();
				  }
			  }	    
			  else if (prefix.equals("baj")){
				  try {
					  byte[] fileData = c.downloadFile(fileName, username, key);
				  
					  if (fileData == null){
					    System.out.println(">>Error: Se ha intentado bajar archivo no encontrado");
					  }
					  else{
					    File file = new File(fileName);
					    BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file.getName()));
					    output.write(fileData, 0, fileData.length);
					    output.flush();
					    output.close();
					    System.out.println(">>Se bajado su archivo");
					  }
				  }
				  catch(Exception e){
				    System.err.println("FileServer exception: "+ e.getMessage());
				    e.printStackTrace();
				  }
			  } else {
			      System.out.println(">>Ha introducido un comando erroneo.");
			  }
		  } // Si el request tiene un String, es de la forma "comando"
		  else if (requestArray.length == 1){
		    
			  if (request.equals("rls")){
				  System.out.println(c.rls(username, key));
			  }
			  else if (request.equals("lls")){
				  lls();
			  }
			  else if (request.equals("info")){
				  System.out.println(c.info(username, key));
			  }
			  else if (request.equals("sal")){
				  System.exit(0);
			  } else {
				  System.out.println(">>Ha introducido un comando erroneo.");
			  }
		  } // Si el request tiene cero o mas de dos Strings, es erroneo
		  else {
			  System.out.println(">>Ha introducido un comando erroneo.");
		  }
	  } catch (RemoteException re){
		  System.out.println();
		  System.out.println("RemoteException");
		  System.out.println(re);
	  }
	}

	/**
	* Metodo ejecuta la clase cliente
	* @param args Arreglo de argumentos obtenidos por consola
	*/
	public static void main(String[] args){
	try {
		String[] arguments = new String[4];
		arguments = findArgs(args);
		String port = arguments[0];
		String server = arguments[1];
		String user = arguments[2];
		String commands = arguments[3];
	  
		if (port == null || server == null){
			System.out.println(port);
			System.out.println("Error: Faltan parametros obligatorios");
			System.exit(0);
		}

		//Parametro del lookup: lo armare en base a la entrada. 
		String parLookUp = "rmi://" + server + ":" + port + "/FileManagerService";

		FileManager c = (FileManager)Naming.lookup(parLookUp); 
		String username = "";
		String key = "";
		
		if (user == null){
			//Pedimos nombre y clave del usuario (Despues esto se hace solo si no
			//se especifica archivo de texto)
			System.out.println("--Autenticacion--");
			Scanner keyboardUsername = new Scanner(System.in);
			System.out.println("Introduzca username de usuario: ");
			username = keyboardUsername.nextLine();

			Scanner keyboardKey = new Scanner(System.in);
			System.out.println("Introduzca su clave: ");
			key = keyboardKey.nextLine();
		}
		else {
			String[] retorno_lect = leerArchivoUser(user);
			if (retorno_lect != null){
				username = retorno_lect[0];
				key = retorno_lect[1];

				if (retorno_lect[2].equals("2")){
					String warning = "Alerta: Ha introducido un archivo con mas de un par nombre:clave";
					warning += ",para efectos de autenticacion se tomara en cuenta solo el primer par\n";
					System.out.println(warning);
				}
			}
			else {
				System.out.println("--Autenticacion--");
				Scanner keyboardUsername = new Scanner(System.in);
				System.out.println("Introduzca username de usuario: ");
				username = keyboardUsername.nextLine();

				Scanner keyboardKey = new Scanner(System.in);
				System.out.println("Introduzca su clave: ");
				key = keyboardKey.nextLine();
			}
		}
		//Pedimos autenticacion inicial
		if (c.initialAuth(username,key) == 0){
			String warning2 = "Alerta: Nombre/clave provistos no validos.";
			warning2 += "No podra realizar ninguna operacion, pues todas intentaran autenticarlo\n";
			warning2 += "Si desea utilizar las funcionalidades, reingrese de nuevo con credenciales validas\n"; 
			System.out.println(warning2);
		}

		if (commands != null){
			try{
				// Abrimos el archivo de comandos
				FileInputStream fstream = new FileInputStream(commands);
				// Creamos el objeto de entrada
				DataInputStream input = new DataInputStream(fstream);
				// Creamos el Buffer de Lectura
				BufferedReader buffer = new BufferedReader(new InputStreamReader(input));
				String strLine;
				// Leemos el archivo linea por linea
				while ((strLine = buffer.readLine()) != null){
					System.out.println(strLine);
					executeRequest(c, strLine, username, key); 
				}
				input.close();
			  
			} catch (Exception e){
				System.err.println("Ocurrio un error: " + e.getMessage());
			}
		}

		System.out.println("Hola " + username + ", utilice el comando 'info' para obtener informacion de los demas comandos.");

		while (true){
			String request;
			Scanner keyboardRequest = new Scanner(System.in);
			request = keyboardRequest.nextLine();
			executeRequest(c, request, username, key);
		}
	} 
		catch (MalformedURLException murle){
			System.out.println("MalformedURLException");
			System.out.println(murle);
		}
		catch (RemoteException re){
			System.out.println();
			System.out.println("RemoteException");
			System.out.println(re);
		}
		catch (NotBoundException nbe){
			System.out.println();
			System.out.println("NotBoundException");
			System.out.println(nbe);
		}
		catch (java.lang.ArithmeticException ae){
			System.out.println();
			System.out.println("java.lang.ArithmeticException");
			System.out.println(ae);
		}
	}
}
