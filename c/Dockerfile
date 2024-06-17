FROM gcc:14
RUN mkdir /app
WORKDIR /app
COPY snake.c .
RUN gcc -lm -o snake snake.c
CMD ["./snake"]
