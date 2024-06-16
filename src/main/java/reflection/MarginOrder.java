package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import org.json.simple.JsonObject;

import com.ib.client.DualOrder;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;

import common.Util;
import tw.util.S;
import web3.RetVal;
import web3.Stablecoin;

class MarginOrder {
		enum Status { Start, StartedTransfer, CompletedTransfer, SubmittedOrder, PlacedOrder, Liquidation, SubLiquidation }
		//Status m_status;

		// config
		double feePct;
		double lastBuffer;  // as percent, try 
		double bidBuffer;
		double maxLeverage;
		double minUserAmt = 100;
		double maxUserAmt = 200;

		// serialized
		private MarginOrder.Status m_status;
		private final ParamMap m_map;
		private final String m_uid;
		private final DualOrder m_order;  // places the order to both SMART and OVERNIGHT
		
		// rebuild
		private Stock m_stock;
		private Stablecoin m_stablecoin;
		private String m_email;  // from users table

		private double entryPrice;
		private double leverage;
		private double userAmt;
		private double stopLoss;
		private double profitTaker;
		private int conid;

		private String walletAddr;

		private String fbId; // must be serialized

		/** Used for testing only */
		public MarginOrder(DualOrder o) {
			m_map = null;
			m_uid = Util.uid( 5);
			m_order = o;
			m_status = Status.Start;
		}

		/** Called when writing to file */
		public JsonObject toJson() {
			JsonObject obj = new JsonObject();
			obj.put( "userMap", m_map.obj() );
			obj.put( "orders", m_order.toJson() );  // only the order id's are saved
			obj.put( "uid", m_uid);
			obj.put( "status", m_status);
			obj.remove( "cookie");
			return obj;
		}

		/** Called when restoring from file */ 
		public MarginOrder(App main, JsonObject json) throws Exception {
			m_map = new ParamMap( json.getObject( "userMap") );
			m_uid = json.getString( "uid");
			m_order = new DualOrder( json.getObject( "orders") );
			m_status = Util.getEnum( json.getString( "status"), Status.values() );
			
			parse( main);
		}
		
		/** Called when a new order is received from the user */
		MarginOrder(App main, ParamMap map, String uid) throws Exception {
			m_map = map;
			m_uid = uid;
			m_order = new DualOrder();
			m_status = Status.Start;
			
			parse( main);
		}

		/** Called for a new order and when the order is restored from the file 
		 * @throws Exception */
		void parse( App main) throws Exception {

			walletAddr = m_map.getWalletAddress();
			
			// get record from Users table
			JsonObject userRec = MarginTransaction.queryUserRec(walletAddr); 
			require( userRec != null, RefCode.INVALID_USER_PROFILE, "Please update your profile and then resubmit your order");

			conid = m_map.getRequiredInt( "conid");
			require( conid > 0, RefCode.INVALID_REQUEST, "'conid' must be positive integer");
			
			m_stock = main.getStock(conid);  // throws exception if conid is invalid
			require( m_stock.allow().allow(Action.Buy), RefCode.TRADING_HALTED, "Trading for this stock is temporarily halted. Please try your order again later.");

			entryPrice = m_map.getRequiredDouble( "entryPrice");
			require( entryPrice > 0, RefCode.INVALID_REQUEST, "Price must be positive");

			//			require( Util.isGtEq(preCommAmt, m_config.minOrderSize()), RefCode.ORDER_TOO_SMALL, "The amount of your order (%s) is below the minimum allowed amount of %s", S.formatPrice( preCommAmt), S.formatPrice( m_config.minOrderSize()) ); // displayed to user
			//			require( Util.isLtEq(preCommAmt, m_config.maxOrderSize()), RefCode.ORDER_TOO_LARGE, "The amount of your order (%s) exceeds the maximum allowed amount of %s", S.formatPrice( preCommAmt), S.formatPrice( m_config.maxOrderSize()) ); // displayed to user

			// set m_stablecoin from currency parameter; must be RUSD or non-RUSD
			String currency = m_map.getRequiredString("currency").toUpperCase();
			if (currency.equals( m_config.rusd().name() ) ) {
				m_stablecoin = m_config.rusd();
			}
			else if (currency.equals(m_config.busd().name() ) ) {
				m_stablecoin = m_config.busd();
			}
			require( m_stablecoin != null, RefCode.INVALID_REQUEST, "Invalid currency");

			Profile profile = new Profile(userRec);
			profile.validate();

			// save email to send alerts later
			m_email = profile.email(); 

			// validate KYC fields
			require(
					S.equals( userRec.getString("kyc_status"), "VERIFIED"),
					RefCode.NEED_KYC,
					"Please verify your identity and then resubmit your order");

			// check the dates (applies to stock splits only)
			main.m_tradingHours.checkSplitDates( m_map.get("simtime"), m_stock.getStartDate(), m_stock.getEndDate() );

			userAmt = m_map.getRequiredDouble( "userAmt");
			require( userAmt >= minUserAmt, RefCode.INVALID_REQUEST, "User amount is too low");
			require( userAmt <= maxUserAmt, RefCode.INVALID_REQUEST,"User amount is too high");

			leverage = m_map.getRequiredDouble( "leverage");
			require( leverage >= 1, RefCode.INVALID_REQUEST, "The leverage cannot be less than one");
			require( leverage <= maxLeverage, RefCode.INVALID_REQUEST, "The leverage is too high");

			stopLoss = m_map.getRequiredDouble( "stopLoss");
			require( stopLoss < entryPrice, RefCode.INVALID_REQUEST, "The stop loss must be less than the entry price");

			profitTaker = m_map.getRequiredDouble( "profitTaker");
			require( profitTaker == 0 || profitTaker > entryPrice, RefCode.INVALID_REQUEST, "The profit-taker must be higher than the entry price");
		}
		
		// call this from somewhere
		synchronized void process(ApiController conn, Prices prices) throws Exception {
			if (m_status == Status.Start) {
				RetVal retVal = m_config.rusd().buyStock(
						walletAddr,
						m_stablecoin,
						userAmt * leverage,
						m_stock.getToken(),
						0
						);
				fbId = retVal.id();
				m_status = Status.StartedTransfer;
				save();
			}
			
			if (m_status == Status.StartedTransfer) {
				Util.require( fbId != null, "fbId can't be null");
				RetVal retVal = new RetVal( fbId);
				retVal.waitForCompleted();  // WE SHOULDN'T WAIT HERE SINCE WE ARE GETTING CALLED IN A LOOP ANYWAY // we have to be 100% sure that it goes through
				m_status = Status.CompletedTransfer;
				save();
			}
			
			if (m_status == Status.CompletedTransfer) {

				// place order
				m_order.action( Action.Buy);
				m_order.lmtPrice( entryPrice);
				m_order.tif( TimeInForce.GTC);
				m_order.outsideRth( true);
				m_order.orderRef(m_uid);
				m_order.ocaGroup(m_uid);
				m_order.placeOrder(conn, conid);
				m_status = Status.PlacedOrder;
				save();
			}

			if (m_status == Status.PlacedOrder) {
				//m_order.tick( prices);  // submits the order if necessary  can we just place it once, or we really need to wait for the exchange to be open?
			}
			
			// must check status here
			
			double balance = getBalance();
			double position = m_order.getPosition();
			
			
			if (balance < 0) {
				double loan = -balance;
				
				if (prices.bid() <= 0) {
					S.out( "liquidating  no bid");
					liquidate( conn);
				}
				else {
					double bidCollat = prices.bid() * position;
					double bidCoverage = bidCollat / loan; 
					if (bidCoverage <= Main.m_config.bidLiqBuffer() ) {
						S.out( "liquidate bid=%s  bidCollat=%s  loan=%s  bidCovererage=%s", 
								prices.bid(), bidCollat, loan, bidCoverage);
						liquidate( conn);
					}
					
					if (prices.validAsk() ) {
						double markCollat = prices.askMark() * position;
						double markCoverage = markCollat / loan;
						if (markCoverage <= Main.m_config.markLiqBuffer() ) {
							S.out( "liquidate bid=%s  ask=%s  last=%s  markCollat=%s  loan=%s  markCovererage=%s", 
									prices.bid(), prices.ask(), prices.ask(), markCollat, loan, markCoverage);
						}
					}
				}
			}
		}
		
		private void liquidate(ApiController conn) throws Exception {
			m_status = Status.Liquidation;
			
			m_order.cancel( conn);
			
			DualOrder order = new DualOrder();
			order.action( Action.Sell);
//			order.lmtPrice( entryPrice);
			order.orderType( OrderType.MKT); // you have to check if night orders support market
			order.tif( TimeInForce.GTC);
			order.outsideRth( true);
			order.orderRef(m_uid);
			order.ocaGroup("CLOSE" + m_uid);
			order.placeOrder(conn, conid);
			
			m_status = Status.SubLiquidation;  // this should be set only after both orders are confirmed
		}

		private double getBalance() {
			double balance = 0;

			// start with the amount we received from the user (positive)
			if (m_status.ordinal() >= Status.PlacedOrder.ordinal() ) {
				balance += userAmt * leverage;
			}

			// subtract out the amount spent on the orders
			balance += m_order.getBalance();

			return balance;
		}

		double maxFee() {
			return totalAmt() * feePct;
		}

		double totalAmt() {
			return userAmt * leverage;
		}

		double maxOrderAmt() {
			return totalAmt() - maxFee();
		}

		double desiredQty() {
			return maxOrderAmt() / entryPrice;
		}

		/** Set the IB order and listen for updates IF the permId matches */
		public void setOrder(ApiController conn, Order order) {
			m_order.setOrder( conn, order);
		}

		public void display() {
			m_order.display();
		}

		public void cancel(ApiController conn) {
			m_order.cancel(conn);
		}
		
		void save() throws Exception {
			MarginTransaction.mgr.write();
		}
	}