package http;

import tw.util.S;

public class EchoServer {
	public static void main(String[] args) {
		String host = args[0];
		if (S.isNull( host) ) {
			S.out( "usage: EchoServer host port");
			return;
		}

		int port = Integer.valueOf( args[1]);
		
		S.out( "listening on %s:%s", host, port);
		
		SimpleTransaction.listen( host, port, simpleTrans -> {
			try {
				simpleTrans.showAll();
				
				simpleTrans.respond( "ok");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
