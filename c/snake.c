#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

int PORT = 3000;

char * get_field(const char *json, const char *name) {
    char *needle = (char *)malloc((strlen(name) + 3) * sizeof(char));
    sprintf(needle, "\"%s\"", name);
    char *ptr = strstr(json, needle);
    if (ptr == NULL) {
        printf("Pointer is NULL!\n");
        printf("%s\n", needle);
        printf("%s\n", json);
    }
    ptr += strlen(needle) + 1;
    free(needle);
    return ptr;
}

char * get_object(const char *json, const char *name, char *buffer) {
    char *ptr = get_field(json, name);
    int idx = 0, indent = 0;
    do {
        if (*ptr == '{')
            indent++;
        else if (*ptr == '}')
            indent--;
        buffer[idx++] = *ptr++;
    } while (indent > 0);
    buffer[idx] = '\0';
    return buffer;
}

char * get_array(const char *json, const char *name, char *buffer) {
    char *ptr = get_field(json, name);
    int idx = 0, indent = 0;
    do {
        if (*ptr == '[')
            indent++;
        else if (*ptr == ']')
            indent--;
        buffer[idx++] = *ptr++;
    } while (indent > 0);
    buffer[idx] = '\0';
    return buffer;
}

int get_number(const char *json, const char *name) {
    char *ptr = get_field(json, name);
    return (int)strtol(ptr, NULL, 10);
}

char * get_text(const char *json, const char *name, char *buffer) {
    char *ptr = get_field(json, name);
    ptr++;
    int idx = 0;
    while (*ptr != '"') {
        buffer[idx++] = *ptr++;
    }
    buffer[idx] = '\0';
    return buffer;
}

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
    char *buffer = (char *)malloc(strlen(body) * sizeof(char));
    char *game = get_object(body, "game", buffer);
    char *id = get_text(game, "id", buffer);
    printf("Game started: %s\n", id);
    free(buffer);
    const char *header = "HTTP/1.1 200 OK\r\n\r\n";
    send(client_socket, header, strlen(header), 0);
}

void preferred_directions(char *board, int head_x, int head_y, int dirs[]) {
    char *buffer = (char *)malloc(strlen(board) * sizeof(char));
    char *food = get_array(board, "food", buffer); 
    printf("%s\n", food);
    dirs[0] = 0;
    dirs[1] = 1;
    dirs[2] = 2;
    dirs[3] = 3;
}

char *get_direction(char *board, int head_x, int head_y) {
    int dirs[4];
    preferred_directions(board, head_x, head_y, dirs);
    switch (dirs[0]) {
        case 0:
            return "left";
        case 1:
            return "up";
        case 2:
            return "right";
        default:
            return "down";
    }
}

void handle_move(int client_socket, const char* body) {
    int turn = get_number(body, "turn");
    printf("turn: %d\n", turn);
    char *buffer = (char *)malloc(strlen(body) * sizeof(char));
    char *head = get_object(get_object(body, "you", buffer), "head", buffer);
    int head_x = get_number(head, "x");
    int head_y = get_number(head, "y");
    char *board = get_object(body, "board", buffer);
    char *direction = get_direction(board, head_x, head_y);
    sprintf(buffer, 
        "HTTP/1.1 200 OK\r\n"
        "Content-Type: application/json\r\n\r\n"
        "{\"move\": \"%s\"}", direction); 
    send(client_socket, buffer, strlen(buffer), 0);
    free(buffer);
}

void handle_end(int client_socket) {
    const char *header = "HTTP/1.1 201 OK\r\n\r\n";
    send(client_socket, header, strlen(header), 0);
}

int get_content_length(char *request) {
    char *ptr = strstr(request, "Content-Length: ") + 16;
    return (int)strtol(ptr, NULL, 10);
}

char *get_body(int client_socket, char *buffer, int buffer_size, int bytes) {
    int content_length = get_content_length(buffer); 
    char *result = strstr(buffer, "\r\n\r\n") + 4;
    while (strlen(result) < content_length) {
        char *ptr = buffer + bytes;
        bytes += (int)recv(client_socket, ptr, buffer_size - bytes, 0);
        result = strstr(buffer, "\r\n\r\n") + 4;
    }
    return result;
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
        int bytes = (int)recv(client_socket, buffer, buffer_size, 0);
        const char *get_meta_data = "GET /";
        const char *post_start = "POST /start";
        const char *post_move = "POST /move";
        const char *post_end = "POST /end";
        if (strncmp(buffer, get_meta_data, strlen(get_meta_data)) == 0) {
            handle_get_meta_data(client_socket);
        } else if (strncmp(buffer, post_start, strlen(post_start)) == 0) {
            char *body = get_body(client_socket, buffer, buffer_size, bytes); 
            handle_start(client_socket, body);
        } else if (strncmp(buffer, post_move, strlen(post_move)) == 0) {
            char *body = get_body(client_socket, buffer, buffer_size, bytes); 
            handle_move(client_socket, body);
        } else if (strncmp(buffer, post_end, strlen(post_end)) == 0) {
            handle_end(client_socket);
        }
        close(client_socket);
    }
}
