package positions;

import static positions.MoralisServer.chain;
import static positions.MoralisServer.moralis;
import static positions.MoralisServer.transferTopic;

import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.postgresql.util.PSQLException;

import common.Util;
import reflection.MySqlConnection;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;

/** This fetches the event log entries from Moralis. */
public class EventFetcher {
	static final char srcQuery = 'Q';
	static final char srcStream = 'S';
	static final char srcMarker = 'M';
	static final char srcParer = 'P';
	
	static int startingBlock = 7710000;
	static int limit = 500; // max per page, 500 is the highest allowed
	
	private final MoralisServer m_server; // not used? 
	private int m_responsesReceived;
	private final HashMap<String,MorStock> m_stockMap; // keys are in lower case
	private HashMap<String,Balances> m_walletMap = new HashMap<>(); // map wallet to token balances
	
	
	public EventFetcher(MoralisServer moralisServer) throws Exception {
		m_server = moralisServer;

		m_stockMap = readStocks();
	}
	
	/** Reads all stocks, active and inactive. */ 
	public static HashMap<String,MorStock> readStocks() throws Exception {
		S.out( "Reading contracts from google sheet");

		HashMap<String,MorStock> stockMap = new HashMap<String,MorStock>(); // map token to Stock
		
		ListEntry[] rows = NewSheet.getTab( NewSheet.Reflection, "Symbols").fetchRows(false);
		for (ListEntry row : rows) {
			String symbol = row.getString("Symbol");
			String token = row.getString( "TokenAddress").toLowerCase();
			int conid = row.getInt("Conid");
			
			if (S.isNotNull( symbol) ) {
				if (Util.validToken(token) ) { 
					MorStock stock = new MorStock( symbol, token, conid);
					stockMap.put( token, stock);
				}
				else {
					S.out( "WARNING: symbol %s has invalid token address", symbol);
				}
			}
		}
		
		return stockMap;
	}
	
	/** Query missing blocks one contract at a time. */
	void backfill(int startingBlock, int endingBlock) throws Exception {
		
		for (MorStock stock : m_stockMap.values() ) {
			String symbol = stock.symbol();
			String token = stock.token();
			
			S.out( "Backfilling %s from %s to %s", symbol, startingBlock, endingBlock);
			queryEvents( token, startingBlock, endingBlock, null);
		}
	}
	
	void queryEvents( String token, int start, int end, String cursor) throws Exception {
		String cursorStr = S.isNotNull( cursor) ? "&cursor=" + cursor : "";
		
		String url = String.format( "%s/%s/logs?chain=%s&from_block=%s&to_block=%s&topic0=%s&limit=%s%s",
				moralis, token, chain, start, end, transferTopic, limit, cursorStr);

		String response = MoralisServer.querySync( url);
		// process events returned to us from our query
		String curs = processEventLog( response, token);

		// query had more data that was not returned?
		if (S.isNotNull( curs) ) {
			queryEvents( token, start, end, curs);
		}
		else if (++m_responsesReceived == m_stockMap.size() ) {
			S.out( "  received last response");
		}
	}

	
	/** Map conid to balance */
	static class Balances extends JsonObject {
	}
	
	public synchronized void increment(String wallet, String token, double val) {
		Balances obj = Util.getOrCreate( m_walletMap, wallet, () -> new Balances() );
		obj.increment( token, val);
	}
	
	private String processEventLog( String json, String token) throws Exception {
		JsonObject obj = JsonObject.parse( json);
		
		JsonArray eventLogs = obj.getArray("result");
		for (JsonObject event : eventLogs) {
			int block = event.getInt( "block_number");
			String top0 = event.getString("topic0");
			
			if (top0.equals( transferTopic) ) {
				String from = PosUtil.formatWallet( event.getString("topic1") );
				String to = PosUtil.formatWallet( event.getString("topic2") );
				double amt = Util.hexToDec( event.getString( "data"), 6);
				String hash = event.getString("transaction_hash").toLowerCase();
			
				S.out( "Log: %s %s %s %s %s", block, token, from, to, amt);
				insert( MoralisServer.m_database, block, token, from, to, amt, hash, srcQuery);
			}
		}
		return obj.getString( "cursor");
	}
	
	/** Insert the from and to transactions into the database, and increment the counters */
	void insert( MySqlConnection db, int block, String token, String from, String to, double val, String hash, char source) throws Exception {
		if (val != 0. && !from.equals(to) ) { // we have to skip these because due to the index the first one would go but the second one would fail which would mess up the balances
			insert( db, block, token, from, -val, hash, source);
			insert( db, block, token, to, val, hash, source);
		}
	}
	
	void insert( MySqlConnection db, int block, String token, String wallet, double val, String hash, char source) throws Exception {
		try {
			if (val != 0 && !wallet.equals( PosUtil.nullWallet) ) {
				String sql = String.format( "insert into events values (%s,'%s','%s',%s,'%s','%s')", block, token, wallet, val, hash, source);
				db.execute(sql); 
	        	increment( wallet, token, val); // this won't get called for dup events
			}
		}
		catch( PSQLException e) {
			if ( Util.startsWith( e.getMessage(), "ERROR: duplicate key") ) {
				// S.out( "dup key, not inserted");  eat this, we don't care
			}
			else throw e;
		}
	}
	
	
	String getAllWalletsJson() { 
		throw new RuntimeException();
		//return m_walletMap.toString(); 
	} 	

	Balances getWalletBalances(String wallet) {
		return (Balances)m_walletMap.get( wallet);
	}
}
