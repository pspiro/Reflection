package redis;

import java.text.SimpleDateFormat;

import redis.MktDataServer.MyTickType;
import redis.clients.jedis.Pipeline;
import reflection.Stock;
import reflection.TradingHours.Session;
import tw.util.S;

class DualPrices {
	private Stock m_stock;
	private Prices m_smart;
	private Prices m_ibeos;
	private Session m_was;

	DualPrices( Stock stock) {
		m_stock = stock;
		m_smart = new Prices(m_stock.getConid());
		m_ibeos = new Prices(m_stock.getConid());
	}
	
	boolean is24() {
		return m_stock.is24Hour();
	}

	public void tickSmart(MyTickType tickType, double price) {
		tick( m_smart, tickType, price);
	}

	public void tickIbeos(MyTickType tickType, double price) {
		tick( m_ibeos, tickType, price);
	}
	
	private void tick( Prices prices, MyTickType tickType, double price) {
		prices.tick( tickType, price);
	}

	
	public void send(Pipeline pipeline, Session inside) {
		if (inside == Session.Smart) {
			m_smart.send( pipeline, m_was != Session.Smart);
			m_was = Session.Smart;
		}
		else if (is24() && inside == Session.Ibeos) {
			m_ibeos.send(pipeline, m_was != Session.Ibeos); // flush the last set of prices from
			m_was = Session.Ibeos;
		}
		else if (m_was != Session.None) {
			// clear out the bid/ask, keep the last
			m_smart.clearOut(pipeline);
			m_was = Session.None;
		}
	}
	
	
	
	static public class Prices {
		private static SimpleDateFormat timeFmt = new SimpleDateFormat( "MM/dd HH:mm:ss");
		
		private double m_bid;
		private double m_ask;
		private double m_last;
		private long m_time;
		private boolean m_changed;
		private String m_conid;
		
		Prices(String conid) {
			m_conid = String.valueOf( conid);
		}

		public synchronized void tick(MyTickType tickType, double price) {
			switch( tickType) {
				case Bid:
					m_bid = price;
					break;
				case Ask:
					m_ask = price;
					break;
				case Last:
					m_last = price;
					m_time = System.currentTimeMillis();
					break;
			}
			m_changed = true;
		}

		public synchronized void send(Pipeline pipeline, boolean force) {  // better would be to store separate changed flags for each field
			if (m_changed || force) {
				if (MktDataServer.m_debug) S.out( "Updating redis with all prices for conid %s", m_conid);
				pipeline.hset( m_conid, "bid", String.valueOf( m_bid) );  // for -1 you should delete or you can wait for this to happen when it changes session
				pipeline.hset( m_conid, "ask", String.valueOf( m_ask) ); 
				pipeline.hset( m_conid, "last", String.valueOf( m_last) ); 
				pipeline.hset( m_conid, "time", String.valueOf( m_time) );
				m_changed = false;
			}
		}

		public void clearOut(Pipeline pipeline) {
			pipeline.hdel( m_conid, "bid");
			pipeline.hdel( m_conid, "ask");
		}
	}	
}