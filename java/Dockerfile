FROM eclipse-temurin:17-jdk
RUN mkdir /app
WORKDIR /app
COPY Snake.java .
RUN javac Snake.java
CMD ["java", "Snake"]
