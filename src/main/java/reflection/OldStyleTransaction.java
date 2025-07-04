package reflection;

import static reflection.Main.require;

import java.util.HashSet;

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

import common.Util;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab;
import tw.util.S;

public class OldStyleTransaction extends MyTransaction {
	enum MsgType {
		disconnect,
		dump,
		getTradingHours,
		getDescription,
		getPositions,
		getCashBal,
		refreshConfig,
		terminate,
		testAlert,
		checkForSignups
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
			case checkForSignups:
				onCheckForSignups();
				break;
		}
	}

	private void onCheckForSignups() throws Exception {
		String WalletPub = "wallet_public_key"; // make a constant
		
		Tab tab = NewSheet.getTab( NewSheet.Prefinery, "Sign-ups from Pete");
		
		GTable emails = new GTable( tab, "email", null, false);
		
		tab.startTransaction();
		int numAdded = 0;
		
		for (JsonObject signup : Main.m_config.sqlQuery("select * from signup") ) {
			
			String email = signup.getString("email").toLowerCase();

			if (S.isNotNull( email) && !emails.containsKey( email) ) {
				tab.insert( signup);
				emails.putLocal( email, "");
				numAdded++;
			}
		}
		
		tab.commit();

		//--------------------------------
		
		tab = NewSheet.getTab( NewSheet.Prefinery, "KYC");

		// build set of wallets on KYC page
		HashSet<String> set = new HashSet<>();
		for (var user : tab.queryToJson( Util.toArray( WalletPub) ) ) {
			set.add( user.getString( WalletPub) ); 
		}
		
		tab.startTransaction();
		
		for (var user : Main.m_config.sqlQuery(
				"select wallet_public_key, email, first_name, last_name, kyc_status, persona_response " +
				"from users where kyc_status = 'VERIFIED' or kyc_status = 'completed'") ) {
			
			if (!set.contains( user.getString( WalletPub) ) ) {
				S.out( "inserting into kyc: " + user);
				tab.insert( user);
			}
		}
		
		tab.commit();
		
		respond( code, RefCode.OK, "added", numAdded);
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

}
