package positions;

import java.sql.ResultSet;
import java.util.HashMap;

import http.MyHttpClient;
import json.MyJsonAr;
import json.MyJsonObj;
import tw.util.S;

/** This is a client that will query Moralis for the token balances for each wallet
 *  and then compares each wallet/token to the value from the Moralis position server. */

public class BalanceClient {
	public static void main(String[] args) {
		try {
			new BalanceClient().run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	HashMap<String, Stock> m_map;
	
	private void run() throws Exception {
		MoralisServer.m_database.connect( "jdbc:postgresql://localhost:5432/reflection", "postgres", "1359");
		
		m_map = EventFetcher.readStocks();
		
		ResultSet res = MoralisServer.m_database.query( "select distinct wallet from events");
		while (res.next() ) {
			query( res.getString(1) );
		}
	}
	
	static String chain = MoralisServer.chain;
	
	void query( String wallet) throws Exception {
		S.out( "querying wallet %s", wallet);
		
		boolean someError = false;
		
		// query moralis server
	    String url = String.format( "%s/%s/erc20?chain=%s", MoralisServer.moralis, wallet, chain);
	    String text = MoralisServer.querySync( url);
		MyJsonAr morPositions = MyJsonAr.parse(text);
		
	    // query my position server
		MyHttpClient cli = new MyHttpClient("localhost", 9393);
		cli.get( "wallet?wallet=" + wallet);
		MyJsonObj myWallet = cli.readMyJsonObject();

		for (MyJsonObj item : morPositions) {
			String token = item.getString("token_address").toLowerCase();
			if (m_map.containsKey(token) ) {
				String name = item.getString("name");
			    String symbol = item.getString("symbol");
			    double morBalance = item.getDouble("balance") / 1000000.;
				double myBalance = myWallet.getDouble( token);
				
				if (match( morBalance, myBalance) ) {
					S.out( "OK %s %s %s", wallet, token, morBalance);
				}
				else {
					S.out( "ERROR: mismatch for %s %s %s %s", wallet, token, morBalance, myBalance);
					someError = true;
				}
			}
		}
		S.out( someError ? "There was one or more errors" : "All positions match");
	}

	/** Return true if we match to within six decimal places. */
	private boolean match(double morBalance, double myBalance) {
		return Math.abs(morBalance - myBalance) < .000001;
	}
	
}
