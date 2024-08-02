import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.StringBuilder;
import java.net.InetSocketAddress;

public class Snake {

    private static String getBody(HttpExchange exchange) throws IOException {
        InputStreamReader isr = 
            new InputStreamReader(exchange.getRequestBody(), "utf-8");
        BufferedReader br = new BufferedReader(isr);
        int b;
        StringBuilder buf = new StringBuilder();
        while ((b = br.read()) != -1) {
            buf.append((char) b);
        }
        br.close();
        isr.close();
        return buf.toString();
    }

    static HttpHandler metaDataHandler = (HttpExchange exchange) -> {
            String response = "{\"apiversion\": \"1\", " +
                "\"author\": \"'robvanderleek\", \"version\": \"1.0\", " +
                "\"color\": \"#b07219\", \"head\": \"safe\", " +
                "\"tail\": \"sharp\"}";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
    };

    static HttpHandler startHandler = (HttpExchange exchange) -> {
            System.out.println("START!");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
    };

    public static void main(String args[]) throws IOException {
        int port = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "3000"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", metaDataHandler);
        server.createContext("/start", startHandler);
        server.setExecutor(null);
        server.start();
        System.out.println(
            String.format("Starting Battlesnake server on port: %d", port));
    }

}
