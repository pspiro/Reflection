package test;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import http.BaseTransaction;
import http.MyServer;

public class TestSecret {
	public static void main(String[] args) throws Exception {
		MyServer.listen( 3333, 3, server -> {
			server.createContext("/test", exch -> new Trans(exch).test() );
		});
	}

	static class Trans extends BaseTransaction {
		public Trans(HttpExchange exch) {
			super( exch, true);
		}

		void test() {
			wrap( () -> {
				String name = Util.getLastToken(
						m_exchange.getRequestURI().toString().toLowerCase(), 
						"/");

				String val = System.getenv(name);
				respond( Util.toJson( "val", val) );
			});
		}
	}
}
