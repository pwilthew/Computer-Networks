CC = gcc
CFLAGS = -g
#LIBS = -lsocket -lnsl

all : schat cchat 

errors.o : errors.c errors.h
	$(CC) $(CFLAGS) -c errors.c -lpthread

schat : schat.c errors.o
	$(CC) $(CFLAGS) -o schat schat.c errors.o -lpthread $(LIBS)

cchat : cchat.c errors.o
	$(CC) $(CFLAGS) -o cchat cchat.c errors.o  -lpthread$(LIBS)
