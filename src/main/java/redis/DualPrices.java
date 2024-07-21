package redis;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import redis.MdServer.MyTickType;
import reflection.Stock;
import reflection.TradingHours.Session;
import tw.util.S;

class DualPrices {
	static Prices NullPrices = new Prices(0);  // used when all sessions are closed; bid/ask is -2, so we know where it came from
	
	private Stock m_stock;
	private final Prices m_smart;
	private final Prices m_overnight;
	
	static {
		NullPrices.m_bid = -2;  // this value is not used anywhere but it shows up in Monitor and means there is no session
		NullPrices.m_ask = -2;
		NullPrices.m_last = -2;
	}

	DualPrices( Stock stock) {
		m_stock = stock;
		m_smart = new Prices(m_stock.conid());
		m_overnight = new Prices(m_stock.conid());
	}
	
	Prices smart() {
		return m_smart;
	}
	
	Stock stock() {
		return m_stock;
	}
	
	@Override public String toString() {
		return String.format( "smart: %s  ibeos: %s", m_smart, m_overnight);
	}
	
	boolean is24() {
		return m_stock.is24Hour();
	}

	public void tickSmart(MyTickType tickType, double price) {
		tick( m_smart, tickType, price, "smart");
	}

	public void tickIbeos(MyTickType tickType, double price) {
		tick( m_overnight, tickType, price, "overnight");
	}
	
	private void tick( Prices prices, MyTickType tickType, double price, String lastExchange) {
		prices.tick( tickType, price, lastExchange);
	}

	static public class Prices {
//		private static SimpleDateFormat timeFmt = new SimpleDateFormat( "MM/dd HH:mm:ss");
		
		private double m_bid;
		private double m_ask;
		private double m_last;
		private double m_bidSize;
		private double m_askSize;
		private long m_bidTime;
		private long m_askTime;
		private long m_lastTime;
		private boolean m_changed;
		private String m_conid;  // not used anymore, good for de
		private String m_from;
		
		Prices(int conid) {
			m_conid = String.valueOf( conid);
		}
		
		double bid() {
			return m_bid;
		}
		
		double ask() {
			return m_ask;
		}
		
		@Override public String toString() {
			return S.format( "conid=%s  bid=%s  ask=%s  last=%s  changed=%s  from=%s",
					m_conid, m_bid, m_ask, m_last, m_changed, m_from);
		}

		public synchronized void tick(MyTickType tickType, double price, String lastExchange) {
			switch( tickType) {
				case Bid:
					m_bid = price;
					m_bidTime = System.currentTimeMillis();
					break;
				case Ask:
					m_ask = price;
					m_askTime = System.currentTimeMillis();
					break;
				case BidSize:
					m_bidSize = price;
					break;
				case AskSize:
					m_askSize = price;
					break;
				case Last:
					m_last = price;
					m_lastTime = System.currentTimeMillis();
					break;
				case Close:  // use the close only if we have no last
					if (m_last <= 0 && price > 0) {
						m_last = price;
						m_lastTime = System.currentTimeMillis();
					}
					break;
			}
			m_changed = true;
			m_from = lastExchange;
		}

		public JsonObject getJsonPrices() {
			return Util.toJson(
					"bid", m_bid, 
					"ask", m_ask,
					"bidSize", m_bidSize, 
					"askSize", m_askSize,
					"last", m_last,
					"bid time", m_bidTime,
					"ask time", m_askTime,
					"last time", m_lastTime
					);
		}

		public double last() {
			return m_last;
		}

		public Prices clearBidAsk() {
			m_bid = -2;
			m_ask = -2;
			return this;
		}

		/** This is for the RefAPI (no sizes) */
		public void update(JsonObject stockPrices) {
			stockPrices.put( "bid", m_bid);
			stockPrices.put( "ask", m_ask);
			stockPrices.put( "last", m_last);
			stockPrices.put( "time", maxTime() );
		}

		private long maxTime() {
			return Math.max( m_bidTime, Math.max( m_askTime, m_lastTime) );
		}

	}

	/** Return all prices; used by Monitor */
	public void addPricesTo(JsonArray ret) {
		addPricesTo( ret, m_smart, "smart");
		addPricesTo( ret, m_overnight, "overnight");
	}

	/** Add one set of prices to the array (smart or ibeos) */
	private void addPricesTo( JsonArray ret, Prices prices, String from) {
		JsonObject stockPrices = new JsonObject();
		stockPrices.put( "conid", m_stock.conid() );
		stockPrices.put( "symbol", m_stock.symbol() );
		stockPrices.put( "from", from);
		stockPrices.putAll( prices.getJsonPrices() );
		ret.add( stockPrices);
	}

	Prices getPrices(Session session) {
		return switch(session) {
			case Smart -> m_smart;
			case Overnight -> m_overnight;
			default -> NullPrices;
		};
	}

	/** Update stockPrices with last price for the correct session
	 * 
	 *  NOTE it seems that in the paper system, smart prices do not
	 *  get updated during the overnight session, so if you want
	 *  prices during overnight session in live system, you will
	 *  have to keep the overnight prices and start using them again */
	public void update(JsonObject stockPrices, Session session) {
		// no we just always use smart
		getPrices(Session.Smart).update(stockPrices);

		// we used to take the prices from the correct session depending on what time it is
//		getPrices(session).update(stockPrices); 
//		
//		// if all sessions are closed, or we are in overnight and there
//		// is no last there, use last from smart
//		if (session == Session.None ||
//			session == Session.Overnight && m_overnight.last() <= 0) {
//			
//			stockPrices.put( "last", smart().last() );
//		}
	}

	public int conid() {
		return m_stock.conid();
	}

	public double getAnyLast() {
		return m_smart.last() > 0 ? m_smart.last() : m_overnight.last();
	}
}
