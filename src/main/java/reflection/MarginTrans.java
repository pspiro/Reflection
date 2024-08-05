package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.ib.client.Types.Action;
import com.sun.net.httpserver.HttpExchange;

import common.Util;
import reflection.MarginOrder.Status;
import reflection.TradingHours.Session;
import web3.Stablecoin;


public class MarginTrans extends MyTransaction {
	
	enum GoodUntil {
		Immediately,
		OneHour,
		EndOfDay,
		EndOfWeek,
		Never
	}

	static JsonObject staticConfig;
	
	MarginTrans(Main main, HttpExchange exchange, boolean debug) {
		super(main, exchange, debug);
	}

	public void marginStatic() {
		wrap( () -> {
			getWalletFromUri();
			
			if (staticConfig == null) {
				staticConfig = Util.toJson(
						"maxLeverage", 5.0,
						"goodUntil", GoodUntil.values(),
						"refreshInterval", 5000,
						"stocks", m_main.stocks().marginStocks().toArray( new Stock[0])
						);
			}
			
			respond( staticConfig);
		});
	}

	// we could remember the set of orders for each user and only resend the changes
	public void marginDynamic() {
		wrap( () -> {
			parseMsg();
			
			m_walletAddr = m_map.getWalletAddress();
			
			validateCookie( "margin-static");
			
			int conid = m_map.getRequiredInt("conid");
			Stock stock = m_main.getStock(conid);
			Util.require( stock != null, "invalid conid");
			Prices prices = stock.prices();
			
			JsonObject resp = Util.toJson(
					"bid", prices.bid(),
					"ask", prices.ask(),
					"last", prices.last(),
					"orders", getOrders() );
			
			respond( resp);
		});
	}

	public void marginOrder() {
		wrap( () -> {
			parseMsg();
			
			m_walletAddr = m_map.getWalletAddress();
			
			validateCookie( "margin-order");
			
			// get record from Users table
			JsonObject userRec = queryUserRec();
			require( userRec != null, RefCode.INVALID_USER_PROFILE, "Please update your profile and then resubmit your order");

			// validate user profile fields
			Profile profile = new Profile(userRec);
			profile.validate();
			
			int conid = m_map.getRequiredInt("conid");
			Stock stock = m_main.getStock(conid);
			require( stock != null, RefCode.INVALID_REQUEST, "Invalid conid");
			require( m_main.marginStore().canPlace( m_walletAddr, conid), RefCode.OPEN_MARGIN_ORDER, 
					"There is already an open margin order for this stock");  
			
			double amtToSpend = m_map.getRequiredDouble( "amountToSpend");
			require( amtToSpend >= m_config.marginMinSpend(), RefCode.INVALID_REQUEST, "The amount to spend must be at least %s", m_config.marginMinSpend() );
			require( amtToSpend >= 0 && amtToSpend <= m_config.marginMaxSpend(), RefCode.INVALID_REQUEST, "The amount to spend cannot be greater than %s", m_config.marginMaxSpend() );

			double leverage = m_map.getRequiredDouble( "leverage");
			require( leverage >= 1, RefCode.INVALID_REQUEST, "Leverage must be greater than or equal to one");
			require( leverage <= m_config.marginMaxLeverage(), RefCode.INVALID_REQUEST, "Leverage cannot be greater than %s", m_config.marginMaxLeverage() );
			
			// must always have valid last, even for limit order
			Prices prices = stock.prices();
			require( prices.validLast(), RefCode.NO_PRICES, "No valid last price in market");

			double entryPrice = m_map.getRequiredPrice( "entryPrice");
			require( entryPrice > prices.last() * .5, RefCode.INVALID_PRICE, "The 'buy' price is too low");
			require( entryPrice < prices.last() * 1.1, RefCode.INVALID_PRICE, "The 'buy' price is too high");

			double profitTakerPrice = m_map.getPrice( "profitTakerPrice");
			require( profitTakerPrice == 0 || profitTakerPrice > entryPrice, RefCode.INVALID_PRICE, "The profit-taker must be > entry price");

			double stopLossPrice = m_map.getPrice( "stopLossPrice");
			require( stopLossPrice >= 0 && stopLossPrice < entryPrice, RefCode.INVALID_PRICE, "The stop-loss price must be < entry price");
			require( stopLossPrice < prices.last() || m_map.getBool( "test"), RefCode.INVALID_PRICE, "The stop-loss price price must be less than current market price of %s", prices.last() );

			GoodUntil goodUntil = m_map.getEnumParam( "goodUntil", GoodUntil.values() );
			
			String currency = m_map.getRequiredString( "currency").toUpperCase();
			require( currency.equals( m_config.rusd().name() ) || currency.equals( m_config.busd().name() ),
					RefCode.INVALID_REQUEST,
					"currency is invalid");
			
			// check that they have sufficient stablecoin
			Stablecoin stablecoin = m_config.getStablecoin( currency);
			require( Util.isGtEq( stablecoin.getPosition( m_walletAddr), amtToSpend), RefCode.INSUFFICIENT_STABLECOIN, "You don't have enough stablecoin in your wallet for this transaction");
			
			// for leveraged orders, if the exchange is open, we must have bid/ask prices
			// if the exchange is closed, we don't care because the Buy order will not fill;
			// we can accept orders at any time; if we are in an overnight session, it means that the
			// market is open the next day so there would be no issue liquidating
			if (leverage > 1) {
				Session session = m_main.m_tradingHours.insideAnyHours( 
						stock.is24Hour(), m_map.get("simtime") );
				require( session == Session.None || prices.validBid() && prices.validAsk(), 
						RefCode.NO_PRICES, "The exchange is open but there are no bid/ask prices available.");
			}

			// check the dates (applies to stock splits only)
			m_main.m_tradingHours.checkSplitDates( m_map.get("simtime"), stock.getStartDate(), stock.getEndDate() );
			
			MarginOrder mo = new MarginOrder(
					m_main.apiController(),
					m_main.stocks(),
					m_main.marginStore(),
					m_walletAddr,
					m_uid + Util.uid(2),  // add an extra 2 to ensure uniqueness but also keep it tied to the original uid
					conid,
					Action.Buy,
					amtToSpend,
					leverage,
					entryPrice,
					profitTakerPrice,
					stopLossPrice,
					goodUntil,
					currency
					);
			
			out( "Received valid margin order " + mo);
			
			m_main.marginStore().startOrder( mo);
			
			respond( code, RefCode.OK, "orderId", mo.orderId(), Message, "Your order was accepted");
		});
	}

	/** Update margin order; user can update prices only */
	public void marginUpdate() {
		wrap( () -> {
			MarginOrder order = getOrder();
			
			Prices prices = m_main.stocks().getStockByConid( order.conid() ).prices();
			require( prices.validLast(), RefCode.NO_PRICES, "The order cannot be modified because there is no valid stock price available");

			double entryPrice = m_map.getPrice( "entryPrice");
			require( entryPrice == 0 || entryPrice > prices.last() * .5, RefCode.INVALID_PRICE, "The 'buy' price is too low");
			require( entryPrice < prices.last() * 1.1, RefCode.INVALID_PRICE, "The 'buy' price is too high (may not be more than 10% higher than current market price)");

			double profitTakerPrice = m_map.getPrice( "profitTakerPrice");
			//require( profitTakerPrice == 0 || profitTakerPrice > entryPrice, RefCode.INVALID_PRICE, "The profit-taker must be > entry price");

			double stopLossPrice = m_map.getPrice( "stopLossPrice");
//			double realEntry = entryPrice != 0 ? entryPrice : order.getDouble( "entryPrice");
//			require( stopLossPrice < realEntry, RefCode.INVALID_PRICE, "The stop-loss price must be < entry price");
//			require( stopLossPrice < prices.last(), RefCode.INVALID_PRICE, "The stop-loss price price must be less than current market price of %s", prices.last() );
			
			order.onUpdated( entryPrice, profitTakerPrice, stopLossPrice);
			respondSuccess();
		});
	}

	public void userCancel() {
		wrap( () -> {
			MarginOrder order = getOrder();
			
			out( "canceling order %s", order.orderId() ); // tie the cancel message to the original order
			
			if (m_map.getBool( "system") ) {  // used for testing
				order.systemCancel( "Canceled by System message");
			}
			else {
				order.userCancel();
			}

			respondSuccess();
		});
	}

	public void marginLiquidate() {
		wrap( () -> {
			MarginOrder order = getOrder();
			require( order.status() != Status.Liquidation, RefCode.INVALID_REQUEST, "The order is already in liquidation");
			require( order.status().canLiquidate(), RefCode.INVALID_REQUEST, "The position cannot be liquidated at this time");
			
			out( "liquidating order %s", order.orderId() ); // tie the cancel message to the original order

			order.userLiquidate();

			respond( code, RefCode.OK, Message, 
					"Liquidation has begun and your position is being closed out"); 
		});
	}

	/** Return all orders. For debug only */
	public void marginGetAll() {
		wrap( () -> respond( m_main.marginStore() ) );		
	}

	/** Get a single order by order id; used for debug and testing only */
	public void marginGetStatus() {
		wrap( () -> {
			String orderId = Util.getLastToken(m_uri, "/");
			require( orderId.length() == m_uid.length() + 2, RefCode.INVALID_REQUEST, "Invalid order id");
			
			MarginOrder order = m_main.marginStore().getById(orderId.toUpperCase() );  // this won't work if we change to mixed case orderId
			require( order != null, RefCode.INVALID_REQUEST, "No such order found");
			
			respond( code, RefCode.OK, "status", order.status() );
		});
	}

	public void marginAddFunds() {
		wrap( () -> {
			MarginOrder order = getOrder();
			
			out( "Adding funds to order %s", order.orderId() ); // tie the cancel message to the original order
			
			//order.addFunds();

			respondSuccess();
		});
	}

	public void marginWithdrawFunds() {
		wrap( () -> {
			MarginOrder order = getOrder();

			order.withdrawFunds();

			respondSuccess();
		});
	}

	public void marginWithdrawTokens() {
		wrap( () -> {
			respondSuccess();
		});
	}

	public void marginInfo() {
		wrap( () -> {
			respondSuccess();
		});
	}

	/** Called from monitor */
	public void systemCancel() {
		wrap( () -> {
			String orderId = Util.getLastToken(m_uri, "/").toUpperCase();
			require( orderId.length() == 10, RefCode.INVALID_REQUEST, "Invalid order id");
			
			MarginOrder order = m_main.marginStore().getById( orderId); 
			require( order != null, RefCode.INVALID_REQUEST, "No such order found");
			
			order.systemCancel( "Canceled by Monitor");

			respondOk();
		});
	}

	public void systemCancelAll() {
		wrap( () -> {
			m_main.marginStore().cancelAll();
			respondOk();
		});
	}

	private void respondSuccess() {
		respond( code, RefCode.OK, Message, "Success"); 
	}
	
	private MarginOrder getOrder() throws Exception {
		parseMsg();
		
		m_walletAddr = m_map.getWalletAddress();
		
		validateCookie( "margin-static");

		MarginOrder order = m_main.marginStore().getById( m_map.getRequiredString("orderId") );
		require( order != null, RefCode.INVALID_REQUEST, "No such order found");
//		require( order.wallet().equalsIgnoreCase( m_walletAddr),  // never happens because a wrong wallet gets picked up by validateCookie()
//				RefCode.INVALID_REQUEST, "Impossible");
		return order;
	}
	
	/** Return orders for one wallet */
	private JsonArray getOrders() throws Exception {
		JsonArray ar = m_main.marginStore().getOrders( m_walletAddr);
		
		Util.forEach( ar, order -> {
			Stock stock = m_main.getStock( order.getInt( "conid") );
			Util.require( stock != null, "order id %s has invalid conid %s", order.getString( "orderId"), order.getInt( "conid") );

			Prices prices = stock.prices();
			order.put( "bidPrice", prices.bid() );
			order.put( "askPrice", prices.ask() );
		});
			
		return ar;
	}
}
