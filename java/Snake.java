import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class Snake {

    static class BattleSnakeHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"apiversion\": \"1\", " +
                "\"author\": \"'robvanderleek\", \"version\": \"1.0\", " +
                "\"color\": \"#b07219\", \"head\": \"safe\", " +
                "\"tail\": \"sharp\"}";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public static void main(String args[]) throws IOException {
        int port = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "3000"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new BattleSnakeHandler());
        server.setExecutor(null);
        server.start();
        System.out.println(
            String.format("Starting Battlesnake server on port: %d", port));
    }

}
