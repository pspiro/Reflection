package reflection;

import java.util.HashMap;

import common.Util;

public class PositionTracker {
	private HashMap<Integer,Pos> m_map = new HashMap<>();
	
	private static final double Small = .0000001;
	
	public int buyOrSell( int conid, boolean buy, double qty) {
		return Util.getOrCreate(m_map, conid, () -> new Pos() ).buyOrSell(buy, qty);  
	}

	public void undo( int conid, boolean buy, double qty, int rounded) {
		Util.getOrCreate(m_map, conid, () -> new Pos() ).undo(buy, qty, rounded);  
	}

	// ideally we would initialize this from the real positions at startup
	
	static class Pos { 
		
		double m_desired; 	// total desired fractional shares only
		int m_actual; 		// total number of real shares derived from fractional shares 
		
		public int buyOrSell(boolean buy, double qty) {
			return buy ? buy(qty) : sell(qty);
		}
		
		public void undo(boolean buy, double qty, int rounded) {
			if (buy) {
				undoBuy(qty, rounded);
			}
			else {
				undoSell(qty, rounded);
			}
		}

		int buy( double qty) {
			double dec = dec(qty);
			m_desired += dec;
			
			if (m_desired - m_actual >= (.5 - Small) ) {
				m_actual++;
				return roundUp(qty); 
			}
			return roundDown(qty);
		}

		int sell(double qty) {
			double dec = dec(qty);
			m_desired -= dec;
			
			if (m_actual - m_desired > .5 + Small ) {
				m_actual--;
				return roundUp(qty);
			}
			
			return roundDown(qty);
		}
		
		/** This is used to undo a transaction that was never filled */
		void undoBuy(double qty, int rounded) {			
			m_desired -= qty;
			if (rounded > roundDown(qty) ) {
				m_actual --;
			}
		}

		/** This is used to undo a transaction that was never filled */
		void undoSell(double qty, int rounded) {
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

	public static void main(String[] args) {
		PositionTracker t = new PositionTracker();
//		S.out( t.buy( 8314, .5) );
//		S.out( t.sell( 8314, .5) );
//		S.out( t.sell( 8314, .5) );
//		S.out( t.sell( 8314, .001) );
//		S.out( t.buy( 8314, 2.25) );
//		S.out( t.buy( 8314, 2.25) );
//		S.out( t.buy( 8314, 2.25) );
//		S.out( t.buy( 8314, 3.1) );
//		S.out( t.buy( 8314, 3.1) );
//		S.out( t.buy( 8314, 3.1) );
//		S.out( t.buy( 8314, 3.1) );
//		S.out( t.buy( 8314, 3.1) );
	}
	
	
}
