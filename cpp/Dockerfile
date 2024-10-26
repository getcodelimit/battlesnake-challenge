FROM frolvlad/alpine-gxx
RUN mkdir /app
WORKDIR /app
COPY snake.cpp .
RUN g++ -o snake snake.cpp
CMD ["./snake"]
