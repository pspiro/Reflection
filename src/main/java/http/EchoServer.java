package http;

import java.text.SimpleDateFormat;
import java.util.Date;

import tw.util.S;

public class EchoServer {
	private static SimpleDateFormat timeFmt = new SimpleDateFormat( "HH:mm:ss.SSS");
	
	public static void main(String[] args) {
		String host = args[0];
		int port = Integer.valueOf( args[1]);
		S.out( "listening on %s:%s", host, port);
		
		SimpleTransaction.listen( host, port, transaction -> {
			try {
				String body = transaction.getRequest();  // we have to call getRequest() for all requests

				if (transaction.getHeaders().size() > 0) {
					S.out( "Headers:");
					S.out( transaction.getHeaders() );
				}
				
				if (transaction.isPost() ) {
					S.out( "Body:");
					S.out( body);
				}
				
				transaction.respond( timeFmt.format( new Date() ) + " OK");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
