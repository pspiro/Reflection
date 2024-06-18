package reflection;

import org.json.simple.JsonObject;

import com.ib.client.DualOrder;
import com.ib.client.DualOrder.DualParent;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;

import common.Util;
import tw.util.S;
import web3.Stablecoin;

class MarginOrder implements DualParent {
		enum Status {
			Start,
			Filled, 
			Liquidation, 
			SubLiquidation  
		}
		
		//Status m_status;

		// new margin config
		double feePct;
		double lastBuffer;  // as percent, try 
		double bidBuffer;
		double maxLeverage;
		double minUserAmt = 100;
		double maxUserAmt = 200;

		// serialized
		private MarginOrder.Status m_status;
		private final JsonObject m_order;
		// dualOrder; // places the order to both SMART and OVERNIGHT
		// rebuild
		
		private Stock m_stock;
		private Stablecoin m_stablecoin;
		private String m_email;  // from users table
		private double m_filledPrimary;


		private DualOrder m_primaryOrder;
		private DualOrder m_profitOrder;
		private DualOrder m_stopOrder;

		private String fbId; // must be serialized
		private JsonObject m_userRec;  // not needed. pas
		private ApiController m_conn;
		private int roundedQty;
		private double desiredQty;

		// jsonorder fields
		/* 
			"wallet_public_key"
			"orderId"
			"conid"
			"action"
			"amountToSpend"
			"leverage"
			"entryPrice"
			"profitTakerPrice"
			"stopLossPrice"
			"goodUntil"
			"currency",
			blockchainHash,  // move to log
			status
		 */
		
		/** Called when order is received by user OR when order is restored from database.
		 *  Note order.status() could be enum or string 
		 * @throws Exception */
		public MarginOrder(ApiController conn, JsonObject order, JsonObject userRec, Stock stock) throws Exception {
			m_conn = conn;
			m_order = order;
			m_userRec = userRec;
			m_stock = stock;
			m_status = Status.Start;
		}
		
		private void out( String format, Object... params) {
			S.out( orderId() + " " + format, params);
		}

		// called when the order is restored from the db
		synchronized void process() throws Exception {
//			if (m_status == Status.Entry) {
//				placeBuyOrder();
//			}

			// check for liquidation
//			if (balance < 0) {
//				double loan = -balance;
//				
//				if (prices.bid() <= 0) {
//					out( "liquidating  no bid");
//					liquidate( conn);
//				}
//				else {
//					double bidCollat = prices.bid() * position;
//					double bidCoverage = bidCollat / loan; 
//					if (bidCoverage <= Main.m_config.bidLiqBuffer() ) {
//						out( "liquidate bid=%s  bidCollat=%s  loan=%s  bidCovererage=%s", 
//								prices.bid(), bidCollat, loan, bidCoverage);
//						liquidate( conn);
//					}
//					
//					if (prices.validAsk() ) {
//						double markCollat = prices.askMark() * position;
//						double markCoverage = markCollat / loan;
//						if (markCoverage <= Main.m_config.markLiqBuffer() ) {
//							out( "liquidate bid=%s  ask=%s  last=%s  markCollat=%s  loan=%s  markCovererage=%s", 
//									prices.bid(), prices.ask(), prices.ask(), markCollat, loan, markCoverage);
//						}
//					}
//				}
//			}
			
		}
		
		void placeBuyOrder() throws Exception {
			if (m_primaryOrder == null) {
				out( "***Placing margin primary orders");
				double feePct = .01; // fix this. pas
				double totalSpend = userAmt() * leverage() * (1. - feePct);
				desiredQty = totalSpend / entryPrice();
				roundedQty = OrderTransaction.positionTracker.buyOrSell( m_stock.conid(), true, desiredQty, 1);
				
				// place day and night orders
				m_primaryOrder = new DualOrder( m_conn, this, "PRIMARY");
				m_primaryOrder.action( Action.Buy);
				m_primaryOrder.quantity( roundedQty);
				m_primaryOrder.orderType( OrderType.LMT);
				m_primaryOrder.lmtPrice( entryPrice() );
				m_primaryOrder.tif( TimeInForce.GTC);   // must consider this
				m_primaryOrder.outsideRth( true);
				m_primaryOrder.orderRef( orderId() + " primary" );
				// m_primaryOrder.ocaGroup( orderId() + " primary" ); // the problem is that the pair is canceled and then we can't see easily which was canceled first, plus we have to replace both orders

				m_primaryOrder.placeOrder( conid() );
			}
		}

		/** Called by dualOrder when the day and night orders are done */  // really we should listen for amt filled and add up the amounts from the profit taker and stop loss; could encapsulate this in a BracketOrder class
		@Override public void onCompleted(double totalFilled, DualOrder which) {
			if (which == m_primaryOrder && m_status == Status.Start) {  // check status here as well to be safe
				m_status = Status.Filled;
				
				m_filledPrimary = totalFilled;
				
				out( "The primary buy order has completed with %s filled shares", totalFilled);
				
				if (totalFilled == 0 && roundedQty > 0) {
					out( "Your order could not be filled");  // how to communicate this?
				}
				else {
					if (totalFilled < roundedQty) {
						out( "Partial fill");
						// need to adjust...something?
					}
					
					if (stopLossPrice() > 0) {
						placeStopLoss();
					}
					
					if (profitTakerPrice() > 0) {
						placeProfitTaker();
					}
					// else we have to monitor the prices and simulate
				}
			}
			else if (which == m_stopOrder || which == m_profitOrder) {
				if (m_status == Status.Filled) {
					Util.ifff( which == m_stopOrder ? m_profitOrder : m_stopOrder, other -> 
							other.cancel( m_conn) );
				}
				else {
					out( "Ignoring onCompleted() event");
				}
			}
		}

		private void placeStopLoss() {
			out( "***Placing margin stop-loss orders");
			
			// need to check m_filledPrimary; if < roundedQty, we need to adjust the size here. pas
			m_stopOrder = new DualOrder( m_conn, this, "STOP", m_stock.prices() );
			m_stopOrder.action( Action.Sell);
			m_stopOrder.quantity( roundedQty);
			m_stopOrder.orderType( OrderType.STP_LMT);  // use STOP_LMT  because STOP cannot be set to trigger outside RTH
			m_stopOrder.lmtPrice( stopLossPrice() * .95);
			m_stopOrder.stopPrice( stopLossPrice() );
			m_stopOrder.tif( TimeInForce.GTC);   // must consider this
			m_stopOrder.outsideRth( true);
			m_stopOrder.orderRef( orderId() + " stop" );
			// m_stopOrder.ocaGroup( orderId() + " bracket" );
			
			try {
				m_stopOrder.placeOrder( conid() );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}  // you need to cancel one when the other one fills; make sure stop is > liq order or place your own stop at the liq price
		
		private void placeProfitTaker() {
			out( "***Placing margin profit-taker orders");
			
			m_profitOrder = new DualOrder( m_conn, this, "PROFIT");
			m_profitOrder.action( Action.Sell);
			m_profitOrder.quantity( roundedQty);
			m_profitOrder.orderType( OrderType.LMT);
			m_profitOrder.lmtPrice( profitTakerPrice() );
			m_profitOrder.tif( TimeInForce.GTC);   // must consider this
			m_profitOrder.outsideRth( true);
			m_profitOrder.orderRef( orderId() + " profit" );
			//m_profitOrder.ocaGroup( orderId() + " bracket" );

			try {
				m_profitOrder.placeOrder( conid() );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		private double stopLossPrice() {
			return m_order.getDouble( "stopLossPrice");
		}

		private double profitTakerPrice() {
			return m_order.getDouble( "profitTakerPrice");
		}

		private String orderId() {
			return m_order.getString( "orderId");
		}

		private int conid() {
			return m_stock.conid();
		}

		private void liquidate(ApiController conn) throws Exception {
//			m_status = Status.Liquidation;
//			
//			m_order.cancel( conn);
//			
//			DualOrder order = new DualOrder();
//			order.action( Action.Sell);
////			order.lmtPrice( entryPrice);
//			order.orderType( OrderType.MKT); // you have to check if night orders support market
//			order.tif( TimeInForce.GTC);
//			order.outsideRth( true);
//			order.orderRef(m_uid);
//			order.ocaGroup("CLOSE" + m_uid);
//			order.placeOrder(conn, conid);
//			
//			m_status = Status.SubLiquidation;  // this should be set only after both orders are confirmed
		}

		private double getBalance() {
			double balance = 0;

//			// start with the amount we received from the user (positive)
//			if (m_status.ordinal() >= Status.PlacedOrder.ordinal() ) {
//				balance += userAmt;
//			}

			return balance;
		}

		double userAmt() {
			return m_order.getDouble( "amountToSpend");
		}
		double leverage() {
			return m_order.getDouble( "leverage");
		}
		double entryPrice() {
			return m_order.getDouble( "entryPrice");
		}
//		private double userAmt;
//		private double leverage;
//		private double profitTaker;
//		private double entryPrice;
//		private double stopLoss;


	}