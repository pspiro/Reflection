package reflection;

import static reflection.Main.log;
import static reflection.Main.require;
import static reflection.Main.round;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.Types.Action;
import com.ib.client.Types.SecType;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController.IPositionHandler;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import fireblocks.Fireblocks;
import tw.google.Auth;
import tw.google.TwMail;
import tw.util.S;
import util.LogType;

/** This class handles events from the Backend. See subclass */
public class MyTransaction {
	enum MsgType {
		checkHours,
		checkOrder,
		disconnect,
		dump,
		getAllPrices,
		getAllStocks,
		getConfig,
		getConnectionStatus,
		getDescription,
		getPositions,
		getPrice,
		mint,
		order,
		orderFb,
		pullBackendConfig,
		pullFaq,
		pushBackendConfig,
		pushFaq,
		refreshConfig,
		refreshStocks,
		terminate,
		test,
		;

		public static String allValues() {
			return Arrays.asList( values() ).toString();
		}
	}


	static double SMALL = .0001; // if difference between order size and fill size is less than this, we consider the order fully filled
	static final String code = "code";
	static final String text = "text";
	public static final String exchangeIsClosed = "The exchange is closed. Please try your order again after the stock exchange opens. For US stocks and ETF's, this is usually 4:00 EST (14:30 IST).";
	public static final String etf = "ETF";  // must match type column from spreadsheet
	private static final String ibeos = "IBEOS";  // IB exchange w/ 24 hour trading for ETF's
	static final HashMap<String,String> nullMap = new HashMap<>();
	
	protected Main m_main;
	protected HttpExchange m_exchange;
	private boolean m_responded;  // only respond once per transaction
	protected ParamMap m_map = new ParamMap();

	MyTransaction( Main main, HttpExchange exchange) {
		m_main = main;
		m_exchange = exchange;
	}

	void handle() {
		wrap( () -> {
			parseMsg();
			handleMsg();
		});
	}

	// you could encapsulate all these methods in MyExchange

	void parseMsg() throws Exception {
		String uri = m_exchange.getRequestURI().toString().toLowerCase();
		require( uri.length() < 4000, RefCode.UNKNOWN, "URI is too long");

		if ("GET".equals(m_exchange.getRequestMethod() ) ) {
			S.out( "Received GET request %s", uri);
			// get right side of ? in URL
			String[] parts = uri.split("\\?");
			require( parts.length ==2, RefCode.INVALID_REQUEST, "No request present. Valid requests are " + MsgType.allValues() );

			// build map of tag/value, expecting tag=value&tag=value
			String[] params = parts[1].split( "&");
			//map.parseJson( )
			for (String param : params) {
				String[] pair = param.split( "=");
				require( pair.length == 2, RefCode.INVALID_REQUEST, "Tag/value format is incorrect");
				m_map.put( pair[0], pair[1]);
			}
		}

		// POST request
		else {
			try {
	            Reader reader = new InputStreamReader( m_exchange.getRequestBody() );

	            JSONObject jsonObject = (JSONObject)new JSONParser().parse(reader);  // if this returns a String, it means the text has been over-stringified (stringify called twice)

	            for (Object key : jsonObject.keySet() ) {
	            	Object value = jsonObject.get(key);
	            	require( key instanceof String, RefCode.INVALID_REQUEST, "Invalid JSON, key is not a string");

	            	if (value != null) {
	            		m_map.put( (String)key, value.toString() );
	            	}
	            }

	            S.out( "Received POST request " + m_map.toString() );
			}
			catch( RefException e) {  // catch the above require() call
				throw e;
			}
			catch( ParseException e) {   // this exception does not set the exception message text
				throw new RefException( RefCode.INVALID_REQUEST, "Error parsing json - " + e.toString() );
			}
			catch( Exception e) {
				e.printStackTrace(); // should never happen
				throw new RefException( RefCode.INVALID_REQUEST, "Error parsing json - " + e.getMessage() ); // no point returning the message text because
			}
		}
	}

	void handleMsg() throws Exception {

		MsgType msgType = m_map.getEnumParam( "msg", MsgType.values() );

		switch (msgType) { // this could be switched to polymorphism if desired
			case mint:
				mint();
				break;
			case getPrice:
				getPrice();
				break;
			case getAllPrices:
				getAllPrices();
				break;
			case order:
				order( false, false);
				break;
			case orderFb:
				order( false, true);
				break;
			case checkOrder:
				order( true, false);
				break;
			case checkHours:
				checkHours();
				break;
			case getDescription:
				getDescription();
				break;
			case getAllStocks:
				getAllStocks();
				break;
			case refreshStocks:
				refreshStocks();
				break;
			case getConfig:
				getConfig();
				break;
			case refreshConfig:
				refreshConfig();
				break;
			case getConnectionStatus:
				getConnStatus();
				break;
			case pushBackendConfig:
				pushBackendConfig();
				break;
			case pullBackendConfig:
				pullBackendConfig();
				break;
			case pullFaq:
				pullFaq();
				break;
			case pushFaq:
				pushFaq();
				break;
			case terminate:
				terminate();
				break;
			case disconnect:
				disconnect();
				break;
			case dump:
				m_main.dump();
				respondOk();
				break;
			case getPositions:
				getStockPositions();
				break;
			case test:
				onTest();
				break;
		}
	}

	private void onTest() {
		Headers headers = m_exchange.getRequestHeaders();
		S.out( "headers");
		S.out( headers);
		S.out( "vals");
		List<String> vals = headers.get( "Authorization");
		S.out( vals);
		for (Entry<String, List<String>> a : headers.entrySet() ) {
			S.out( a);
		}
		
	}

	/** Used by the Monitor program */
	private void getStockPositions() {
		JSONArray ar = new JSONArray();
		
		m_main.orderController().reqPositions( new IPositionHandler() {
			@Override public void position(String account, Contract contract, Decimal pos, double avgCost) {
				JSONObject obj = new JSONObject();
				obj.put( "conid", contract.conid() );
				obj.put( "position", pos.toDouble() );
				ar.add( obj);
			}
			@Override public void positionEnd() {
				respond( new Json(ar) ); 
			}
		});
		
		setTimer( Main.m_config.timeout(), () -> timedOut( "getPositions timed out") );
	}

	/** Top-level message handler. This version takes wallet param; you can also call
	 *  reflection.trading/mint/0xxxx.xxx */
	void mint() throws Exception {
		Main.mint( m_map.getRequiredParam( "wallet") );
		respond( code, "OK");
	}

	/** Simulate disconnect to test reconnect */
	private void disconnect() {
		S.out( "simulating disconnecting");
		m_main.orderConnMgr().disconnect();
		respondOk();
	}

	private void terminate() {
		log( LogType.TERMINATE, "");
		System.exit( 0);
	}

	/** Top-level message handler */
	private void pushFaq() throws Exception {
		S.out( "Pushing FAQ");
		Main.m_config.pushFaq( Main.m_database);
		respondOk();
	}

	/** Top-level message handler */
	private void pullFaq() throws Exception {
		S.out( "Pulling FAQ");
		Main.m_config.pullFaq( Main.m_database);
		respondOk();
	}

	/** Top-level message handler */
	private void pushBackendConfig() throws Exception {
		S.out( "Pushing backend config from google sheet to database");
		Main.m_config.pushBackendConfig( Main.m_database);
		respondOk();
	}

	/** Top-level message handler */
	private void pullBackendConfig() throws Exception {
		S.out( "Pulling backend config from database to google sheet");
		Main.m_config.pullBackendConfig( Main.m_database);
		respond( code, RefCode.OK);
	}

	/** Top-level message handler */
	private void getConnStatus() {
		S.out( "Sending connection status");
		respond( "orderConnectedToTWS", m_main.orderController().isConnected(),
				 "orderConnectedToBroker", m_main.orderConnMgr().ibConnection() );
	}

	/** Top-level message handler */
	void getConfig() throws Exception {
		S.out( "Sending config");
		respond( Main.m_config.toJson() );
	}

	/** Top-level message handler */
	void refreshConfig() throws Exception {
		S.out( "Refreshing config from google sheet");
		Main.m_config.readFromSpreadsheet(m_main.tabName() );
		respond( Main.m_config.toJson() );
	}

	/** Top-level message handler */
	void refreshStocks() throws Exception {
		S.out( "Refreshing stock list from google sheet");
		m_main.refreshStockList();
		respondOk();
	}

	/** Top-level message handler */
	private void getAllStocks() throws RefException {
		require( !m_main.stocks().isEmpty(), RefCode.UNKNOWN, "We don't have the list of stocks");

		S.out( "Returning all stocks");
		respond( new Json( m_main.stocks() ) );
	}

	/** Top-level method; used for admin purposes only, to get the conid */
	private void getDescription() throws RefException {
		require( m_main.orderController().isConnected(), RefCode.NOT_CONNECTED, "Not connected");

		S.out( "Returning stock description");
		Contract contract = new Contract();
		contract.secType( SecType.STK);
		contract.symbol( m_map.getRequiredParam("symbol").replace( '_', ' ') );
		contract.exchange( m_map.getParam("exchange") );
		contract.currency( m_map.getParam("currency") );

		m_main.orderController().reqContractDetails(contract, list -> {
			wrap( () -> {
				require( list.size() > 0, RefCode.UNKNOWN, "No such stock");

				JSONArray whole = new JSONArray();

				for (ContractDetails deets : list) {
					JSONObject tradingHours = new JSONObject();
					tradingHours.put( "tradingHours", deets.tradingHours() );
					tradingHours.put( "liquidHours", deets.liquidHours() );
					tradingHours.put( "timeZone", deets.timeZoneId() );

					JSONObject obj = new JSONObject();
					obj.put( "symbol", deets.contract().symbol() );
					obj.put( "conid", deets.conid() );
					obj.put( "exchange", deets.contract().exchange() );
					obj.put( "prim_exchange", deets.contract().primaryExch() );
					obj.put( "currency", deets.contract().currency() );
					obj.put( "name", deets.longName() );
					obj.put( "tradingHours", tradingHours);

					whole.add( obj);
				}

				respond( new Json( whole).fmtArray() );
			});
		});

		setTimer( Main.m_config.timeout(), () -> timedOut( "getDescription timed out") );
	}

	/** Top-level method. */
	private void checkHours() throws RefException {
		require( m_main.orderController().isConnected(), RefCode.NOT_CONNECTED, "Not connected");

		int conid = m_map.getRequiredInt( "conid");
		require( conid > 0, RefCode.INVALID_REQUEST, "Param 'conid' must be positive integer");

		S.out( "Returning trading hours for %s", conid);
		Contract contract = new Contract();
		contract.conid( conid);
		contract.exchange( m_main.getExchange( conid) );

		m_main.orderController().reqContractDetails(contract, list -> processHours( conid, list) );

		setTimer( Main.m_config.timeout(), () -> timedOut( "checkHours timed out") );
	}

	private void processHours(int conid, List<ContractDetails> list) {
		wrap( () -> {
			require( !list.isEmpty(), RefCode.NO_SUCH_STOCK, "No contract details found for conid %s", conid);

			ContractDetails deets = list.get(0);

			if (inside( deets, deets.liquidHours() ) ) {
				respond( "hours", "liquid");
			}
			else if (inside( deets, deets.tradingHours() ) ) {
				respond( "hours", "illiquid");
			}
			else {
				respond( "hours", "closed");
			}
		});
	}

	/** Top-level method. */
	private void getPrice() throws RefException {
		int conid = m_map.getRequiredInt( "conid");

		Prices prices = m_main.getPrices( conid);
		require(prices.hasAnyPrice(), RefCode.NO_PRICES, "No prices available for conid %s", conid);

		S.out( "Returning prices  bid=%s  ask=%s  for conid %s", prices.bid(), prices.ask(), conid);
		respond( prices.toJson(conid) );
	}

	/** Top-level method. */  // put a timer so we don't call this more than once every n ms. pas
	private void getAllPrices() throws RefException {
		S.out( "Returning all prices");

		boolean admin = m_map.getBool("admin");

		// build the json response   // we could reuse this and just update the prices each time
		JSONObject whole = new JSONObject();
		for (Object obj : m_main.stocks() ) {
			Stock stk = (Stock)obj;

			JSONObject single = new JSONObject();
			// in admin mode, which is for debugging, return the actual prices
			if (admin) {
				single.put( "bid", round( stk.prices().bid() ) );
				single.put( "ask", round( stk.prices().ask() ) );
				single.put( "last", round( stk.prices().last() ) );
				single.put( "close", round( stk.prices().close() ) );
				single.put( "time", stk.prices().getFormattedTime() );
			}
			// for the user, we return best bid/ask we can come up with
			else {                                // this part should go away
				single.put( "bid", round( stk.prices().anyBid() ) );
				single.put( "ask", round( stk.prices().anyAsk() ) );
			}
			whole.put( stk.get("conid"), single);
		}

		respond( new Json( whole) );
	}

	/** Top-level method. */
	void order(boolean whatIf, boolean fireblocks) throws RefException {
		require( m_main.orderController().isConnected(), RefCode.NOT_CONNECTED, "Not connected");
		require( m_main.orderConnMgr().ibConnection() , RefCode.NOT_CONNECTED, "No connection to broker");

		int conid = m_map.getRequiredInt( "conid");
		require( conid > 0, RefCode.INVALID_REQUEST, "'conid' must be positive integer");
		m_main.getStock(conid);  // throws exception if conid is invalid

		String side = m_map.getParam( "side");
		if (S.isNull( side) ) {
			side = m_map.getRequiredParam("action");
		}
		require( side == "buy" || side == "sell", RefCode.INVALID_REQUEST, "Side must be 'buy' or 'sell'");

		double quantity = m_map.getRequiredDouble( "quantity");
		require( quantity > 0.0, RefCode.INVALID_REQUEST, "Quantity must be positive");

		double price = m_map.getRequiredDouble( "price");
		require( price > 0, RefCode.INVALID_REQUEST, "Price must be positive");

		double amt = price * quantity;
		double maxAmt = side == "buy" ? Main.m_config.maxBuyAmt() : Main.m_config.maxSellAmt();
		require( amt <= maxAmt, RefCode.ORDER_TOO_LARGE, "The total amount of your order (%s) exceeds the maximum allowed amount of %s", S.formatPrice( amt), S.formatPrice( maxAmt) ); // this is displayed to user

		String wallet = null;
		String cryptoId = null;
		if (!whatIf) {
			wallet = m_map.getRequiredParam("wallet");
			cryptoId = m_map.getParam("cryptoid");  // remove this, no longer used
		}

		// calculate order price
		double prePrice;
		if (side == "buy") {
			prePrice = price - price * Main.m_config.minBuySpread();
		}
		else {
			prePrice = price + price * Main.m_config.minSellSpread();
		}
		double orderPrice = Util.round( prePrice);  // round to two decimals
		
		if (fireblocks) {
			m_map.getRequiredParam("currency");
		}

		Contract contract = new Contract();
		contract.conid( conid);
		contract.exchange( m_main.getExchange( conid) );

		Order order = new Order();
		order.action( side == "buy" ? Action.BUY : Action.SELL);
		order.totalQuantity( quantity);
		order.lmtPrice( orderPrice);
		order.tif( TimeInForce.IOC);
		order.allOrNone(true);  // all or none, we don't want partial fills
		order.whatIf( whatIf);
		order.transmit( true);
		order.outsideRth( true);
		order.cryptoId( cryptoId);
		order.walletAddr( wallet);
		order.stockTokenAddr( m_main.getSmartContractId(conid) );

		S.out( "Requesting contract details for %s on %s", conid, contract.exchange() );

		insideAnyHours( contract, inside -> {
			require( inside, RefCode.EXCHANGE_CLOSED, exchangeIsClosed);

			// check that we have prices and that they are within bounds;
			// do this after checking trading hours because that would
			// explain why there are no prices which should never happen otherwise
			Prices prices = m_main.getPrices( contract.conid() );
			prices.checkOrderPrice( order, orderPrice, Main.m_config);

			// if the user submitted an order for < .5 shares, we round to zero so no order is placed
			if (whatIf) {
				// for what-if, submit it with at least qty of 1 to make sure it is a valid order
				if (order.roundedQty() == 0) {
					order.totalQuantity(1);
				}

				log( LogType.CHECK, order.getCheckLog(contract) );
				submitWhatIf( contract, order);
			}
			else {
				log( LogType.ORDER, order.getOrderLog(contract) );

				// if order size < .5, we won't submit an order; better would be to compare our total share balance with the total token balance. pas
				if (order.roundedQty() == 0) {
					respondToOrder(order, 0, false, OrderStatus.Filled, fireblocks);
				}
				else {
					submitOrder(  contract, order, fireblocks);
				}
			}
		});
	}

	interface Inside {
		void run(boolean inside) throws Exception;
	}

	/** Check if we are inside trading hours. For ETF's, check smart; if that fails,
	 *  check IBEOS and change the exchange on the contract passed in to IBEOS */
	void insideAnyHours( Contract contract, Inside runnable) {
		insideHours( contract, inside -> {
			if (!inside && etf.equals( m_main.getType( contract.conid() ) ) ) {
				contract.exchange(ibeos);
				insideHours( contract, runnable);
			}
			else {
				runnable.run(inside);
			}
		});
	}

	/** Call true or false on the Inside runnable */
	void insideHours( Contract contract, Inside runnable) {
		m_main.orderController().reqContractDetails(contract, list -> {
			wrap( () -> {
				require( !list.isEmpty(), RefCode.INVALID_REQUEST, "No contract details");

				ContractDetails deets = list.get(0);
				deets.simTime( m_map.getParam("simtime") );  // this is for use by the test scripts in TestOutsideHours only

				runnable.run( inside( deets) );
			});
		});
	}

	/** Return true if we are current inside trading hours OR liquid hours.
	 *  When running test scripts, simTime param will be set and used. */
	static boolean inside( ContractDetails deets) throws Exception {
		return inside( deets, deets.tradingHours() ) ||
			   inside( deets, deets.liquidHours() );
	}

	/** Return true if we are inside the specified hours; uses deets.simTime if set.
	 * @throws Exception */
	static boolean inside(ContractDetails deets, String hours) throws Exception {
		return Util.inside( deets.getNow(), deets.conid(), hours, deets.timeZoneId() );
	}

	private void submitWhatIf( Contract contract, final Order order) throws RefException {
		// check trading hours first since it is a nicer error message

		// submit what-if order
		m_main.orderController().placeOrModifyOrder(contract, order, new OrderHandlerAdapter() {
			@Override public void orderState(OrderState orderState) {
				wrap( () -> {
					S.out( "  rec what-if orderState  id=%s  %s", order.orderId(), Main.tos( orderState) );
					double initMargin = Double.valueOf( orderState.initMarginAfter() );
					double elv = Double.valueOf( orderState.equityWithLoanAfter() );
					require( initMargin <= elv, RefCode.REJECTED, "Insufficient liquidity in brokerage account");
					respondOk();
				});
			}
			@Override public void handle(int errorCode, String errorMsg) {
				wrap( () -> {
					S.out( "  rec what-if error for %s; %s - %s", order.orderId(), errorCode, errorMsg);

					// NOTE that this code is duplicated in what-if handler

					// price does not conform to market rule, e.g. too many decimals
					if (errorCode == 110) {
						require( false, RefCode.INVALID_PRICE, errorMsg);
					}
					require( false, RefCode.UNKNOWN, errorCode + " - " + errorMsg);
				});
			}
		});

		setTimer( Main.m_config.timeout(), () -> timedOut( "checkorder timed out") );
	}

	private void submitOrder( Contract contract, Order order, boolean fireblocks) throws RefException {
		ModifiableDecimal shares = new ModifiableDecimal();

		// very dangerous!
		if (Main.m_config.approveAll() ) {
			S.out( "Auto-filling order  id=%s", order.orderId() );
			respond( code, RefCode.OK, "filled", order.totalQty() );

			log( LogType.AUTO_FILL, "id=%s  cryptoid=%s  action=%s  orderQty=%s  filled=%s  orderPrc=%s  commission=%s  tds=%s  hash=%s",
					order.orderId(), order.cryptoId(), order.action(), order.totalQty(),
					order.totalQty(), order.lmtPrice(),
					Main.m_config.commission(), 0, "");
			return;
		}

		m_main.orderController().placeOrModifyOrder(contract, order, new OrderHandlerAdapter() {
			@Override public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice,
					int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {

				wrap( () -> {
					S.out( "  order status  id=%s  status=%s", order.orderId(), status);

					// save the number of shares filled
					shares.value = filled.toDouble();
					//shares.value = filled.toDouble() - 1;  // to test partial fills

					// better is: if canceled w/ no shares filled, let it go to handle() below

					if (status.isComplete() ) {
						respondToOrder( order, shares.value(), false, status, fireblocks);
					}
				});
			}

			@Override public void handle(int errorCode, String errorMsg) {
				wrap( () -> {
					log( LogType.ORDER_ERR, "id=%s  errorCode=%s  errorMsg=%s", order.orderId(), errorCode, errorMsg);

					// if some shares were filled, let orderStatus or timeout handle it
					if (shares.nonZero() ) {
						return;
					}

					// no shares filled

					// price does not conform to market rule, e.g. too many decimals
					if (errorCode == 110) {
						throw new RefException( RefCode.INVALID_PRICE, errorMsg);
					}

					// order rejected, never sent to exchange; could be not enough liquidity
					if (errorCode == 201) {
						throw new RefException( RefCode.REJECTED, errorMsg);
					}
				});
			}
		});

		log( LogType.SUBMIT, "wallet=%s  cryptoid=%s  orderid=%s",
				order.walletAddr(), order.cryptoId(), order.orderId() );

		// use a higher timeout here; it should never happen since we use IOC
		// order timeout is a special case because there could have been a partial fill
		setTimer( Main.m_config.orderTimeout(), () -> onTimeout( order, shares.value(), OrderStatus.Unknown, fireblocks) );
	}

	private synchronized void onTimeout(Order order, double filledShares, OrderStatus status, boolean fireblocks) throws Exception {
		// this could happen if our timeout is lower than the timeout of the IOC order,
		// which should never be the case
		if (!m_responded) {
			log( LogType.ORDER_TIMEOUT, "id=%s  cryptoid=%s   order timed out with %s shares filled and status %s", order.orderId(), order.cryptoId(), filledShares, status);

			// if order is still live, cancel the order
			if (!status.isComplete() && !status.isCanceled() ) {
				S.out( "Canceling order %s on timeout", order.orderId() );
				m_main.orderController().cancelOrder( order.orderId(), "", null);
			}

			respondToOrder( order, filledShares, true, status, fireblocks);
		}
	}

	/** This is called when order status is "complete" or when timeout occurs.
	 *  Access to m_responded is synchronized.
	 *  In the case where order qty < .5 and we didn't submit an order,
	 *  orderStatus will be Filled. */
	private synchronized void respondToOrder(Order order, double filledShares, boolean timeout, OrderStatus status, boolean fireblocks) throws Exception {
		if (m_responded) {
			return;    // this happens when the timeout occurs after an order is filled, which is normal
		}

		// no shares filled and order size >= .5?
		if (filledShares == 0 && status != OrderStatus.Filled) {  // Filled status w/ zero shares means order size was < .5
			String msg = timeout ? "Order timed out, please try again" : "The order could not be filled; it may be that the price changed. Please try again.";
			respond( code, RefCode.REJECTED, text, msg);
			log( LogType.REJECTED, "id=%s  cryptoid=%s  orderQty=%s  orderPrc=%s  reason=%s",
					order.orderId(), order.cryptoId(), order.totalQty(), order.lmtPrice(), msg);
			return;
		}

		double stockTokenQty;  // quantity of stock tokens to swap
		LogType logType;  // fill or partial fill
		RefCode refCode;  // fill or partial fill

		// for a filled order, the (order size - filled size) should always be <= .5
		// if > .5, then it was a partial fill and we will use the filled size instead of the order size
		// this way we always have max .5 shares difference between stock pos and token pos (for a single order)
		if (order.totalQuantity() - filledShares > .5001) {
			// this should never happen since we set all-or-none on the orders
			stockTokenQty = filledShares;
			logType = LogType.PARTIAL_FILL;
			refCode = RefCode.PARTIAL_FILL;
		}
		else {
			stockTokenQty = order.totalQuantity();
			logType = LogType.FILLED;
			refCode = RefCode.OK;
		}

		double tds = 0;     // the tds tax paid by Indian residents
		String hash = "";   // the blockchain hashcode

		if (fireblocks) {
			try {
				String id;

				// buy
				if (order.action() == Action.BUY) {
					double stablecoinAmt = stockTokenQty * order.lmtPrice() + Main.m_config.commission();
					
					// buy with RUSD
					if (m_map.getParam("currency").toLowerCase().equals( "rusd") ) {
						id = m_main.rusd().buyStockWithRusd(
								order.walletAddr(), 
								stablecoinAmt,
								order.stockTokenAddr(), 
								stockTokenQty
						);
					}
					
					// buy with BUSD
					else {
						id = m_main.rusd().buyStock(
								order.walletAddr(),
								Main.m_config.newBusd(),
								stablecoinAmt,
								order.stockTokenAddr(), 
								stockTokenQty
						);
					}
				}
				
				// sell
				else {
					double preAmt = stockTokenQty * order.lmtPrice() - Main.m_config.commission();
					tds = .01 * preAmt;
					double stablecoinAmt = preAmt - tds;
					id = m_main.rusd().sellStockForRusd(
							order.walletAddr(),
							stablecoinAmt,
							order.stockTokenAddr(),
							stockTokenQty
					);
				}

				// it would be better if we could send back the response in two blocks, one
				// when the order fills and one when the blockchain transaction is completed

				// wait for the transaction to be signed
				// this won't be good if we have multiple orders pending since each one is
				// polling every one second; either put them in a queue or use the Fireblocks
				// callback mechanism
				hash = Fireblocks.getTransHash(id, 60);  // do we really need to wait this long? pas
				log( LogType.ORDER, "Order %s completed Fireblocks transaction with hash %s", order.orderId(), hash);
			}
			catch( Exception e) {
				e.printStackTrace();
				log( LogType.ERROR, "Fireblocks failed for order %i - %s", order.orderId(), e.getMessage() );
				respond( code, RefCode.BLOCKCHAIN_FAILED, text, "Blockchain transaction failed; please try again");
				unwindOrder(order);
				return;
			}
		}

		respond( code, refCode, "filled", stockTokenQty);

		log( logType, "id=%s  cryptoid=%s  action=%s  orderQty=%s  filled=%s  orderPrc=%s  commission=%s  tds=%s  hash=%s",
				order.orderId(), order.cryptoId(), order.action(), order.totalQty(),
				S.fmt4(filledShares), order.lmtPrice(),
				Main.m_config.commission(), tds, hash);
	}

	/** The order was filled, but the blockchain transaction failed, so we must unwind the order. */
	private void unwindOrder(Order order) {
		// send an alert to the operator to manually unwind the order for now. pas
	}

	public void respondOk() {
		respond( code, RefCode.OK);
	}

	synchronized boolean respond( Object...data) {
		return respond( Util.toJsonMsg( data) );
	}

	/** Only respond once for each request
	 *  @return true if we responded just now. */
	boolean respond( Json response) {
		return respond( response, nullMap);
	}

	synchronized boolean respond( Json response, HashMap<String,String> headers) {
		if (m_responded) {
			return false;
		}

		// need this? pas
		try (OutputStream outputStream = m_exchange.getResponseBody() ) {
			m_exchange.getResponseHeaders().add( "Content-Type", "application/json");

			// add custom headers, if any  (add URL encoding here?)
			for (Entry<String, String> header : headers.entrySet() ) {
				m_exchange.getResponseHeaders().add( header.getKey(), header.getValue() );
			}
			
			m_exchange.sendResponseHeaders( 200, response.length() );
			outputStream.write(response.getBytes());
		}
		catch (Exception e) {
			e.printStackTrace();
			log( LogType.ERROR, "Exception while responding with json");
		}
		m_responded = true;
		return true;
	}

	void wrap( ExRunnable runnable) {
		try {
			runnable.run();
		}
		catch( RefException e) {
			boolean responded = respond( e.toJson() );  // return false if we already responded

			// display log except for timeouts where we have already responded
			if (responded || e.code() != RefCode.TIMED_OUT) {
				log( LogType.ERROR, e.toString() );
			}
		}
		catch( Exception e) {
			e.printStackTrace();
			log( LogType.ERROR, S.notNull( e.getMessage() ) );
			respond( 
					RefException.eToJson(e, RefCode.UNKNOWN) 
				);
		}
	}

	/** Runnable, returns void, throws Exception */
	public interface ExRunnable {
		void run() throws Exception;
	}

	void setTimer( long ms, ExRunnable runnable) {
		Timer timer = new Timer();
		timer.schedule( new TimerTask() {  // this could be improved to have only one Timer and hence one Thread for all the scheduling. pas
			@Override public void run() {
				wrap( runnable);
				timer.cancel();
			}
		}, ms);
	}

	static void timedOut( String text, Object... params) throws RefException {
		throw new RefException( RefCode.TIMED_OUT, text, params);
	}

	static class ModifiableDecimal {
		private double value = 0;

		@Override public String toString() {
			return S.fmt3(value);
		}

		public double value() {
			return this.value;
		}

		boolean isZero() {
			return value == 0;
		}

		boolean nonZero() {
			return value != 0;
		}
	}
	
	protected void alert(String subject, String body) throws Exception {
		TwMail mail = Auth.auth().getMail();
		mail.send(
				"RefAPI", 
				"peteraspiro@gmail.com", 
				"peteraspiro@gmail.com",
				subject,
				body,
				"plain");
	}
}

// with 2 sec timeout, we see timeout occur before fill is returned
// add tests for partial fill, no fill
// confirm that TWS does not accept fractional shares
// test w/ a short timeout to see the timeout happen, ideally with 0 shares and partial fill
// test exception during fireblocks part
// IOC timeout seems to be 3-4 seconds
// don't store commission in two places in db