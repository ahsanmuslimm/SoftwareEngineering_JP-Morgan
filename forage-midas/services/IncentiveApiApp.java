import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

/**
 * Simple Incentive API Server for Midas Core testing.
 * 
 * Provides POST /incentive endpoint that returns a fixed incentive amount.
 * This is a minimal implementation for testing purposes only.
 */
public class IncentiveApiApp {
    
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/incentive", new IncentiveHandler());
        server.setExecutor(null);
        
        System.out.println("Incentive API Server started on http://localhost:8080");
        System.out.println("Endpoint: POST /incentive");
        server.start();
    }
    
    static class IncentiveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Return a fixed incentive amount for testing
                String response = "{\"amount\": 15.0}";
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
                
                System.out.println("Incentive API called - returned: " + response);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
}