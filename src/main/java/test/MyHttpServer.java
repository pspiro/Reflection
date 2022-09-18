package test;


import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import javax.swing.JFrame;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import tw.util.S;

public class MyHttpServer implements HttpHandler {
	public static void main(String[] args) {
		try {
			JFrame f = new JFrame();
			f.setSize( 200, 200);
			f.setVisible(true);
			
			new MyHttpServer().run();
		}
		catch( BindException e) {
			S.out( "The application is already running");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void run() throws Exception {
		int port = Integer.valueOf( System.getenv("PORT") );

		// create HTTP server w/ single thread executor
		HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/", this); 
		server.setExecutor( Executors.newFixedThreadPool(5) );  // five threads but we are synchronized for single execution
		server.start();
	}

	/** Handle HTTP msg */
	@Override public synchronized void handle(HttpExchange exch) throws IOException {  // we could/should reduce the amount of synchronization, especially if there are messages that don't require the API
		
		try {
			if ("GET".equals(exch.getRequestMethod() ) ) {
			}
			else {
			}
			
			String response = "{ \"response\": \"ok\" }";

			OutputStream outputStream = exch.getResponseBody();
			exch.getResponseHeaders().add( "Content-Type", "application/json");

			exch.sendResponseHeaders(200, response.length());
			outputStream.write(response.getBytes());
			outputStream.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}

