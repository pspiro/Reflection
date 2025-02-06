package http;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.sun.net.httpserver.HttpServer;

import tw.util.S;

/** User this one to create multipe endpoints or no endpoints.
 * See also SimpleTransaction.listen() */
public class MyServer {
	public static void listen(int port, int threads, Consumer<HttpServer> adder) {
		try {
			S.out( "Listening on 0.0.0.0:%s  (%s threads)", port, threads);
			HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
			server.createContext("/favicon", exch -> {} ); // return something here so you don't tie up the thread
			adder.accept(server);
			server.setExecutor( Executors.newFixedThreadPool(threads) );
			server.start();
		}
		catch( BindException e) {
			S.out( "The application is already running");
			e.printStackTrace();
			System.exit(1);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(2);  // we need this because listening on the port will keep the app alive
		}
	}		
}
