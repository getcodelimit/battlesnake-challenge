#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

int PORT = 3000;

void handle_get_meta_data(int client_socket) {
    int buffer_size = 32768;
    char *buffer = (char *)malloc(buffer_size * sizeof(char));
    const char *header = 
        "HTTP/1.1 200 OK\r\n"
        "Content-Type: application/json\r\n\r\n";
    const char *meta_data = 
        "{\"apiversion\": \"1\", \"author\": \"robvanderleek\", "
        "\"version\": \"1.0\", \"color\": \"#555555\", "
        "\"head\": \"safe\", \"tail\": \"sharp\"}";
    sprintf(buffer, "%s%s", header, meta_data); 
    send(client_socket, buffer, strlen(buffer), 0);
    free(buffer);
}

void handle_start(int client_socket, const char* body) {
    const char *header = "HTTP/1.1 200 OK\r\n\r\n";
    send(client_socket, header, strlen(header), 0);
}

const char *get_body(const char *request) {
    return strstr(request, "\r\n\r\n") + 4;
}

int main(int argc, char *argv[]) {
    printf("Starting Battlesnake server on port: %d\n", PORT);
    int server_socket;
    server_socket = socket(AF_INET, SOCK_STREAM, 0);
    struct sockaddr_in server_address;
    server_address.sin_family = AF_INET;
    server_address.sin_addr.s_addr = INADDR_ANY;
    server_address.sin_port = htons(PORT);
    bind(server_socket, (struct sockaddr *)&server_address, 
        sizeof(server_address));
    listen(server_socket, 10);
    struct sockaddr client_address;
    socklen_t client_address_len = sizeof(client_address);
    while (1) {
        int client_socket = accept(server_socket, &client_address, 
            &client_address_len);
        int buffer_size = 32768;
        char *buffer = (char *)malloc(buffer_size * sizeof(char));
        size_t bytes = recv(client_socket, buffer, buffer_size, 0);
        const char *get_meta_data = "GET /";
        const char *post_start = "POST /start";
        if (strncmp(buffer, get_meta_data, strlen(get_meta_data)) == 0) {
            handle_get_meta_data(client_socket);
        } else if (strncmp(buffer, post_start, strlen(post_start)) == 0) {
            handle_start(client_socket, get_body(buffer));
        } else {
            printf("%s\n", buffer);
        }
        close(client_socket);
    }
}
