#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <unistd.h>

int main(int argc, char *argv[]) {
    printf("Starting server...\n");
    int server_socket;
    server_socket = socket(AF_INET, SOCK_STREAM, 0);
    struct sockaddr_in server_address;
    server_address.sin_family = AF_INET;
    server_address.sin_addr.s_addr = INADDR_ANY;
    server_address.sin_port = htons(3000);
    bind(server_socket, (struct sockaddr *)&server_address, 
        sizeof(server_address));
    listen(server_socket, 10);
    struct sockaddr client_address;
    socklen_t client_address_len = sizeof(client_address);
    while (1) {
        int client_socket;
        printf("Listening...\n");
        client_socket = accept(server_socket, &client_address, 
            &client_address_len);
        printf("Connected!\n");
        int buffer_size = 32768;
        char *buffer = (char *)malloc(buffer_size * sizeof(char));
        size_t bytes = recv(client_socket, buffer, buffer_size, 0);
        printf("Hello world!\n");
        close(client_socket);
    }
}
