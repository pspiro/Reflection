package http;


import reflection.MySqlConnection;
import tw.util.OStream;
import tw.util.S;

public class MyHttpServer {

	// this is so fucking weird. it works when run from command prompt, but
	// when run from eclipse you can't connect from browser using external ip 
	// http://69.119.189.87  but you can use 192.168.1.11; from dos prompt you
	// can use either. pas
	static MySqlConnection m_database = new MySqlConnection();
	
	public static void main(String[] args) throws Exception {
		m_database.connect( "jdbc:postgresql://localhost:5432/reflection", "postgres", "1359");		

		
		
		String host = args[0];
		int port = Integer.valueOf( args[1]);
		S.out( "listening on %s:%s", host, port);
		
		OStream os = new OStream( "stream");

		SimpleTransaction.listen( host, port, transaction -> {
			try {
				MyJsonObj msg = transaction.getJson();
				msg.displ();

				S.out( "ERC20 Transfers");
				for (MyJsonObj transfer : msg.getAr( "erc20Transfers") ) {
		        	String from = transfer.getStr( "from");
		        	String to = transfer.getStr( "to");
		        	double val = transfer.getDouble( "valueWithDecimals");
		        	S.out( "%s %s %s", from, to, val);  // formats w/ two dec.

//		        	m_database.execute( String.format( "insert into events values (%s %s %s)", block, from, val) );
//					m_database.execute( String.format( "insert into events values (%s %s %s)", block, to, val) );
		        }
				
				
//				S.out( "");
//				S.out( "Logs");
//		        for (MyJsonObj log : msg.getAr( "logs") ) {
//		        	String top1 = log.getStr( "topic1");
//		        	String top2 = log.getStr( "topic2");
//		        	String data = log.getStr( "data");
//		        	S.out( "%s %s %s", top1, top2, data);
//		        }
				
				transaction.respond( "OK");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
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
