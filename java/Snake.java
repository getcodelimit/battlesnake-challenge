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

    private static String getField(String json, String name) {
        String needle = '"' + name + '"';
        return json.substring(json.indexOf(needle) + needle.length() + 1);
    }

    private static String getBalanced(String json, String name, char open, 
            char close) {
        String start = getField(json, name);
        int idx = 0, indent = 0;
        do {
            if (start.charAt(idx) == open) {
                indent++;
            } else if (start.charAt(idx) == close) {
                indent--;
            }
            idx++;
        } while (indent > 0);
        return start.substring(0, idx);
    }

    private static String getObject(String json, String name) { 
        return getBalanced(json, name, '{', '}');
    }

    private static String getArray(String json, String name) { 
        return getBalanced(json, name, '[', ']');
    }

    private static int getNumber(String json, String name) {
        String start = getField(json, name);
        String numberChars = "";
        int idx = 0;
        while (Character.isDigit(start.charAt(idx))) {
            numberChars += start.charAt(idx);
            idx++;
        }
        return Integer.parseInt(numberChars);
    }

    private static String getText(String json, String name) {
        String start = getField(json, name);
        String result = "";
        int idx = 1;
        while (start.charAt(idx) != '"') {
            result += start.charAt(idx);
            idx++;
        }
        return result;
    }

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

    static String[] getPreferredDirections(String board, int headX, int headY) {
        return new String[]{"left", "right", "down", "up"};
    }

    static boolean freeCell(String board, int x, int y) {
        return true;
    }

    static String selectDirection(String board, int headX, int headY, 
            String directions[]) {
        for (String direction: directions) {
            if (direction == "left" && freeCell(board, headX - 1, headY)) {
                return "left";
            }
            if (direction == "right" && freeCell(board, headX + 1, headY)) {
                return "right";
            }
            if (direction == "down" && freeCell(board, headX, headY - 1)) {
                return "down";
            }
            if (direction == "up" && freeCell(board, headX, headY + 1)) {
                return "up";
            }
        }
        System.out.println("Oops");
        return "left";
    }

    static String getDirection(String board, int headX, int headY) {
        String directions[] = getPreferredDirections(board, headX, headY);
        return selectDirection(board, headX, headY, directions);
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
            String json = getBody(exchange);
            String game = getObject(json, "game");
            String id = getText(game, "id");
            System.out.println(String.format("Game started: %s", id));
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
    };

    static HttpHandler moveHandler = (HttpExchange exchange) -> {
            String json = getBody(exchange);
            int turn = getNumber(json, "turn");
            System.out.println(String.format("Turn: %d", turn));
            String head = getObject(getObject(json, "you"), "head");
            int headX = getNumber(head, "x");
            int headY = getNumber(head, "y");
            String board = getObject(json, "board");
            String direction = getDirection(board, headX, headY);
            String response = String.format("{\"move\": \"%s\"}", direction);
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
    };

    public static void main(String args[]) throws IOException {
        int port = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "3000"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", metaDataHandler);
        server.createContext("/start", startHandler);
        server.createContext("/move", moveHandler);
        server.setExecutor(null);
        server.start();
        System.out.println(
            String.format("Starting Battlesnake server on port: %d", port));
    }

}
