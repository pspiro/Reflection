package http;


import java.net.BindException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import reflection.ParamMap;
import tw.util.S;

public class MyHttpServer implements HttpHandler {
	
	public static void main(String[] args) {
		try {
			int port = 8484; //Integer.valueOf( System.getenv("PORT") );
			Transaction.listen( new MyHttpServer(), port);
		}
		catch( BindException e) {
			S.out( "The application is already running");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override public synchronized void handle(HttpExchange exch) {
		try {
			handle( exch, Transaction.getMap( exch) );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	private void handle(HttpExchange exch, ParamMap map) {
		Transaction.respond( exch, map.toString() );
	}
	
		
}
