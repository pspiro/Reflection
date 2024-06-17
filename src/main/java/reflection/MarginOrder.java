package reflection;

import org.json.simple.JsonObject;

import com.ib.client.DualOrder;
import com.ib.client.DualOrder.ParentOrder;
import com.ib.client.Types.Action;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;

import common.Util;
import tw.util.S;
import web3.Stablecoin;

class MarginOrder implements ParentOrder {
		enum Status {
			Entry,
			SubmittedOrder, 
			PlacedOrder, 
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

		// user fields from order
//		private int conid;
//		private double userAmt;
//		private double leverage;
//		private double profitTaker;
//		private double entryPrice;
//		private double stopLoss;

		private String walletAddr;
		private DualOrder m_dualOrder;

		private String fbId; // must be serialized
		private JsonObject m_userRec;  // not needed. pas
		private ApiController m_conn;

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
			m_status = Util.getEnum( order.getString( "status"), Status.values() );
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
//					S.out( "liquidating  no bid");
//					liquidate( conn);
//				}
//				else {
//					double bidCollat = prices.bid() * position;
//					double bidCoverage = bidCollat / loan; 
//					if (bidCoverage <= Main.m_config.bidLiqBuffer() ) {
//						S.out( "liquidate bid=%s  bidCollat=%s  loan=%s  bidCovererage=%s", 
//								prices.bid(), bidCollat, loan, bidCoverage);
//						liquidate( conn);
//					}
//					
//					if (prices.validAsk() ) {
//						double markCollat = prices.askMark() * position;
//						double markCoverage = markCollat / loan;
//						if (markCoverage <= Main.m_config.markLiqBuffer() ) {
//							S.out( "liquidate bid=%s  ask=%s  last=%s  markCollat=%s  loan=%s  markCovererage=%s", 
//									prices.bid(), prices.ask(), prices.ask(), markCollat, loan, markCoverage);
//						}
//					}
//				}
//			}
			
		}
		
		void placeBuyOrder() throws Exception {
			if (m_dualOrder == null) {
				double feePct = .01; // fix this. pas
				double totalSpend = userAmt() * leverage() * (1. - feePct);
				double desiredQty = totalSpend / entryPrice();
				int roundedQty = OrderTransaction.positionTracker.buyOrSell( m_stock.conid(), true, desiredQty);
				
				// place day and night orders
				m_dualOrder = new DualOrder( this, m_conn);
				m_dualOrder.action( Action.Buy);
				m_dualOrder.quantity( roundedQty);
				m_dualOrder.lmtPrice( entryPrice() );
				m_dualOrder.tif( TimeInForce.GTC);   // must consider this
				m_dualOrder.outsideRth( true);
				m_dualOrder.orderRef( orderId() );
				m_dualOrder.ocaGroup( orderId() );
				
				m_dualOrder.placeOrder( m_conn, conid() );
			}

			if (m_status == Status.PlacedOrder) {
				if (m_dualOrder == null) {
				}
				//m_order.tick( prices);  // submits the order if necessary  can we just place it once, or we really need to wait for the exchange to be open?
			}
			
			// must check status here
			
//			double balance = getBalance();
		}

		/** Called by dualOrder */
		@Override public void onCompleted(double totalFilled) {
			S.out( "some dual order finished; which one?");
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