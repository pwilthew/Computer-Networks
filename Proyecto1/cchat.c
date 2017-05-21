/************************************************************
*                        CLIENTE PARA CHAT                  *
************************************************************/
/**
 * CCHAT: programa cliente de la aplicacion de chat realizada.
 * @author Carlos Da Silva
 * @author Patricia Wilthew
 * @version 1.0
 */

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <string.h>
#include <unistd.h>
#include <netdb.h>
#include <arpa/inet.h>
#include "errors.h"

#define PORT 25503


// Variables globales del programa: representan los argumentos entrantes por
// linea de comandos.
char *host="-1";
char *puerto="-1";
char *nombre="-1";
char *archivo="-1"; //Podra ser opcional


//Se declara el arreglo de hilos (Por la naturaleza del programa, solo se tendra 1).
pthread_t *Hilos;
int iterador_hilos=0;


//Tipo de estructura necesaria para el pase de parametros al hilo.
typedef struct{
  int socket;
}DATOS;


 /**
  * Funcion que envia al server la informacion basica del usuario que se acaba
  * de conectar.
  *
  * @param sockfd A traves del cual se enviara la informacion inicial del usuario.
  * @param info[] Informacion que se enviara.
  */
void enviarInfoInicial(int sockfd, char info[]) {
  int c;
  char outbuffer[255];
  char inbuffer[255];
  memset(inbuffer,0,255);
  int tam;
  tam=strlen(info); 
  write (sockfd, info, tam+1);
}


 /**
  * Funcion que dado un socket y un string comando, prepara dicho string a un formato
  * concorde al protocolo y lo envia por dicho socket. Dicho string sera una 
  * indicacion al server de la accion que desea realizar el usuario.
  *
  * @param sockfd A traves del cual se enviara la informacion inicial del usuario
  * @param comando[] Comando ingresado por el usuario por entrada estandar. 
  * Se procesara y se enviara.
  */
void mandarInstruccion(int sockfd, char comando[]){
  //Antes de enviar instruccion se prepara en un arreglo, con la accion de la instruccion,
  //el argumento de la instruccion si lo tiene y el usuario que la hace.
  char accion[3000];
  memset(accion,0,3000);
  char argumento[1000];
  memset(argumento,0,1000);
  char msj_preparado[500];
  memset(msj_preparado,0,500);
  
  //Se obtiene la accion y el argumento de la instruccion.
  int i=0;
  while (i<3){
    accion[i]=comando[i];
    i=i+1;
  }
  int j=4;
  while (j<strlen(comando)){
    if (j == strlen(comando)-1){
      ;
    }
    else{
      argumento[j-4]=comando[j];
    }
    j=j+1;
  }
  
  if (strlen(accion) == 2){
    printf("Error: Ha introducido un comando erroneo!\n");
  }
  else{
    //Se concatenan los strings.
    strcat(accion,"~");
    strcat(accion,argumento);
    strcat(accion,"~");
    strcat(accion,nombre);
    write (sockfd,accion,strlen(accion)+1);
  }

  /*Nota: Al final de la prepracion, se le envia al server un string con el siguiente formato:
    <Accion> ~ <Argumento de la accion> ~ <Usuario que desea hacer la accion>  */
}


 /**
  * Procedimiento que analiza dos strings contiguos de un arreglo, dependiendo
  * del valor del primero, se determina a que hace referencia el segundo.
  * Recibe el arreglo y el indice desde el cual realizar la verificacion.
  *
  * @param *arg1[] String a analizar
  * @param index1[] Posicion del string a analizar
  */
void Analizar(char *arg1[],int index1){
  if (!strcmp(arg1[index1],"-h")){
    host=arg1[index1+1];
  }
  if (!strcmp(arg1[index1],"-p")){
    puerto=arg1[index1+1];
  }
  if (!strcmp(arg1[index1],"-n")){
    nombre=arg1[index1+1];
  }
  if (!strcmp(arg1[index1],"-a")){
    archivo=arg1[index1+1];
  }
}

 /**
  * Procedimiento que analiza el arreglo de parametros de entrada y asigna en
  * caso de introduccion de los mismos correcta.
  *
  * @param *params[] Parametros obtenidos por entrada estandar.
  * @param argc Tamano del arreglo anterior.
  */
void Lectura(char *params[], int argc){
    //Si hay 9 args, se han introducido en teoria todos los args.
    if (argc == 9){
      int i=1;
      for (i = 1; i < 8; i =i+ 2){
        Analizar(params,i);
      }
    }
    //Si hay 7 args se ha dejado de introducir uno, podria ser valido, si
    //no introdujo el nombre de archivo
    else if (argc == 7){
      int i=1;
      for (i = 1; i < 6; i =i+ 2){
	Analizar(params,i);
      }
    }
    //Menos de 7 ya significa que ha olvidado colocar uno obligatorio.
    else {
      fatalerror("Ha faltado algun parametro obligatorio");
    }
    //Se verifico que los obligatorios no hayan quedado sin signar.
    if ((!strcmp(puerto,"-1")) || (!strcmp(host,"-1")) || (!strcmp(nombre,"-1"))){
      fatalerror("Ha faltado algun parametro obligatorio");
    } 
}


 /**
  * Funcion con la que se crea el hilo encargado de escuchar del socket. Dicho hilo
  * se quedara siempre ciclando esperando a recibir algo del socket, si recibe algo
  * lo imprime.
  *
  * @param *estructura Tipo de datos que contiene la informacion que el hilo necesita. 
  *                                Dicha informacion es el socket que atendera.
  */
void *Atender(void *estructura){
  DATOS *info;
  info = (DATOS *)estructura;
  int newsockfd=info->socket;
  char recibido[755];
  memset(recibido,1,755);
  while (1){
    read(newsockfd, recibido, 755);
    if (!strcmp(recibido,"-1")){
      printf ("Error: Su nombre de usuario ya existe, desconectese(fue) y vuelva a ingresar con otro nombre de usuario\n");
      pthread_exit(NULL);
    }
    //Si el comando es fue en el archivo, cerrar.
    if (!strcmp(recibido,"-2")){
      close(newsockfd);
      exit(0);
    }
    printf ("%s \n \n",recibido);
    memset(recibido,0,755);
  }
}


 /**
  * Programa principal
  *
  * @param argc Numero de argumentos
  * @param *argv[] Arreglo de argumentos
  */
main(int argc, char *argv[]) {
  int sockfd;
  struct sockaddr_in serveraddr;
  char *server;
  Hilos = malloc((200)*sizeof(pthread_t)); //Arreglo de hilos.
  programname = argv[0];
  
  //Se realiza la lectura y se imprime para chequear que se leyo todo bien.
  Lectura(argv,argc);
  server=host;
  
  //Se obtiene la direccion del servidor y se llena la estructura.
  struct in_addr inaddr;
  struct hostent *serv;
  if (inet_aton(server,&inaddr)){
    serv=gethostbyaddr((char *) &inaddr,sizeof(inaddr),AF_INET);
  }
  else {
    serv=gethostbyname(server);
  }
  bzero(&serveraddr, sizeof(serveraddr));
  serveraddr.sin_family = AF_INET;
  memcpy(&serveraddr.sin_addr, serv->h_addr_list[0],sizeof(serveraddr.sin_addr));
  serveraddr.sin_port = htons(atoi(puerto));

  //Se abre el socket.
  sockfd = socket(AF_INET, SOCK_STREAM, 0);
  if (sockfd < 0)
    fatalerror("can't open socket");

  //Una vez abierto el socket, se conecta el cliente al servidor.
  if (connect(sockfd, (struct sockaddr *) &serveraddr,
              sizeof(serveraddr)) < 0)
    fatalerror("can't connect to server");

  //Recien conectado, se debe enviar al socket cierta informacion basica:
  //El nombre de usuario.
  enviarInfoInicial(sockfd,nombre);

  char buff[700];
  memset(buff,0,700);
 
  //Creacion del hilo que se quedara siempre escuchando.
  DATOS d;
  d.socket=sockfd;
  pthread_create(&Hilos[iterador_hilos],NULL,Atender,(void *)&d);
  iterador_hilos++;
  
  FILE * fd=fopen (archivo, "r");
  if (fd == NULL){
    printf ("Error: No se encuentra el archivo\n");
  }
  else{
    char linea[1000];
    while (fgets(linea,1000,fd)){
      printf ("Comando: %s", linea);
      mandarInstruccion(sockfd,linea);
      sleep(1);
    }
  }
  printf("CCHAT: terminal interactivo, escriba el comando a ejecutar:\n");
  
  //El que no es un hilo se queda siempre esperando por si se quiere
  //introducir un comando. Se obtiene el comando que se desea realizar, y se
  //envia al server llamando a la funcion MandarInstruccion
  while (1){
      fgets(buff, sizeof buff, stdin);
      mandarInstruccion(sockfd,buff);
      if ((buff[0] == 'f') & (buff[1] == 'u') & (buff[2] == 'e')){
	    printf("Cchat se ha cerrado. \n");
	    close(sockfd);
	    exit(0);
      }
      memset(buff,0,700);
    }
}
