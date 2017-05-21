import java.io.*;
import java.rmi.Naming;
import java.util.*; 

/**
 * Esta clase actua como la implementacion de la interfaz de la clase de la cual
 * el servidor ofrecera un objeto remoto
 * @author: Carlos Da Silva  (10-10175)
 * @author: Patricia Wilthew (09-10910)
 */

public class FileManagerImpl
    extends 
      java.rmi.server.UnicastRemoteObject
    implements FileManager {

		/** Atributos de la clase **/
		public static final long serialVersionUID = 1L;
		public String log = "";
		public Authentication auth; //Objeto de autenticacion
		public List<String> files = new ArrayList<String>(); //Lista de archivos
		public List<String> owners = new ArrayList<String>(); //Lista de duenos de archivos

		/**
		* Constructor de la clase. 
		* Llenamos la lista de archivos
		* con aquellos que ya se encuentren en la carpeta. Su dueno sera
		* un superusuario especial: "~serv_owned~".
		* @param a Objeto de la clase ofrecida por el servidor de autenticacion.
		*/
		public FileManagerImpl(Authentication a)
			throws java.rmi.RemoteException {
				super();
				this.auth = a;
				// Directory path here
				String path = "."; 
				String files;
				File folder = new File(path);
				File[] listOfFiles = folder.listFiles(); 
		  
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					files = listOfFiles[i].getName();
					this.files.add(files);
					this.owners.add("~serv_owned~");
				}
			}
		}

		/**
		* Metodo que obtiene logs del sistema
		* @return un String con los ultimos logs guardados
		*/
		public String getLogs(){
			return this.log;
		}

		/**
		* Metodo que muestra la informacion sobre los posibles comandos
		* @param name Nombre de usuario
		* @param key Clave de dicho usuario
		* @return Un String con la informacion
		*/
		public String info(String name, String key)
			throws java.rmi.RemoteException {
				String name_key = name + ":" + key;
				this.log += "info";
				this.log += ",";
				this.log += name;
				this.log += "~";
				if (this.auth.authenticate(name_key) == 1){
					String linea = "--> RLS: muestra la lista de archivos disponibles en servidor centralizado. \n";
					linea += "\n";
					linea += "--> LLS: muestra la lista de archivos disponibles localmente (en el cliente). \n";
					linea += "\n";
					linea += "--> sub archivo: sube un archivo al servidor remoto.\n";
					linea += "\n";
					linea += "--> baj archivo: baja un archivo desde el servidor remoto.\n";
					linea += "\n";
					linea += "--> bor archivo: borra el archivo en el servidor remoto.\n";
					linea += "\n";
					linea += "--> info: muestra la lista de comandos que el cliente puede";
					linea += " usar con una breve descripcion de cada uno de ellos.\n";
					linea += "\n";
					linea += "--> sal: termina la ejecucion del programa cliente, notificando";
					linea += " este hecho a todos los procesos del sistema que lo requieran.\n";
					return linea;
				}
				else {
					return ">>Error: Fallo de autenticacion";
				}
			}

		/**
		* Metodo que Determina si el usuario "nombre" es dueno del archivo "filename"
		* @param name Nombre de usuario
		* @param key Clave de dicho usuario
		* @return 1 en caso de que si sea el dueno, 0 en caso contrario
		*/
		public int fileOwner(String name, String filename){
			//Obtengo indice en el arrayList del nombre de archivo.
			int k = this.files.indexOf(filename);
			if (k == -1){
				return 0;
			}
			//Verifico que el nombre de usuario dueno corresponda.
			if (this.owners.get(k).equals(name)){
				return 1;
			}
			return 0;
		}
		
		/**
		* Metodo que elimina un archivo de una lista de archivos
		* @param filename Nombre del archivo a remover
		*/
		public void disappearFile(String filename){
			int k = this.files.indexOf(filename);
			this.files.remove(k);
		}
		
		/**
		* Metodo que muestra una lista con los archivos en el servidor centralizado
		* @param name Nombre de usuario
		* @param key Clave de dicho usuario
		* @return La lista de archivos como String
		*/
		public String rls(String name, String key)
			throws java.rmi.RemoteException {
				this.log += "rls";
				this.log += ",";
				this.log += name;
				this.log += "~";
				String name_key = name + ":" + key;
				
				if (this.auth.authenticate(name_key) == 1){
					String output = "";
					for (int i = 0;i<this.files.size();i++){
						output += this.files.get(i);
						output += "\n";
					}
					return output;
				}
				else{
					return ">>Error: fallo de autenticacion";
				}
		}
		
		/**
		* Metodo que elimina un archivo especifico
	    	* @param name Nombre de usuario
		* @param key Clave de dicho usuario
		* @param filename Nombre del archivo a borrar
		* @return Un String que informa si se pudo realizar lo deseado
		*/
		public String deleteFile(String filename,String name, String key)
			throws java.rmi.RemoteException {
				String name_key = name + ":" + key;
				this.log += "bor";
				this.log += ",";
				this.log += name;
				this.log += "~";
				
				if (this.auth.authenticate(name_key) == 1){
					File f = new File(filename);
					if (!f.exists()){
						return ">>El archivo que desea eliminar no existe";
					}
					if (f.isDirectory()) {
						return ">>No puede borrar un directorio";
					}

					//Antes de hacer delete, debemos verificar que el usuario esta
					//borrando un archivo que realmente es de el.

					if (fileOwner(name, filename) == 1){
						f.delete();
						disappearFile(filename);
					return ">>Se ha borrado con exito el archivo deseado";
					}
					else {
						return ">>Error: No puede borrar un archivo que no es de su propiedad";
					}

				}
				else{
					return ">>Error: fallo de autenticacion";
				}
			}

		/**
		* Metodo que descarga un documento determinado del servidor
	    	* @param name Nombre de usuario
		* @param key Clave de dicho usuario
		* @param fileName Nombre del archivo a descargar
		* @return El nuevo archivo como una arreglo de bytes
		*/
		public byte[] downloadFile(String fileName, String name, String key){
			try {
				String name_key = name + ":" + key;
				this.log += "bajar";
				this.log += ",";
				this.log += name;
				this.log += "~";
				
				if (this.auth.authenticate(name_key) == 1){
					File file = new File(fileName);
					byte buffer[] = new byte[(int)file.length()];
					BufferedInputStream input = new
					BufferedInputStream(new FileInputStream(fileName));
					input.read(buffer, 0, buffer.length);
					input.close();
					return(buffer);
				}
				else {
					return (null);
				}
			} catch(Exception e){
				System.out.println("FileImpl: " + e.getMessage());
				return(null);
			}
		}
		
		/**
		* Metodo que sube un archivo al servidor
	  	* @param name Nombre de usuario
		* @param key Clave de dicho usuario
		* @param fileName Nombre del archivo a subir
		* @return Un string que informa si se pudo realizar la operacion o no
		*/
		public String uploadFile(String fileName, byte[] buffer, String name, String key){
			try {
				String name_key = name + ":" + key;
				this.log += "subir";
				this.log += ",";
				this.log += name;
				this.log += "~";
				
				if (this.auth.authenticate(name_key) == 1){
					if (this.files.indexOf(fileName) != -1){
						return "Error: Ya existe un archivo en el servidor con dicho nombre, cambielo e intente de nuevo";
					}
					else {
						File file = new File(fileName);
						BufferedOutputStream output = new
						BufferedOutputStream(new FileOutputStream(fileName));
						output.write(buffer, 0, buffer.length);
						output.flush();
						output.close();

						//Agrego el archivo/usuario a las listas correspondientes
						this.files.add(fileName);
						this.owners.add(name);
					}
				}
				else{
					return "Error: fallo de autenticacion";
				}
			} catch(Exception e) {
				System.out.println("FileImpl: " + e.getMessage());
				e.printStackTrace();
			}
			return "Archivo subido correctamente";
	  }

		/**
		* Metodo que devuelve si un cliente esta inicialmente autenticado
		* (valiendose del servidor de auth)
	    	* @param name Nombre de usuario
		* @param key Clave de dicho usuario
		*/
		public int initialAuth(String name, String key)
			throws java.rmi.RemoteException {
				String name_key = name + ":" + key;
				return this.auth.authenticate(name_key);
			}
	}

