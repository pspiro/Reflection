package reflection;

import static common.Util.round;
import static reflection.Main.require;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Decimal;
import com.ib.client.Types.SecType;
import com.ib.controller.AccountSummaryTag;
import com.ib.controller.ApiController.IAccountSummaryHandler;
import com.ib.controller.ApiController.IPositionHandler;
import com.sun.net.httpserver.HttpExchange;

import tw.util.S;

public class OldStyleTransaction extends MyTransaction {
	enum MsgType {
		disconnect,
		dump,
		getAllPrices,
		getTradingHours(),
		getDescription,
		getPositions,
		getPrice,
		getCashBal,
		refreshConfig,
		terminate,
		testAlert,
		;
	}

	OldStyleTransaction(Main main, HttpExchange exchange) {
		super(main, exchange);
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
			case getTradingHours:
				getTradingHours();
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
			case refreshConfig:
				refreshConfig();
				break;
			case terminate:
				out( "Received terminate message");
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
			case getCashBal:
				onCashBal();
				break;
		}
	}

	private void getTradingHours() {
		respond( m_main.m_tradingHours.getHours() );
	}

	/** This is by the Monitor to get cash in IB account  */ 
	private void onCashBal() {
		JsonObject obj = new JsonObject();
		
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
	}

	private void onTestAlert() {
		out( "Sending test alert");
		alert("TEST", "This is a test of the alert system");
		respondOk();
	}

	/** Return the IB stock positions; used by the Monitor program */
	private void getStockPositions() {
		JsonArray ar = new JsonArray();
		
		m_main.orderController().reqPositions( new IPositionHandler() {
			@Override public void position(String account, Contract contract, Decimal pos, double avgCost) {
				JsonObject obj = new JsonObject();
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

	/** Simulate disconnect to test reconnect */
	private void disconnect() {
		out( "simulating disconnecting");
		m_main.orderConnMgr().disconnect();
		respondOk();
	}

	/** Top-level message handler, refresh all config and stock tokens */
	void refreshConfig() throws Exception {
		out( "Refreshing config and FAQs from google sheet");
		m_main.readSpreadsheet( m_map.getBool("refreshstocks") );  // just read config settings unless "readstocks=true" is passed
		respondOk();
	}

	/** Top-level method; used for admin purposes only, to get the conid */
	// should be removed after we write an algo to update the spreadsheet w/ the conids
	private void getDescription() throws RefException {
		require( m_main.orderController().isConnected(), RefCode.NOT_CONNECTED, "Not connected");

		out( "Returning stock description");
		Contract contract = new Contract();
		contract.secType( SecType.STK);
		contract.symbol( m_map.getRequiredParam("symbol").replace( '_', ' ') );
		contract.exchange( m_map.getParam("exchange") );
		contract.currency( m_map.getParam("currency") );

		m_main.orderController().reqContractDetails(contract, list -> {
			wrap( () -> {
				require( list.size() > 0, RefCode.NO_SUCH_STOCK, "No such stock");

				JsonArray whole = new JsonArray();

				for (ContractDetails deets : list) {
					JsonObject tradingHours = new JsonObject();
					tradingHours.put( "tradingHours", deets.tradingHours() );
					tradingHours.put( "liquidHours", deets.liquidHours() );
					tradingHours.put( "timeZone", deets.timeZoneId() );

					JsonObject obj = new JsonObject();
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

	/** Not being used because the google sheet IMPORTDATA doesn't work */
	private void getPrice() throws RefException {
		int conid = m_map.getRequiredInt( "conid");
		boolean csv = m_map.getBool("csv");
		returnPrice(conid, csv);
	}	

	/** Top-level message, return prices for debugging */
	private void getAllPrices() throws RefException {
		
		// build the json response   // we could reuse this and just update the prices each time
		JsonObject whole = new JsonObject();

		for (Object obj : m_main.stocks().stocks() ) {
			Stock stk = (Stock)obj;

			JsonObject single = new JsonObject();
			single.put( "bid", round( stk.prices().bid() ) );
			single.put( "ask", round( stk.prices().ask() ) );
			single.put( "last", round( stk.prices().last() ) );
			single.put( "time", stk.prices().getFormattedTime() );

			whole.put( stk.get("conid").toString(), single);
		}

		respond(whole);
	}

}
