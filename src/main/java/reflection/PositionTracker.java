package reflection;

import java.util.HashMap;

import org.json.simple.JsonArray;

import common.Util;

/** Track the fractional shares of each contract. Ultimately we should populate
 *  this with the current value at startup, but that would entail querying for
 *  the outstanding blockchain position for each stock */
public class PositionTracker {
	private HashMap<Integer,Pos> m_map = new HashMap<>();
	
	private static final double Small = .0000001;
	
	/** For stop or limit orders, always trade at least one share */
	public int buyOrSell( int conid, boolean buy, double qty) {
		return buyOrSell( conid, buy, qty, 0);  
	}

	public int buyOrSell( int conid, boolean buy, double qty, int atLeast) {
		return Util.getOrCreate(m_map, conid, () -> new Pos() ).buyOrSell(buy, qty, atLeast);  
	}

	public void undo( int conid, boolean buy, double qty, int rounded) {
		Util.getOrCreate(m_map, conid, () -> new Pos() ).undo(buy, qty, rounded);  
	}

	// ideally we would initialize this from the real positions at startup
	
	private static class Pos { 
		
		double m_desired; 	// total desired fractional shares only
		int m_actual; 		// total number of real shares derived from fractional shares 
		
		/** For stop or limit orders, always trade at least one share */
		synchronized int buyOrSell(boolean buy, double qty, int atLeast) {
			return buy ? buy(qty, atLeast) : sell(qty, atLeast);
		}
		
		synchronized void undo(boolean buy, double qty, int rounded) {
			if (buy) {
				undoBuy(qty, rounded);
			}
			else {
				undoSell(qty, rounded);
			}
		}

		/** For stop or limit orders, always trade at least one share */
		private int buy( double qty, int atLeast) {
			double dec = dec(qty);
			m_desired += dec;
			
			if (m_desired - m_actual >= (.5 - Small) || qty < 1. && atLeast >= 1) {
				m_actual++;
				return roundUp(qty); 
			}
			return roundDown(qty);
		}

		/** For stop or limit orders, always trade at least one share */
		private int sell(double qty, int atLeast) {
			double dec = dec(qty);
			m_desired -= dec;
			
			if (m_actual - m_desired > .5 + Small || qty < 1 && atLeast >= 1) {
				m_actual--;
				return roundUp(qty);
			}
			
			return roundDown(qty);
		}
		
		/** This is used to undo a transaction that was never filled */
		private void undoBuy(double qty, int rounded) {			
			m_desired -= qty;
			if (rounded > roundDown(qty) ) {
				m_actual --;
			}
		}

		/** This is used to undo a transaction that was never filled */
		private void undoSell(double qty, int rounded) {
			m_desired += qty;
			if (rounded > roundDown(qty) ) {
				m_actual++;
			}
		}
	}

	/** Return the decimal portion */
	public static double dec(double qty) {
		return qty - (int)qty;
	}

	/** 5.00001 should round to 5 */
	public static int roundUp(double qty) {
		return (int)Math.ceil(qty - Small);
	}

	/** 4.99999 should round to 5 */
	public static int roundDown(double qty) {
		return (int)Math.floor(qty + Small);
	}

	public JsonArray dump() {
		JsonArray ar = new JsonArray();
		m_map.entrySet().forEach( entry -> ar.add( Util.toJson(
				"conid", entry.getKey(),
				"actual", entry.getValue().m_actual,
				"desired", entry.getValue().m_desired) ) );
		return ar;
	}
}
