package reflection;

import static reflection.Main.log;
import static reflection.Main.require;
import static reflection.Util.round;

import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Decimal;
import com.ib.client.Types.SecType;
import com.ib.controller.AccountSummaryTag;
import com.ib.controller.ApiController.IAccountSummaryHandler;
import com.ib.controller.ApiController.IPositionHandler;
import com.sun.net.httpserver.HttpExchange;

import fireblocks.Busd;
import fireblocks.Rusd;
import positions.MoralisServer;
import redis.clients.jedis.Jedis;
import tw.util.S;
import util.LogType;

public class OldStyleTransaction extends MyTransaction {
	enum MsgType {
		disconnect,
		dump,
		getAllPrices,
		getAllStocks,
		getConfig,
		getConnectionStatus,
		getDescription,
		getPositions,
		getPrice,
		getCashBal,
		mint,
		pullBackendConfig,
		pullFaq,
		pushBackendConfig,
		pushFaq,
		refreshConfig,
		seedPrices,
		terminate,
		testAlert,
		wallet,
		;
	}

	OldStyleTransaction(Main main, HttpExchange exchange) {
		super(main, exchange);
		S.out( "  ***FOR DEBUG OR ADMIN ONLY***");
	}

	void handle() {
		wrap( () -> {
			parseMsg();
			handleMsg();
		});
	}

	private void handleMsg() throws Exception {
		Main.require( S.isNotNull(m_map.getParam("msg") ), RefCode.INVALID_REQUEST, "The 'msg' parameter is missing or the URI path is invalid");

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
			case getDescription:
				getDescription();
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
			case terminate:
				log( LogType.TERMINATE, "");
				System.exit( 0);
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
			case testAlert:
				onTestAlert();
				break;
			case seedPrices:
				onSeedPrices();
				break;
			case wallet:
				onShowWallet();
				break;
			case getCashBal:
				onCashBal();
				break;
		}
	}

	private void onCashBal() {
		wrap( () -> {
			JSONObject obj = new JSONObject();
			
			m_main.orderController().reqAccountSummary("All", AccountSummaryTag.nice, new IAccountSummaryHandler() {
				@Override public void accountSummary(String account, AccountSummaryTag tag, String value, String currency) {
					obj.put( tag.toString(), value);
				}
				@Override public void accountSummaryEnd() {
					m_main.orderController().cancelAccountSummary(this);					
					respond( obj);
				}
			});
			
			setTimer( Main.m_config.timeout(), () -> timedOut( "onCashBal() timed out") ); 
		});
	}

	/** Return json showing balance, allowance, and stock token positions */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void onShowWallet() throws Exception {
		String wallet = m_map.getRequiredParam("address");
		Main.require( Util.isValidAddress(wallet), RefCode.INVALID_REQUEST, "Invalid wallet address: %s", wallet);

		JSONObject obj = new JSONObject();
		obj.put( "Native token balance", MoralisServer.getNativeBalance(wallet) );
		obj.put( "Allowance", Main.m_config.busd().getAllowance(wallet, Main.m_config.rusdAddr() ) );
		
		Rusd rusd = Main.m_config.rusd();
		Busd busd = Main.m_config.busd();
		
		// should group these differently
//		for (MyJsonObject item : MoralisServer.reqPositions(wallet) ) {
//			String addr = item.getString("token_address");
//			if (addr.equals( rusd.address() ) ) {
//				obj.put( "RUSD", rusd.fromBlockchain( item.getString("balance") ) );
//			}
//			else if (addr.equals( busd.address() ) ) {
//				obj.put( "BUSD", busd.fromBlockchain( item.getString("balance") ) );
//			}
//			else {
//				HashMap stock = m_main.getStockByTokAddr(addr);
//				if (stock != null) {
//					obj.put( stock.get("symbol"), Erc20.fromBlockchain( 
//							item.getString("balance"), 
//							StockToken.stockTokenDecimals)); 
//				}
//			}
//		}
		
		respond(obj);
	}

	private void onTestAlert() {
		S.out( "Sending test alert");
		alert("TEST", "This is a test of the alert system");
		respondOk();
	}

	/** Return the IB stock positions; used by the Monitor program */
	private void getStockPositions() {
		JSONArray ar = new JSONArray();
		
		m_main.orderController().reqPositions( new IPositionHandler() {
			@SuppressWarnings("unchecked")
			@Override public void position(String account, Contract contract, Decimal pos, double avgCost) {
				JSONObject obj = new JSONObject();
				obj.put( "conid", contract.conid() );
				obj.put( "position", pos.toDouble() );
				ar.add( obj);
			}
			@Override public void positionEnd() {
				respond(ar); 
			}
		});
		
		setTimer( Main.m_config.timeout(), () -> timedOut( "getPositions timed out") );
	}

	/** Top-level message handler. This version takes wallet param; you can also call
	 *  reflection.trading/mint/0xxxx.xxx */
	private void mint() throws Exception {
		Main.mint( m_map.getRequiredParam( "wallet") );
		respond( code, "OK");
	}

	/** Simulate disconnect to test reconnect */
	private void disconnect() {
		S.out( "simulating disconnecting");
		m_main.orderConnMgr().disconnect();
		respondOk();
	}

	/** Top-level message handler */
//	private void pushBackendConfig() throws Exception {
//		S.out( "Pushing backend config from google sheet to database");
//		Main.m_config.pushBackendConfig( m_main.sqlConnection() );
//		respondOk();
//	}

	/** Top-level message handler, show TWS connection status */
	private void getConnStatus() {
		S.out( "Sending connection status");
		respond( "orderConnectedToTWS", m_main.orderController().isConnected(),
				 "orderConnectedToBroker", m_main.orderConnMgr().ibConnection() );
	}

	/** Top-level message handler, return RefAPI config */
	void getConfig() throws Exception {
		S.out( "Sending config");
		respond( Main.m_config.toJson() );
	}

	/** Top-level message handler, refresh all config and stock tokens */
	void refreshConfig() throws Exception {
		S.out( "Refreshing config and FAQs from google sheet");
		m_main.readSpreadsheet();
		respondOk();
	}

	/** Top-level message handler, return all stocks with prices */
//	private void getAllStocks() throws RefException {
//		require( !m_main.stocks().isEmpty(), RefCode.NO_STOCKS, "We don't have the list of stocks");
//
//		respond(m_main.stocks());
//	}

	/** Top-level method; used for admin purposes only, to get the conid */
	// should be removed after we write an algo to update the spreadsheet w/ the conids
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
				require( list.size() > 0, RefCode.NO_SUCH_STOCK, "No such stock");

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

				respond(whole);
			});
		});

		setTimer( Main.m_config.timeout(), () -> timedOut( "getDescription timed out") );
	}

	private void getPrice() throws RefException {
		int conid = m_map.getRequiredInt( "conid");
		returnPrice(conid);
	}	

	/** Top-level method; set some prices for use in test systems */
	void onSeedPrices() {
		wrap( () -> {
			Jedis jedis = Main.m_config.redisPort() == 0
				? new Jedis( Main.m_config.redisHost() )  // use full connection string
				: new Jedis( Main.m_config.redisHost(), Main.m_config.redisPort() );
	
			jedis.hset( "8314", "bid", "128.20");
			jedis.hset( "8314", "ask", "128.30");
			jedis.hset( "13824", "bid", "148.48");
			jedis.hset( "13824", "ask", "148.58");
			jedis.hset( "13977", "bid", "116.05");
			jedis.hset( "13977", "ask", "116.15");
			jedis.hset( "265598", "bid", "165.03");
			jedis.hset( "265598", "ask", "165.13");
			jedis.hset( "320227571", "bid", "318.57");
			jedis.hset( "320227571", "ask", "328.57");
			jedis.hset( "73128548", "bid", "341.03");
			jedis.hset( "73128548", "ask", "342.05");
			jedis.hset( "72063691", "bid", "328.55");
			jedis.hset( "72063691", "ask", "330.55");
			
			// update the prices on the objects
			m_main.queryAllPrices();
			
			// respond with prices
			getAllPrices();
		});
	}

	/** Top-level message, return prices for debugging */
	private void getAllPrices() throws RefException {
		
		// build the json response   // we could reuse this and just update the prices each time
		JSONObject whole = new JSONObject();

		for (Object obj : m_main.stocks() ) {
			Stock stk = (Stock)obj;

			JSONObject single = new JSONObject();
			single.put( "bid", round( stk.prices().bid() ) );
			single.put( "ask", round( stk.prices().ask() ) );
			single.put( "last", round( stk.prices().last() ) );
			single.put( "close", round( stk.prices().close() ) );
			single.put( "time", stk.prices().getFormattedTime() );

			whole.put( stk.get("conid"), single);
		}

		respond(whole);
	}

	/** Respond with build date/time */
	public void about() {
		wrap( () -> respond( "Built", Util.readResource( Main.class, "version.txt") ) );
	}

}
