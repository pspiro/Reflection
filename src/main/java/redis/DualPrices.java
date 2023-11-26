package redis;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import redis.MdServer.MyTickType;
import redis.clients.jedis.Pipeline;
import reflection.Stock;
import reflection.TradingHours.Session;
import tw.util.S;

class DualPrices {
	static Prices NullPrices = new Prices(0);  // used when all sessions are closed; bid/ask is -2, so we know where it came from
	
	private Stock m_stock;
	private Prices m_smart;
	private Prices m_overnight;
	private Session m_was;   // prevSession would be a better name
	
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

	// generally, we force-send if the session type has changed
	public void send(Pipeline pipeline, Session inside) {
		if (inside == Session.Smart) {
			m_smart.send( pipeline, m_was != Session.Smart);
			m_was = Session.Smart;
		}
		else if (is24() && inside == Session.Overnight) {
			m_overnight.send(pipeline, m_was != Session.Overnight); // this won't work if last is never sent from IBEOS
			m_was = Session.Overnight;
		}
		else if (m_was != Session.None) {
			// clear out the bid/ask, keep the last
			m_smart.clearOut(pipeline);
			m_was = Session.None;
		}
	}
	
	
	static public class Prices {
//		private static SimpleDateFormat timeFmt = new SimpleDateFormat( "MM/dd HH:mm:ss");
		
		private double m_bid;
		private double m_ask;
		private double m_last;
		private long m_bidTime;
		private long m_askTime;
		private long m_lastTime;
		private boolean m_changed;
		private String m_conid;  // not used anymore, good for de
		private String m_from;
		
		Prices(int conid) {
			m_conid = String.valueOf( conid);
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

		public synchronized void send(Pipeline pipeline, boolean force) {  // better would be to store separate changed flags for each field
			if (m_changed || force) {
				if (MdServer.m_debug) S.out( "Updating redis with all prices for conid %s", m_conid);
				pipeline.hset( m_conid, "bid", String.valueOf( m_bid) );  // for -1 you should delete or you can wait for this to happen when it changes session
				pipeline.hset( m_conid, "ask", String.valueOf( m_ask) ); 
				pipeline.hset( m_conid, "last", String.valueOf( m_last) ); 
				pipeline.hset( m_conid, "time", String.valueOf( m_lastTime) );
				if (S.isNotNull( m_from) ) {
					pipeline.hset( m_conid, "from", m_from);
				}
				m_changed = false;
			}
		}

		public void clearOut(Pipeline pipeline) {
			if (MdServer.m_debug) S.out( "Clearing out bid/ask in redis for conid %s", m_conid);
			pipeline.hdel( m_conid, "bid");
			pipeline.hdel( m_conid, "ask");
		}

		public JsonObject getJsonPrices() {
			return Util.toJson(
					"bid", m_bid, 
					"ask", m_ask,
					"last", m_last,
					"bid time", m_bidTime,
					"ask time", m_askTime,
					"last time", m_lastTime
					);
		}

		public double bid() {
			return m_bid;
		}

		public double ask() {
			return m_ask;
		}
		
		public double last() {
			return m_last;
		}

		public Prices clearBidAsk() {
			m_bid = -2;
			m_ask = -2;
			return this;
		}

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

	/** Return one set of prices (smart or ibeos) */
	private void addPricesTo( JsonArray ret, Prices prices, String from) {
		JsonObject stockPrices = new JsonObject();
		stockPrices.putAll( m_stock);
		stockPrices.putAll( prices.getJsonPrices() );
		stockPrices.put( "from", from);
		ret.add( stockPrices);
	}

	public Prices getRefPrices(Session session) {
		return switch(session) {
			case Smart -> m_smart;
			case Overnight -> m_overnight;
			default -> NullPrices;
		};
	}

}