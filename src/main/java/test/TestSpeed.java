package test;

import java.sql.ResultSet;

import org.asynchttpclient.DefaultAsyncHttpClient;

import common.Util.ObjectHolder;
import reflection.MySqlConnection;
import tw.util.S;

/** Compare db access to RefAPI access. */
public class TestSpeed {
	static String dbUrl = "jdbc:postgresql://34.86.193.58:5432/reflection";
	static String dbUser = "postgres";
	static String dbPassword = "1359";

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		MySqlConnection db = new MySqlConnection();
		db.connect(dbUrl, dbUser, dbPassword);
		
		for (int i = 0; i < 10; i++) {
			ResultSet ret = db.queryNext( "select * from events");
			S.out( " read " + ret.getString(3) );
		}
		S.out( "done");
		
	}
	
	
	public static void main2(String[] args) throws Exception {
		for (int i = 0; i < 10; i++) {
			S.out( "" + i);
			sendReq();
		}
		S.out( "done");  // 3 sec, 3+2  2+3
	}

	private static String sendReq() {
		ObjectHolder<String> holder = new ObjectHolder<String>();

		DefaultAsyncHttpClient client = new DefaultAsyncHttpClient(); 
		
		client.prepare("GET", "http://34.125.38.193:8383/?msg=getallstocks")
			.setHeader("accept", "application/json")
		  	.execute()
		  	.toCompletableFuture()
		  	.thenAccept( obj -> {
		  		try {
		  			client.close();
		  			holder.val = obj.getResponseBody();
		  		}
		  		catch (Exception e) {
		  			e.printStackTrace();
		  		}
		  	}).join();  // the .join() makes is synchronous

		return holder.val;				
	}
}
