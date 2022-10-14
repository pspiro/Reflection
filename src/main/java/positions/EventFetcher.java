package positions;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.json.simple.JSONObject;
import org.postgresql.util.PSQLException;

import http.MyJsonObj;
import http.MyJsonObj.MyJsonAr;
import reflection.Main;
import reflection.MySqlConnection;
import reflection.RefCode;
import reflection.Util;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;

/** This fetches the event log entries from Moralis. */
public class EventFetcher {
	static String transferTopic = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
	static String chain = "goerli";  // or eth
	static int startingBlock = 7710000;
	static int endingBlock   = 9999999; // this won't last very long, pass a higher number. pas
	static int limit = 500; // max per page, you can increase this. pas
	
	private final MoralisServer m_server; // not used? 
	private final HashMap<String,Stock> m_stockMap;
	private int m_responsesReceived;
	private TypedJson<Balances> m_walletMap = new TypedJson<Balances>(); // map wallet to token balances
	
	
	public EventFetcher(MoralisServer moralisServer) throws Exception {
		m_server = moralisServer;

		m_stockMap = readStocks();
	}
	
	/** Reads all stocks, active and inactive. */ 
	public static HashMap<String,Stock> readStocks() throws Exception {
		S.out( "Reading contracts from google sheet");

		HashMap<String,Stock> stockMap = new HashMap<String,Stock>(); // map token to Stock
		
		ListEntry[] rows = NewSheet.getTab( NewSheet.Reflection, "Symbols").fetchRows(false);
		for (ListEntry row : rows) {
			String symbol = row.getValue("Symbol");
			String token = row.getValue( "TokenAddress").toLowerCase();
			int conid = row.getInt("Conid");
			
			if (S.isNotNull( symbol) ) {
				if (Util.validToken(token) ) { 
					Stock stock = new Stock( symbol, token, conid);
					stockMap.put( token, stock);
				}
				else {
					S.out( "WARNING: symbol %s has invalid token address", symbol);
				}
			}
		}
		
		return stockMap;
	}
	
	void backfill(int startingBlock) throws Exception {
		for (Stock stock : m_stockMap.values() ) {
			String symbol = stock.symbol();
			String token = stock.token();
			
			S.out( "Backfilling token %s", symbol);
			queryEvents( token, startingBlock, endingBlock, null);
		}
	}
	
	private void backfill(String symbol, String token) throws Exception {
		S.out( "Backfilling %s %s", symbol, token);

		// i'm not clear if we need to do this query for each token or not. pas 
		S.out( "  querying database for highest block");
		ResultSet res = MoralisServer.m_database.query( "select max(block) from events where token = '%s'", token);
		
		int start = res.next() && res.getInt(1) > 0 ? res.getInt(1) : startingBlock;  // note that we re-query the last block in case it was partial
		queryEvents( token, start, endingBlock, null);
	}
	
	void queryEvents( String token, int start, int end, String cursor) throws IOException {
		String cursorStr = S.isNotNull( cursor) ? "&cursor=" + cursor : "";
		
		// create a set of all blocks in this query; the blocks will get removed
		// as database entries are added for each blocks; any remaining blocks
		// have no database entries so we would create a null entry
		HashSet<Integer> blocks = new HashSet<Integer>();  // we're not actually doing this, probably no need. pas
		for (int i = start; i <= end; i++) {
			blocks.add( i);
		}
		
		AsyncHttpClient client = new DefaultAsyncHttpClient();
		String url = String.format( "https://deep-index.moralis.io/api/v2/%s/logs?chain=%s&from_block=%s&to_block=%s&topic0=%s&limit=%s%s",
				token, chain, start, end, transferTopic, limit, cursorStr);
		
		S.out( "  querying Moralis " + url);
		
		client.prepare("GET", url)
		  	.setHeader("accept", "application/json")
		  	.setHeader("X-API-Key", "2R22sWjGOcHf2AvLPq71lg8UNuRbcF8gJuEX7TpEiv2YZMXAw4QL12rDRZGC9Be6")
		  	.execute()
		  	.toCompletableFuture()
		  	.thenAccept( obj -> {
		  		try {
		  			client.close();

		  			// process events returned to us from our query
		  			S.out( "  processing %s", token.substring(0,5) );
		  			String curs = processJson( obj.getResponseBody(), token, blocks);

		  			// query had more data that was not returned?
		  			if (S.isNotNull( curs) ) {
		  				queryEvents( token, start, end, curs);
		  			}
		  			else if (++m_responsesReceived == m_stockMap.size() ) {
		  				S.out( "Received last response");
		  				incrementCountersFromDatabase();
		  				S.out( "Ready to receive queries");
		  			}
		  		}
		  		catch (Exception e) {
		  			e.printStackTrace();
		  		}
		  	}).join(); // add join here to make it syncronous
	}

	
	static class TypedJson<T> extends JSONObject { // use this everywhere. pas
		@Override public T get(Object key) {
			return (T)super.get(key);
		}
		
		@SuppressWarnings("unchecked")
		void putt( String tag, T value) {
			put( tag, value);
		}
	}
	
	/** Map token to balance */
	static class Balances extends TypedJson<Double> {
		double getDouble(String token) {
			Double val = get(token);
			return val != null ? val : 0;
		}
		
		void increment( String token, double val) {
			putt( token, getDouble( token) + val);
		}
	}
	
	private EventFetcher incrementCountersFromDatabase() throws Exception { // must be synchronized with the insert methods. pas
		S.out( "Incrementing wallets from database");
		// move this query into the database, I think you can do that
		String sql = "select wallet, token, sum(quantity) from events group by wallet, token";
		ResultSet res = MoralisServer.m_database.query( sql);
		while( res.next() ) {
			String wallet = res.getString(1);
			String token = res.getString(2);
			double val = res.getDouble(3);
			
			Stock stock = m_stockMap.get( token);
			Main.require( stock != null, RefCode.UNKNOWN, "stock not found for %s", token); // add checks for ALL nulls. pas

			increment( wallet, token, val);
		}
		return this;
	}
	
	public synchronized void increment(String wallet, String token, double val) {
		Balances walletMap = getOrCreateBalances( wallet);
		walletMap.increment( token, val);
	}
	
	private synchronized Balances getOrCreateBalances(String wallet) {
		Balances balances = m_walletMap.get( wallet);
		if (balances == null) {
			balances = new Balances();
			m_walletMap.put( wallet, balances);
		}
		return balances;
	}

	private String processJson( String json, String token, HashSet<Integer> blocks) {
		try {
			MyJsonObj obj = MyJsonObj.parse( json);
			//obj.display();
			
			MyJsonAr eventLogs = obj.getAr("result");
			for (MyJsonObj event : eventLogs) {
				int block = event.getInt( "block_number");
				String top0 = event.getString("topic0");
				
				if (top0.equals( transferTopic) ) {
					String from = PosUtil.formatWallet( event.getString("topic1") );
					String to = PosUtil.formatWallet( event.getString("topic2") );
					double amt = Util.hexToDec( event.getString( "data"), 6);
					String hash = event.getString("transaction_hash").toLowerCase();
				
					S.out( "%s %s %s %s %s", block, token, from, to, amt);
					insert( MoralisServer.m_database, block, token, from, to, amt, hash);
					blocks.remove( block);
				}
			}
			return obj.getString( "cursor");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/** Insert the from and to transactions into the database, and increment the counters */
	void insert( MySqlConnection db, int block, String token, String from, String to, double val, String hash) throws Exception {
		insert( db, block, token, from, -val, hash);
		insert( db, block, token, to, val, hash);
	}
	
	void insert( MySqlConnection db, int block, String token, String wallet, double val, String hash) throws Exception {
		try {
			if (val != 0 && !wallet.equals( PosUtil.nullWallet) ) {
				db.execute( String.format( "insert into events values (%s,'%s','%s',%s,'%s')", block, token, wallet, val, hash) );
	        	increment( wallet, token, val); // this won't get called for dup events
			}
		}
		catch( PSQLException e) {
			if ( Util.startsWith( e.getMessage(), "ERROR: duplicate key") ) {
				S.out( "Duplicate key inserting into events");
			}
			else throw e;
		}
	}
	

	
	String getAllWalletsJson() { 
		return m_walletMap.toString(); 
	} 	

	Balances getWalletBalances(String wallet) {
		return m_walletMap.get( wallet);
	}

	
}

// you should peridically query for the current balance and compare to what you have to check for mistakes
// in fact another possible and simpler approach might be to listen for events and just
// use that as a queue to then query for the balance since you would know both the
// wallet and the token. pas

//seriously, this will be much easier. but how to catch up after a crash? you would have to requery all balances. pas

/** Maybe save this for later. This code can fill in missing blocks. */
/*void rebuild() {
	
	S.out( "Reading database");
	ResultSet res = m_database.query( "select distinct block from events order by block");
	
	int last = 0;
	
	while (res.next() ) {
		int block = res.getInt( 1);
		
		if (last == 0) {
			if (block > startingBlock) {
				query( startingBlock, block - 1); // assume first block is full
			}
		}
		else {
			if (block > last + 1) {
				query( last + 1, block - 1);
			}
		}
		last = block;
	}
		
	// no entries in events table? query everything
	if (last == 0) {
		query( startingBlock, endingBlock);
	}
	else if (last < endingBlock) {
		query( last + 1, endingBlock);
	}
}*/
/*
void test() {
	Balances wallet1 = getOrCreateBalances("wallet1");
	wallet1.increment( "IBM", 3);
	wallet1.increment( "GE", 4);
	wallet1.increment( "IBM", 2);

	Balances wallet2 = getOrCreateBalances("wallet2");
	wallet2.increment( "IBM", 33);
	wallet2.increment( "GE", 44);
	wallet2.increment( "IBM", 22);
	
	Balances wallet3 = getOrCreateBalances("wallet1");
	wallet3.increment( "IBM", 3);
	wallet3.increment( "GE", 4);
	wallet3.increment( "IBM", 2);

	MyJsonObj.display( m_walletMap, 0);
}
*/
	// # select wallet, token, sum(quantity) from events group by wallet, token;
