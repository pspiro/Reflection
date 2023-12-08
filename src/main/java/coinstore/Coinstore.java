//package coinstore;
//
//import static reflection.Main.m_config;
//
//import com.ib.client.Contract;
//import com.ib.client.Order;
//
//import common.Util;
//import fireblocks.MyServer;
//import http.MyClient;
//import reflection.Config;
//import reflection.Prices;
//import reflection.Stock;
//import reflection.Stocks;
//import reflection.TradingHours.Session;
//import tw.util.S;
//import util.LogType;
//
//public class Coinstore {
//	private static String m_mdsUrl = String.format( "http://localhost:%s/mdserver/get-ref-prices", m_config.mdsPort() );
//	private static Config m_config;
//	private static Stocks m_stocks;
//	
//	public static void main( String[] args) {
//		m_config = Config.ask();
//		
//		MyServer.listen( m_config.refApiPort(), m_config.threads(), server -> {
//			server.createContext("/path", exch -> new CsTransaction( this, exch).handle() );
//		});
//		
//		m_stocks = m_config.readStocks();
//		for (Stock stock : m_stocks) {
//			// map
//		}
//
//		Util.execute( "CSQ", () -> processQueue() );  // name the thread. pas
//	}
//
//	// probably better to integrate this into RefAPI
//	// ultimately the user orders could be combined with the Coinstore orders--maybe
//	static class CoinStock {
//		String m_contractId;  // lower case
//		int m_conid;
//		String m_symbol;
//		int m_csBalance;
//		int m_ibFilled;
//		int m_ibSub;
//		int m_position;
//		
//		// live orders must be saved so you know how much to subtract out later
//		
//		public boolean shouldBalance() {
//			return Math.abs(m_position) > .5; 
//		}
//		
//		void balance() {
//			Contract contract = new Contract();
//			// set conid and exchange
//
//			Order order = new Order();
//			order.side();
//			order.totalQty( m_desiredQuantity);
//			order.lmtPrice( orderPrice);
//			order.tif( m_config.tif() );  // VERY STRANGE: IOC does not work for API orders in paper system; TWS it works, and DAY works; if we have the same problem in the prod system, we will have to rely on our own timeout mechanism
//			order.allOrNone(session == Session.Smart);  // all or none, we don't want partial fills (not supported for Overnight)
//			order.transmit( true);
//			order.outsideRth( true);
//			order.orderRef(m_uid);
//			
//			
//			
//		}
//	}
//	
//	void incoming(String contractId) {
//		// load up
//		CoinStock cStock = m_map.get(contractId.toLowerCase());
//		// add to the balances
//		
//		
//	}
//	
//	private static Object processQueue() {
//		while (true) {
//			process();
//			S.sleep(60000);
//		}
//	}
//
//	
//	private static void process() {
//		for (CoinStock stk : m_map) {
//			stk.balance();
//		}
//	}
//	
//	
//
//	public void handleOrder( Order order) {
//		
//	}
//
//	public void queryAllPrices() {  // might want to move this into a separate microservice
//		try {
//			MyClient.getArray( m_mdsUrl).forEach( prices -> {
//				Stock stock = m_stocks.getStock( prices.getInt("conid") );
//				if (stock != null) {
//					stock.setPrices( new Prices(prices) );
//				
//					// we never delete a valid last price
//					double last = prices.getDouble("last");
//					if (last > 0) {
//						stock.put( "last", last); // I think it's wrong and Frontend doesn't use this pas
//					}
//				}
//			});
//		}
//		catch( Exception e) {
//			S.out( "Error fetching prices - " + e.getMessage() ); // need this because the exception doesn't give much info
//			// e.printStackTrace(); the stack trace is useless here and fills up the log
//			log( LogType.ERROR_4, e.getMessage() );
//		}
//	}
//	
//
//}
