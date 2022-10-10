package http;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.HashSet;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;

import http.MyJsonObj.MyJsonAr;
import reflection.MySqlConnection;
import reflection.Util;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;

public class LogClient {
	static String transferTopic = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

	static MySqlConnection m_database = new MySqlConnection();
	// empty block 15704618
	
	// this should be before any of the contracts started; we could change to use per-contract
	static int startingBlock = 7710000;
	static int endingBlock   = 9999999; // this won't last very long, pass a higher number. pas
	static int limit = 500; // max per page, you can increase this. pas
	static String chain = "goerli";  // or eth
	static int counter = 0;
	static boolean doneSending;  // i think we don't need this; test w/ large block and small page size
	
	// each contract has its own starting point which is the block on which it deployed
	
	// TODO
	// add transaction has to db and make a unique key on it
	// * test it with a very small page size
	// * fill in the blanks so it doesn't send queries from blocks w/ no entries every time
	
	// this query is very slow; why is that?
	// you can query by date; that would be better, then you only need to know the start date
	// see if you can set up the database to make them all lower case. pas
	

	public static void main(String[] args) throws Exception {
		try {
			m_database.connect( "jdbc:postgresql://localhost:5432/reflection", "postgres", "1359");
			
			int i = 0;
			Tab tab = NewSheet.getTab( NewSheet.Reflection, "Symbols");
			for (ListEntry row : tab.fetchRows(false) ) {
				String symbol = row.getValue("Symbol");
				String token = row.getValue( "SmartContractID").toLowerCase();
				
				if (S.isNotNull( symbol) ) {
					if (Util.validToken( token) ) {
						backfill( symbol, token);
					}
					else {
						S.out( "WARNING: symbol %s has invalid token address", symbol);
					}
				}
			}
			S.out( "Done sending queries");
			doneSending = true;
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void backfill(String symbol, String token) throws Exception {
		S.out( "Backfilling %s %s", symbol, token);
		S.out( counter++);

		// you should delete the last block first in case it was partial. pas
		
		S.out( "  querying database for highest block");
		ResultSet res = m_database.query( "select max(block) from events where token = '%s'", token);
		
		int start = res.next() && res.getInt(1) > 0 ? res.getInt(1) : startingBlock;  // note that we re-query the last block in case it was partial
		query( token, start, endingBlock);
		
		// NOTE: even though the response is handled in another thread,
		// this thread waits for the response to be processed, although I
		// don't know if it would wait if the initial response has a cursor
	}

	/** Maybe save this for later. */
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

	
	static void query( String token, int start, int end) throws IOException {
		S.out( "  querying Moralis from %s to %s", start, end);
		query( token, start, end, null);
	}
	
	static void query( String token, int start, int end, String cursor) throws IOException {
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
		
		S.out( "   sending " + url);
		
		client.prepare("GET", url) 
		  .setHeader("accept", "application/json")
		  .setHeader("X-API-Key", "2R22sWjGOcHf2AvLPq71lg8UNuRbcF8gJuEX7TpEiv2YZMXAw4QL12rDRZGC9Be6")
		  .execute()
		  .toCompletableFuture()
		  .thenAccept( obj -> {
			  String curs = processJson( obj.getResponseBody(), token, blocks);
			  if (S.isNotNull( curs) ) {
				  try {
					  query( token, start, end, curs);
				  } catch (IOException e) {
					  e.printStackTrace();
				  }
			  }
			  else {
				  S.out( "counter %s", counter);
				  if (--counter == 0 && doneSending) {
					  S.out( "Done receiving");
				  }
				  //createNullEntriesIfNeeded( token, blocks);
			  }
		  })
		  .join();

		client.close();
	}

	private static String processJson( String json, String token, HashSet<Integer> blocks) {
		try {
			MyJsonObj obj = MyJsonObj.parse( json);
			//obj.displ();
			
			MyJsonAr eventLogs = obj.getAr("result");
			for (MyJsonObj event : eventLogs) {
				int block = event.getInt( "block_number");
				String top0 = event.getString("topic0");
				
				if (top0.equals( transferTopic) ) {
					String from = formatWallet( event.getString("topic1") );
					String to = formatWallet( event.getString("topic2") );
					double amt = Util.hexToDec( event.getString( "data"), 6);
					String hash = event.getString("transaction_hash").toLowerCase();
				
					S.out( "%s %s %s %s %s", block, token, from, to, amt);
					MoralisServer.insert( m_database, block, token, from, to, amt, hash);
					blocks.remove( block);
				}
			}
			return obj.getString( "cursor");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/** Return a string of length 42 starting with 0x */
	private static String formatWallet(String str) {
		return "0x" + S.right( str, 40).toLowerCase();
	}

	/** Create null database entries for each block. */
	private static void createNullEntriesIfNeeded(String token, HashSet<Integer> blocks) {
		try {
			for (Integer block : blocks) {
				S.out( "Inserting null entry for block %s", block);
				//MyHttpServer.insert( m_database, block, "0x0", 0); // don't enter zeros because those get filtered
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}

	// # select wallet, token, sum(quantity) from events group by wallet, token;