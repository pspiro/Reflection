package http;


import org.json.simple.parser.JSONParser;

import tw.util.IStream;
import tw.util.S;

public class MyHttpServer {

	// this is so fucking weird. it works when run from command prompt, but
	// when run from eclipse you can't connect from browser using external ip 
	// http://69.119.189.87  but you can use 192.168.1.11; from dos prompt you
	// can use either. pas
	public static void main(String[] args) throws Exception {
		String host = args[0];
		int port = Integer.valueOf( args[1]);
		S.out( "listening on %s:%s", host, port);

		SimpleTransaction.listen( host, port, transaction -> {
			try {
				MyJsonObj msg = transaction.getJson();

				for (MyJsonObj transfer : msg.getAr( "erc20Transfers") ) {
		        	String from = transfer.getStr( "from");
		        	String to = transfer.getStr( "to");
		        	double val = transfer.getDouble( "valueWithDecimals");
		        	S.out( "%s %s %s", from, to, val);  // formats w/ two dec.
		        }
				
				transaction.respond( "OK");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	
	
	private static void main2() throws Exception {
		S.out( Double.valueOf( "500.127108"));
		IStream is = new IStream("c:\\temp\\f.t");
		String str = is.readln();
        MyJsonObj msg = new MyJsonObj( new JSONParser().parse(str) );
        msg.displ();
        
        for (MyJsonObj log : msg.getAr( "logs") ) {
        	String top1 = log.getStr( "topic1");
        	String top2 = log.getStr( "topic2");
        	String data = log.getStr( "data");
        	S.out( "%s %s %s", top1, top2, data);
        }
        
        
	}

	private static String strip(String str) {
		int i = 0;
		while (str.charAt( i) == '0') i++;
		return str.substring( i);
	}


	static String tab(int level) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < level * 3; i++) {
			sb.append( " ");
		}
		return sb.toString();
	}
	
	
}
