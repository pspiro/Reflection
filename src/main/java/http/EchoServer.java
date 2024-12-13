package http;

import tw.util.S;

public class EchoServer {
	public static void main(String[] args) {
		String host = "0.0.0.0";
		//int port = Integer.valueOf( args[0]);
		int port = 8201;
		
		S.out( "listening on %s:%s", host, port);
		
		SimpleTransaction.listen( host, port, trans -> {
			try {
				trans.showAll();

				String str = trans.exchange().getRequestMethod() + trans.exchange().getRequestURI();
				
				trans.respond( str);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
