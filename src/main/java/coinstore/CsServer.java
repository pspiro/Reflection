//package coinstore;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.HashSet;
//
//import org.json.simple.JsonArray;
//
//import com.ib.client.Order;
//
//import common.Util;
//import fireblocks.MyServer;
//import http.MyClient;
//import tw.util.S;
//
//public class CsServer {
//
//	static class Config {
//		int interval;
//		String mdServerBaseUrl;
//		
//		String mdServerBaseUrl() {
//			return mdServerBaseUrl;
//		}
//
//		int interval() {
//			return interval;
//		}
//	}
//
//	static Config m_config = new Config();
//	static HashMap<String,MM> set = new HashMap<>();
//	static HashMap<String,Prices> prices = new HashMap<>(); // let it be static so we keep old prices
//	
//	static class Prices {
//		double bid;
//		double ask;
//
//		Prices(double bid_, double ask_) {
//			bid = bid_;
//			ask = ask_;
//		}
//	}
//	
//	public static void main(String[] args) throws IOException {
//		MyServer.listen( 8385, 10, server -> {
//			//server.createContext("/csserver/status", exch -> new CsTransaction(this, exch).onStatus() ); 
//		});
//
//
//		set.add( new MM( "AAPL", 500) );
//		
//		Util.executeEvery(0, m_config.interval(), () -> check() );
//		
//	}
//	
//	static void check() {
//		// map symbol to prices
//		ar = MyClient.getArray( String.format("%s/mdserver/get-ref-prices") ).forEach( item -> {
//			prices.put( item.getString("symbol"), item);
//		});
//		
//		HashMap<String,Double> positions = Coinstore.getPositions("");
//		S.out( positions);
//		
//		set.forEach( mm -> {
//			mm.process( positions.get(mm.symbol()), prices.get(mm.symbol) );
//			
//	}
//	
//	void getPrices( ) {
//	}
//
//	HashSet<Order> orders = new HashSet<>();
//	
//	static class MM {
//		private String m_symbol;
//		HashSet<Order> orders = new HashSet<>();
//
//		MM(String symbol, int basePos) {
//			m_symbol = symbol;
//		}
//		
//		HashMap<Integer,Ord>
//		void check(double position, Prices prices) {
//			
//			
//		}
//			
//
//	}
//		
//}
