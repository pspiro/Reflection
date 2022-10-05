package http;


import tw.util.S;

public class MyHttpServer {

	// this is so fucking weird. it works when run from command prompt, but
	// when run from eclipse you can't connect from browser using external ip 
	// http://69.119.189.87  but you can use 192.168.1.11; from dos prompt you
	// can use either. pas
	public static void main(String[] args) {
		String host = args[0];
		int port = Integer.valueOf( args[1]);
		S.out( "listening on %s:%s", host, port);

		SimpleTransaction.listen( host, port, trans -> {
			try {
				String str = trans.getRequest();
				S.out( str);
				trans.respond( "OK");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
