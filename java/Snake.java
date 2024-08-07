import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.StringBuilder;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class Snake {

    private String getField(String json, String name) {
        String needle = '"' + name + '"';
        return json.substring(json.indexOf(needle) + needle.length() + 1);
    }

    private String getBalanced(String json, String name, char open, 
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

    private String getObject(String json, String name) { 
        return getBalanced(json, name, '{', '}');
    }

    private String getArray(String json, String name) { 
        return getBalanced(json, name, '[', ']');
    }

    private int getNumber(String json, String name) {
        String start = getField(json, name);
        String numberChars = "";
        int idx = 0;
        while (Character.isDigit(start.charAt(idx))) {
            numberChars += start.charAt(idx);
            idx++;
        }
        return Integer.parseInt(numberChars);
    }

    private String getText(String json, String name) {
        String start = getField(json, name);
        String result = "";
        int idx = 1;
        while (start.charAt(idx) != '"') {
            result += start.charAt(idx);
            idx++;
        }
        return result;
    }

    private String getBody(HttpExchange exchange) throws IOException {
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

    static class Coordinate {
        public final int x;
        public final int y;

        public Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public boolean equals(Object that) {
            return this.x == ((Coordinate)that).x && 
                this.y == ((Coordinate)that).y;
        }

        public final int hashCode() {
            return this.x + this.y;
        }
    }

    private Set<Coordinate> getCoordinates(String json) {
        Set<Coordinate> result = new HashSet<Coordinate>();
        Pattern p = Pattern.compile("[{]\"x\":(\\d+),\"y\":(\\d+)[}]");
        Matcher m = p.matcher(json);
        while (m.find()) {
            result.add(new Coordinate(Integer.parseInt(m.group(1)),
                Integer.parseInt(m.group(2))));
        }
        return result;
    }

    private Coordinate nearestFood(String board, Coordinate head) {
        String foodJson = getArray(board, "food");
        Set<Coordinate> food = getCoordinates(foodJson);
        double distance = Double.MAX_VALUE;
        int x = 255, y = 255;
        for (Coordinate f: food) {
            double d = Math.sqrt(Math.pow(head.x - f.x, 2) +
                    Math.pow(head.y - f.y, 2));
            if (d < distance) {
                distance = d;
                x = f.x;
                y = f.y;
            }
        }
        return new Coordinate(x, y);
    }

    private String[] getPreferredDirections(String board, Coordinate head) {
        Coordinate food = nearestFood(board, head);
        if (head.x != food.x) {
            if (head.x < food.x) {
                return new String[]{"right", "up", "down", "left"};
            } else {
                return new String[]{"left", "up", "down", "right"};
            }
        }
        if (head.y < food.y) {
            return new String[]{"up", "left", "right", "down"};
        } else {
            return new String[]{"down", "left", "right", "up"};
        }
    }

    private boolean freeCell(String board, Coordinate c) {
        int width = getNumber(board, "width");
        int height = getNumber(board, "height");
        if (c.x < 0 || c.y < 0 || c.x >= width || c.y >= height) {
            return false;
        }
        String snakes = getArray(board, "snakes");
        Set<Coordinate> snakeBodies = getCoordinates(snakes);
        return !snakeBodies.contains(c);
    }

    private String selectDirection(String board, Coordinate head, 
            String directions[]) {
        for (String direction: directions) {
            if (direction == "left" && freeCell(board, 
                    new Coordinate(head.x - 1, head.y))) {
                return "left";
            }
            if (direction == "right" && freeCell(board, 
                    new Coordinate(head.x + 1, head.y))) {
                return "right";
            }
            if (direction == "down" && freeCell(board, 
                    new Coordinate(head.x, head.y - 1))) {
                return "down";
            }
            if (direction == "up" && freeCell(board, 
                    new Coordinate(head.x, head.y + 1))) {
                return "up";
            }
        }
        System.out.println("Oops");
        return "left";
    }

    private String getDirection(String board, Coordinate head) {
        String directions[] = getPreferredDirections(board, head);
        return selectDirection(board, head, directions);
    }

    private void sendResponse(HttpExchange exchange, int status, String body) 
            throws IOException {
            exchange.sendResponseHeaders(status, body.length());
            OutputStream os = exchange.getResponseBody();
            os.write(body.getBytes());
            os.close();
    }

    private HttpHandler metaDataHandler = (HttpExchange exchange) -> {
            String metadata = "{\"apiversion\": \"1\", " +
                "\"author\": \"'robvanderleek\", \"version\": \"1.0\", " +
                "\"color\": \"#b07219\", \"head\": \"safe\", " +
                "\"tail\": \"sharp\"}";
            sendResponse(exchange, 200, metadata);
    };

    private HttpHandler startHandler = (HttpExchange exchange) -> {
            String json = this.getBody(exchange);
            String game = this.getObject(json, "game");
            String id = this.getText(game, "id");
            System.out.println(String.format("Game started: %s", id));
            sendResponse(exchange, 200, "");
    };

    private HttpHandler endHandler = (HttpExchange exchange) -> {
            sendResponse(exchange, 200, "");
    };

    private HttpHandler moveHandler = (HttpExchange exchange) -> {
            String json = getBody(exchange);
            int turn = getNumber(json, "turn");
            System.out.println(String.format("Turn: %d", turn));
            String headJson = getObject(getObject(json, "you"), "head");
            Coordinate head = new Coordinate(getNumber(headJson, "x"),
                getNumber(headJson, "y"));
            String board = getObject(json, "board");
            String direction = getDirection(board, head);
            String response = String.format("{\"move\": \"%s\"}", direction);
            sendResponse(exchange, 200, response);
    };

    private void startServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", metaDataHandler);
        server.createContext("/start", startHandler);
        server.createContext("/move", moveHandler);
        server.createContext("/end", endHandler);
        server.setExecutor(null);
        server.start();
        System.out.println(
            String.format("Starting Battlesnake server on port: %d", port));
    }

    public static void main(String args[]) throws IOException {
        int port = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "3000"));
        new Snake().startServer(port);
    }

}
