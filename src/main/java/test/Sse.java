package test;



import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import common.Util;
import http.BaseTransaction;
import http.MyServer;

public class Sse {
	public static void main(String[] args) throws IOException {
		//SimpleTransaction.listen("0.0.0.0", 8181, trans -> process( trans) );
		
		MyServer.listen( 8181, 10, server -> {
			server.createContext("/", exch -> process2( exch) );
	        server.createContext("/sse", new SSEHandler());
		});
	}

	private static void process2(HttpExchange exch) {
		BaseTransaction trans = new BaseTransaction(exch, true);
		trans.respond(Util.toJson( "msg", "hello"));
	}

	static int i = 1;
	
    static class SSEHandler implements HttpHandler {
        private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        @Override public void handle(HttpExchange exchange) throws IOException {
        	int n = new Random().nextInt();
        	
            // Set the response headers for SSE
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");

            // Send the response headers with a response code of 200 (OK)
            exchange.sendResponseHeaders(200, 0);

            // Get the output stream to send SSE data
            OutputStream outputStream = exchange.getResponseBody();

            // Schedule a task to send SSE events every 5 seconds
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    // Write a server-sent event message
                    String message = "data: " + "Hello from native Java SSE Server! " + i++ + " " + n + "\n\n";
                    outputStream.write(message.getBytes());
                    outputStream.flush();  // Ensure the message is sent immediately
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, 0, 3, TimeUnit.SECONDS);

            // Keep the connection alive indefinitely until the client disconnects
        }
    }
}
