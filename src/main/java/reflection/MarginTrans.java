package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.ib.client.Types.Action;
import com.sun.net.httpserver.HttpExchange;

import common.Util;


public class MarginTrans extends MyTransaction {
	static JsonObject staticConfig;
	
	// new config
	static int maxLeverage = 1;
	static int minSpend = 10;
	static int maxSpend = 1000;

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
					"orders", getOrders() );
			
			respond( resp);
		});
	}

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

	public Object marginUpdate() {
		// TODO Auto-generated method stub
		return null;
	}

	public void marginCancel() {
		wrap( () -> {
			parseMsg();
			
			m_walletAddr = m_map.getWalletAddress();
			
			validateCookie( "margin-static");
			
			MarginOrder order = m_main.marginStore().getById( m_map.getRequiredString("orderId") );
			require( order != null, RefCode.INVALID_REQUEST, "No such order found");
			
			order.userCancel();
			
			// think about the different states

			respondOk();
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
			
			Prices prices = stock.prices();
			require( prices.validBid() && prices.validAsk(), RefCode.INVALID_PRICE, "No valid prices in market");

			double amtToSpend = m_map.getRequiredDouble( "amountToSpend");
			require( amtToSpend >= minSpend, RefCode.INVALID_REQUEST, "The amount to spend must be at least %s", minSpend);
			require( amtToSpend >= 0 && amtToSpend <= maxSpend, RefCode.INVALID_REQUEST, "The amount to spend cannot be greater than %s", maxSpend);

			double leverage = m_map.getRequiredDouble( "leverage");
			require( leverage >= 1, RefCode.INVALID_REQUEST, "Leverage must be greater than or equal to one");
			require( leverage <= maxLeverage, RefCode.INVALID_REQUEST, "Leverage must less than %s", maxLeverage);
			
			double entryPrice = m_map.getRequiredPrice( "entryPrice");
			require( entryPrice > prices.markPrice() * .5, RefCode.INVALID_PRICE, "Entry price is too high");
			require( entryPrice < prices.markPrice() * 1.1, RefCode.INVALID_PRICE, "Entry price is too high");

			double profitTakerPrice = m_map.getPrice( "profitTakerPrice");
			require( profitTakerPrice == 0 || profitTakerPrice > entryPrice, RefCode.INVALID_REQUEST, "profitTakerPrice is invalid; must be > entry price");
			
			double stopLossPrice = m_map.getPrice( "stopLossPrice");
			require( stopLossPrice >= 0 && stopLossPrice < entryPrice, RefCode.INVALID_REQUEST, "stopLossPrice must be < entry price");
			require( stopLossPrice >= 0 && stopLossPrice < entryPrice, RefCode.INVALID_REQUEST, "stopLossPrice must be < entry price");

			GoodUntil goodUntil = m_map.getEnumParam( "goodUntil", GoodUntil.values() );
			
			String currency = m_map.getRequiredString( "currency");
			require( currency.equals( m_config.rusd().name() ) || currency.equals( m_config.busd().name() ),
					RefCode.INVALID_REQUEST,
					"currency is invalid");
			
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

			m_main.marginStore().add( mo);
			m_main.marginStore().save();
			
			respond( code, RefCode.OK, "orderId", mo.orderId() );

			// don't tie up the http server thread
			executeAndWrap( () -> {
				mo.acceptPayment();
			});
		});
		
	}
	enum GoodUntil {
		Immediately,
		OneHour,
		EndOfDay,
		EndOfWeek,
		Never
	}

	public void marginAll() {
		wrap( () -> respondFull(
				m_main.marginStore(),		
				200,
				null,
				"text/html") );
	}
}
