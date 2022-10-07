package http;

import tw.util.S;

public class EchoServer {
	public static void main(String[] args) {
		String host = args[0];
		int port = Integer.valueOf( args[1]);
		S.out( "listening on %s:%s", host, port);
		
		SimpleTransaction.listen( host, port, transaction -> {
			try {
				S.out( transaction.getRequest() );
				transaction.respond( "ok");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
