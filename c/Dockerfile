FROM gcc:14
RUN mkdir /app
WORKDIR /app
COPY snake.c .
RUN gcc -o snake snake.c
CMD ["./snake"]
