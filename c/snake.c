#include <netinet/in.h>
#include <stdio.h>
#include <sys/socket.h>

int main(int argc, char *argv) {
    int server_socket;
    struct sockaddr_in server_addr;
    server_socket = socket(AF_INET, SOCK_STREAM, 0);
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = INADDR_ANY;
    server_addr.sin_port = htons(3000);
}
