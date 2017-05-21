/************************************************************
*                             SERVIDOR PARA CHAT            *
************************************************************/
/**
 * CCHAT: programa servidor de la aplicacion de chat realizada.
 * @author Carlos Da Silva
 * @author Patricia Wilthew
 * @version 1.0
 */

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <pthread.h>
#include <netinet/in.h>
#include <string.h>
#include <unistd.h>
#include <semaphore.h> 

#include "errors.h"

#define QUEUELENGTH 300

//Semaforo
sem_t mutex;


//Tipo que representa los datos que debe recibir un hilo al ser creado
typedef struct{
  int socket;
}DATOS;


//Tendremos un arreglo de salas. Cada sala es simplemente su nombre.
typedef struct{
  char sala[50];
}SALAS;


//Tipo que representa un usuario que se conecto al server via CCHAT.
typedef struct{
    char nombre[50];
    SALAS lsalau[40];
    char sala[50];
    int socket;
    int itsala;
}USUARIO;


//Variables que representan los argumentos de entrada del programa.
char *puerto="-1";
char *sala="Actual";


//Arreglo general de usuarios.
USUARIO lusuarios[200];
int iterador=0;


//Arreglo de salas del servidor.
SALAS lsalas[200];
int iterador_salas=0;


//Arreglo de hilos. Se tiene un hilo especializado en atender a cada usuario
//que se conecte.
pthread_t *Hilos;
int iterador_hilos=0;


/**
  * Procedimiento que elimina a un usuario del servidor. Para ello
  * lo hace inalcanzable en el arreglo.
  *
  * @param nombre[] Username del usuario a eliminar.
  */
void EliminarSocketUsuario(char nombre[]){
  int i=0;
  while (i<200){
    if (!strcmp((lusuarios[i].nombre),nombre)){
      strcpy(lusuarios[i].nombre,"0");
      lusuarios[i].itsala=0;
    }
    i++;
  }
}


/**
  * Procesa una instruccion previamente desglosada. Para
  * se determina que tipo de instruccion es y en base a eso
  * se procede.
  * @param tipo[] Instruccion a ejecutar
  * @param argumento[] Argumento necesario para el tipo de instruccion
  * @param usuario[] Usuario que desea ejecutar la instruccion dada por tipo.
  */
void EjecutarInstruccion(char tipo[],char argumento[],char usuario[]){
  
   sem_wait(&mutex);  //Verificamos si podemos entrar
   
  /*Si la instruccion es enviar un mensaje, en sintesis: Vemos a que salas
   *pertenece el usuario, para asi buscar todas las demas personas que
   *pertenezcan a dichas salas y enviarles el mensaje a travez de su socket
   */
  if (!strcmp(tipo,"men")){
    char sala[20];
    char total[954];
    SALAS listasalas[20];
    memset(total,0,954);
    
    //Se consigue la posicion del usuario en el arreglo de usuarios.
    int y=0;
    int p=0;
    while (y<200) {
      if (!strcmp(lusuarios[y].nombre,usuario)){
	break;
      }
      y++;
    }
    if (lusuarios[y].itsala==0){
      write (lusuarios[y].socket, "Error: No se encuentra en ninguna sala", 70);
    }
    else{
      int k=0;
      int i=0;
      for (i;i<200;i++){
	    int w=0;
	    
	    /*Iteramos y enviamos el mensaje a traves de los sockets cuyo usuario
	     *concuerde en sala con el remitente
	     */
	    for (w;w<lusuarios[i].itsala;w++){
	      if (SalaContieneAUsuario(lusuarios[i].lsalau[w].sala,lusuarios[y].lsalau,lusuarios[y].itsala)){
	        strcat(total,usuario);
	        strcat(total,"(");
	        strcat(total,lusuarios[i].lsalau[w].sala);
	        strcat(total,")");
	        strcat(total,": ");
	        strcat(total,argumento);
	        write (lusuarios[i].socket, total, strlen(total)+1);
	        memset(total,0,954);
            }
        }
      }
    }
  }
  
  /*Si el comando es sal, se arma un string a enviar que contenga todas
   *las salas del sistema
   */
  else if (!strcmp(tipo,"sal")){
    char total[700];
    memset(total,0,700);
    int j=0;
    for (j;j<iterador_salas;j++){
      if (strcmp(lsalas[j].sala,"-1~")){
	    strcat(total,lsalas[j].sala);
	    strcat(total,"\n");
      }
    }
    int sock;
    sock=ObtenerSocketUsuario(usuario);
    write (sock,total,strlen(total)+1);
    
  }
  
  /*Para crear una sala, primero verificamos que esta ya no exista y en caso
   *de que no, la agregamos al arreglo de salas
   */
  else if (!strcmp(tipo,"cre")){
    int w=UbicarUsuario(usuario);
    if (!EstaSala(argumento,lsalas)){
      strcpy(lsalas[iterador_salas].sala,argumento);
      iterador_salas++;
      write(lusuarios[w].socket,"Exito: Sala creada satisfactoriamente",65);
    }
    else {
      write(lusuarios[w].socket,"Error: Sala ya creada",65);
    }
  }
  
  /*Analogamente al comando "sal", se arma un string que contenga todos
   *los clientes conectados
   */
  else if (!strcmp(tipo,"usu")){
    char total[700];
    memset(total,0,700);
    int j=0;
    for (j;j<iterador;j++){
      if (strcmp(lusuarios[j].nombre,"0")){
	    strcat(total,lusuarios[j].nombre);
	    strcat(total,"\n");
      }
    }
    int sock;
    sock=ObtenerSocketUsuario(usuario);
    write (sock,total,strlen(total)+1);
  }
  
  /*Si el cliente desea irse, se procede concordemente y se culmina el hilo
   *que se encargaba de atenderlo
   */
  else if (!strcmp(tipo,"fue")){
    int w=UbicarUsuario(usuario);
    write(lusuarios[w].socket,"-2",4);
    EliminarSocketUsuario(usuario);
    sem_post(&mutex); //Dejamos libre el camino
    pthread_exit(NULL);
  }
  
  /*Para suscribirse a una sala se debe verificar que la sala no exista y
   *en caso de que exista, que el usuario no se encuentre ya suscrito a la misma
   */
  else if(!strcmp(tipo,"sus")){
    int w=UbicarUsuario(usuario);
    if (EstaSala(argumento,lsalas)){
      int w=UbicarUsuario(usuario);
      if (SalaContieneAUsuario(argumento,lusuarios[w].lsalau,lusuarios[w].itsala)){
	    write(lusuarios[w].socket,"Error: Ya se esta sucrito a dicha sala",65);
      }
      else{
	    int pos=lusuarios[w].itsala;
	    strcpy(lusuarios[w].lsalau[pos].sala,argumento);
	    lusuarios[w].itsala++;
	    write(lusuarios[w].socket,"Exito: Se ha suscrito a la sala deseada",65);
      }
    }
    else {
      write(lusuarios[w].socket,"Error: El nombre de la sala a la cual se desea suscribir, no existe",70);
    }
  }
  
  /*Al momento de eliminar una sala, verificamos si dicha sala esta vacia, en
   *caso de estarlo, el cliente esta en derecho de eliminarla
   */
  else if (!strcmp(tipo,"eli")){
    if (!strcmp(argumento,sala)){
      int w=UbicarUsuario(usuario);
      write(lusuarios[w].socket,"Error: No puede eliminar la sala default del server",70);
    }
    else{
      if (EstaSala(argumento,lsalas)){
	    if (SalaVacia(argumento)){
	      int i=0;
	      for (i;i<iterador_salas; i++){
	        if (!strcmp(argumento,lsalas[i].sala)){
		        strcpy(lsalas[i].sala,"-1~");
		        int w=UbicarUsuario(usuario);
		        write(lusuarios[w].socket,"Exito: Se ha eliminado la sala solicitada",70);
	        }
	      }
	    }
	    else {
	      int w=UbicarUsuario(usuario);
	      write(lusuarios[w].socket,"Error: No puede eliminar una sala que no esta vacia",70);
	    }
      }
      else {
	    int w=UbicarUsuario(usuario);
	    write(lusuarios[w].socket,"Error: La sala no existe",70);
      }
    }
  }
  
  /*Para desuscribir a un usuario de sus salas, se reinicia el iterador sobre
   *su arreglo personal de salas*
   */
  else if (!strcmp(tipo,"des")){
    int w=UbicarUsuario(usuario);
    lusuarios[w].itsala=0;
    write(lusuarios[w].socket,"Exito: Se ha desuscrito de todas las salas",70);
  }
  
  /*Finalmente, si no se especifico ninguno de los comandos previos, se especifico
   *un comando erroneo, y se le envia un mensaje pertinente al usuario
   */
  else {
    char error[60];
    strcpy(error,"Error: Ha introducido un comando erroneo!");
    int sock;
    sock=ObtenerSocketUsuario(usuario);
    write (sock,error,(strlen(error)+1));
  }
  
  sem_post(&mutex); //Dejamos libre el camino
}


/**
  * Interpreta una instruccion enviada por un cliente. La desglosa
  * y pasa el control a la funcion que ejecute a instruccion procesada.
  *
  * @param instruccion[] Instruccion a interpretar/procesar
  */
void InterpretarInstruccion(char instruccion[]){
  char tipo_instr[4];
  char argumento[855];
  char usuario[50];
  memset(usuario,0,50);
  memset(argumento,0,855);
  memset(tipo_instr,0,4);

  /*A continuacion, se desglosa toda instruccion en tres campos vitales
   *para su funcionamiento: tipo de instruccion,argumento y usuario ejecutante
   *(todas vienen empaquetadas y separadas por el caracter especial "~"
   */
  int i=0;
  for (i;i<3;i++){
    tipo_instr[i]=instruccion[i];
  }

  i=i+1;
  int j=0;
  int w=0;
  for (i;i<strlen(instruccion);i++){
    if (instruccion[i] == '~'){
      break;
    }
    else {
      argumento[w]=instruccion[i];
      w++;
    }
  }
  i=i+1;
  
  for (i;i<strlen(instruccion);i++){
    usuario[j]=instruccion[i];
    j++;
  }
  
  //Finalmente, una vez obtenido lo necesario, invocamos a la funcion para ejecutar
  EjecutarInstruccion(tipo_instr, argumento, usuario);
}


/**
  * Se utiliza para obtener la posicion (numero) de un usuario en la lista de usuarios.
  *
  * @param usuario[] String correspondiente al nombre del usuario.
  * @return int Entero correspondiente a la posicion del usuario en el arreglo de usuarios.
  */
int UbicarUsuario(char usuario[]){
   int y=0;
   while (y<200) {
      if (!strcmp(lusuarios[y].nombre,usuario)){
	return y;
      }
      y++;
    }
}


/**
  * Procedimiento que recibe instrucciones de un cliente y solicita que se interpreten.
  *
  * @param sockfd Numero de socket que contiene la instruccion.
  */
void RecibirInstruccion(int sockfd){
  char buffer[955];
  memset(buffer,1,955);
  read (sockfd,buffer,strlen(buffer));
  InterpretarInstruccion(buffer);
}


 /**
  * Todos los hilos son creados para ejecutar esta funcion, que atendera
  * en un loop los comandos del cliente dado por el socket que le entra.
  *
  * @param *estructura Tipo de datos que contiene la informacion que el hilo necesita. 
  */
void *Atender(void *estructura){
  DATOS *info;
  info = (DATOS *)estructura;
  int newsockfd=info->socket;
  //Nos mantenemos siempre escuchando al cliente que atendemos.
  while (1){
    RecibirInstruccion(newsockfd);
  }
}


/**
  * Funcion para determinar si un usuario existe en el arreglo general de usuarios.
  *
  * @param nombre[] Username a determinar si existe.
  * @return int Retorna 1 en caso de que el usuario no se encuentre en el arreglo, 0 en
  * el otro caso. 
  */
int EstaUsuario(char nombre[]){
  int i=0;
  for (i;i<iterador;i++){
    if (!strcmp(nombre,lusuarios[i].nombre)){
      return 0;
    }
  }
  return 1;
}


/**
  * Procedimiento encargado de recibir la informacion inicial cuando un cliente se
  * conecta al servidor. Recibe su nombre y hace las asignaciones pertinentes.
  *
  * @param sockfd Socket por el cual se recibira la informacion.
  */
void RecibirInfoInicial(int sockfd) {
  char *c;
  int status;
  char buffer[255];
  memset(buffer,1,255);
  int i=0;
  read (sockfd, buffer, strlen(buffer));
  int posicion=0;

  /*Si el usuario ya existe, se retorna codigo de error. En caso contrario, se
   *agrega el usuario a la lista y se le asigna a la sala por default del servidor
   */
  if (EstaUsuario(buffer)){
    lusuarios[iterador].itsala=0;
    strcpy(lusuarios[iterador].nombre,buffer);
    posicion=lusuarios[iterador].itsala;
    strcpy((lusuarios[iterador].lsalau[posicion].sala),sala);
    lusuarios[iterador].itsala++;
    lusuarios[iterador].socket=sockfd;
    iterador=iterador+1;
  }
  else{
    write (sockfd, "-1", 70);
  }
}


/**
  * Busca un usuario en el arreglo de los mismos, y devuelve el file descriptor de
  * su socket.
  *
  * @param nombre[] Nombre del usuario al cual se le buscara su socket.
  * @return Devuelve el socket del usuario y -1 si no se consigue.
  */
int ObtenerSocketUsuario(char nombre[]){
  int i=0;
  while (i<200){
    if (!strcmp((lusuarios[i].nombre),nombre)){
      return lusuarios[i].socket;
    }
    i++;
  }
  return -1;
}


/**
  * Determina si una sala (por su nombre) se encuentra en la lista de salas
  * del servidor.
  *
  * @param nombresala[] Sala a buscar dentro del arrego.
  * @param lsalas[] Arreglo de salas en donde buscarla.
  * @return int Devuelve 1 en caso de que la sala sea ubicada, 0 en caso contrario
  */
int EstaSala(char nombresala[], SALAS lsalas[]){
  int k=0;
  int w=0;
  for (w;w<iterador_salas;w++){
    if (!strcmp(nombresala,lsalas[w].sala)){
      return 1;
    }
  }
  return 0;
}


/**
  * Determina si una sala esta en el arreglo de salas interno a un usuario
  * dado.
  *
  * @param nombresala[] Sala a buscar dentro del arrego.
  * @param lsalas[] Arreglo de salas en donde buscarla.
  * @param itsala Entero que determina hasta donde iterar sobre lsalas.
  * @return Devuelve 1 en caso de que se ubique a la sala. 0 en caso contrario.
  */
int SalaContieneAUsuario(char nombresala[], SALAS lsalas[],int itsala){
  int k=0;
  int w=0;
  for (w;w<itsala;w++){
    if (!strcmp(nombresala,lsalas[w].sala)){
      return 1;
    }
  }
  return 0;
}


/**
  * Determina si una sala esta vacia o no.
  *
  * @param nombresala[] Sala a determinar si esta vacia o no.
  * @return int Devuelve 0 en caso de que la sala no este vacia, 1 en caso contrario.
  */
int SalaVacia(char nombresala[]){
  int i=0;
  for (i;i<iterador;i++){
    if (SalaContieneAUsuario(nombresala,lusuarios[i].lsalau,lusuarios[i].itsala)){
      return 0;
    }
  }
  return 1;
}


/**
  * Lee y procesa los argumentos leidos por linea de comandos al momento
  * de la llamada a ejecutar el programa.
  * @param *params[] Arreglo de argumentos.
  * @param argc Numero de argumentos que hay en el arreglo params.
  */
void lectura(char *params[], int argc){
  
  //Se revisa primero la longitud del arreglo para ver si no se escribio algun argumento
  if (argc < 5){
    if ((!strcmp(params[1],"-p"))){
      puerto=params[2];
    }
    else if ((!strcmp(params[1],"-s"))){
      sala=params[2];
    }
    else {
      fatalerror("Parametros de entrada incompletos (puerto,sala)");
    }
    if ((!strcmp(sala,"-1"))){
      sala="Actual";
    }
  }
  else if ((!strcmp(params[1],"-p"))) {
    puerto=params[2]; 
    sala=params[4];
  }
  else if ((!strcmp(params[1],"-s"))){
    sala=params[2];
    puerto=params[4]; 
  }
  
  //Se verifica que despues de analizar, no se haya dejado algo en -1
  if (((!strcmp(puerto,"-1")))){
    fatalerror("Parametros de entrada incompletos (puerto,sala)");
  }
}


/**
  * Programa principal.
  * @param *argv[] Argumentos obtenidos de linea de comandos
  * @param argc Numero de argumentos
  */
main(int argc, char *argv[]) {
  int sockfd, newsockfd;
  struct sockaddr_in clientaddr, serveraddr;
  int clientaddrlength;
  int pid;
  Hilos = malloc((200)*sizeof(pthread_t));
  programname = argv[0];
  sem_init(&mutex, 0, 1);  //Inicializacion del semaforo
  
  
  //Se realiza la lectura para obtener en las variables puerto-sala, dichos 
  //valores introducidos por el usuario.
  lectura(argv,argc);
  strcpy(lsalas[iterador_salas].sala,sala);
  iterador_salas++;

  //Se abre el socket.
  sockfd = socket(AF_INET, SOCK_STREAM, 0);
  if (sockfd < 0)
    fatalerror("can't open socket");

  //Se hace el Bind.
  bzero(&serveraddr, sizeof(serveraddr));
  serveraddr.sin_family = AF_INET;
  serveraddr.sin_addr.s_addr = htonl(INADDR_ANY);
  serveraddr.sin_port = htons(atoi(puerto));
  if (bind(sockfd, (struct sockaddr *) &serveraddr, sizeof(serveraddr)) != 0)
    fatalerror("can't bind to socket");
  
  //Se escucha.
  if (listen(sockfd, QUEUELENGTH) < 0)
    fatalerror("can't listen");
  
  /*Ciclo que espera por conexiones de cliente. Ante cada conexion, se crea
   *un hilo que se encargara de los requests de dicho usuario.
   */
   while (1) {
    clientaddrlength = sizeof(clientaddr);
    newsockfd = accept(sockfd, 
                       (struct sockaddr *) &clientaddr,
                       &clientaddrlength);
    if (newsockfd < 0)
      fatalerror("accept failure");
    
    //Despues de recibir y aceptar conexion, se espera que se envie
    //la informacion personal del cliente recien conectado.
    RecibirInfoInicial(newsockfd);

    /*Despues de recibir la informacion inicial, se crea un Hilo, que sera
     *el encargado de manejar todos los requests del cliente recien conectado
     *El hilo debe recibir el socket del cual atendera.
     */
    
    //Se crea la estructura de datos que recibira el hilo y tambien el hilo.
    DATOS d;
    d.socket=newsockfd;
    pthread_create(&Hilos[iterador_hilos],NULL,Atender,(void *)&d);
    iterador_hilos++;
  }
}
