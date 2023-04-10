package reflection;

import static reflection.Main.log;
import static reflection.Main.require;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.moonstoneid.siwe.SiweMessage;
import com.moonstoneid.siwe.util.Utils;
import com.sun.net.httpserver.HttpExchange;

import fireblocks.Accounts;
import fireblocks.Busd;
import fireblocks.Erc20;
import fireblocks.Rusd;
import fireblocks.StockToken;
import json.MyJsonArray;
import json.MyJsonObject;
import positions.MoralisServer;
import tw.util.S;
import util.LogType;

/** This class handles events from the Frontend, simulating the Backend */
public class BackendTransaction extends MyTransaction {

	public BackendTransaction(Main main, HttpExchange exch) {
		super(main,exch);
	}
	
/** Msg received directly from Frontend via nginx */
	public void backendOrder(boolean whatIf) {
		wrap( () -> {
			require( "POST".equals(m_exchange.getRequestMethod() ), RefCode.INVALID_REQUEST, "order and check-order must be POST"); 
			
			parseMsg();

	   		// some should be written to the log file
	        // String = m_map.get("symbol": "META",
			// String = m_map.getRequiredParam("currency");
			// String smartcontractid = m_map.getRequiredParam("smartcontractid");
			// double spread = m_map.getRequiredDouble("spread"); //??????????
			// double commission = m_map.getRequiredDouble("commission");

//			int conid = m_map.getRequiredInt("conid");
//			double quantity = m_map.getRequiredDouble("quantity");
//			double price = m_map.getRequiredDouble("price");

			String side = m_map.getRequiredParam("action");
			require( side == "buy" || side == "sell", RefCode.INVALID_REQUEST, "Side must be 'buy' or 'sell'");
			m_map.put( "side", side);

			String wallet = m_map.getRequiredParam("wallet_public_key");
			m_map.put( "wallet", wallet); // you can remove this when orders are no longer being passed through the back-end

			order(whatIf, false);
		});
    }

	/** Used by the portfolio section on the dashboard
	 *  We're returning the token positions from the blockchain, not IB positions */
	public void handleReqPositions(String uri) {
		wrap( () -> {
			// get wallet address (last token in URI)
			String address = Util.getLastToken(uri, "/");
			require( Util.isValidAddress(address), RefCode.INVALID_REQUEST, "Wallet address is invalid");
			
			// query positions from Moralis
			setTimer( Main.m_config.timeout(), () -> timedOut( "request for toekn positions timed out") );
			MyJsonArray positions = MoralisServer.reqPositions(address);
			
			JSONArray retVal = new JSONArray();
			
			for (MyJsonObject position : positions) {
				HashMap stock = m_main.getStockByTokAddr( position.getString("token_address") );

				double balance = Erc20.fromBlockchain(
						(String)position.getString("balance"), 
						StockToken.stockTokenDecimals);
				
				if (stock != null && balance >= Main.m_config.minTokenPosition() ) {
					JSONObject resp = new JSONObject();
					resp.put("conId", stock.get("conid") );
					resp.put("symbol", stock.get("symbol") );
					resp.put("price", getPrice(stock) );
					resp.put("quantity", balance); 
					retVal.add(resp);
				}
			}
			
			respond( new Json(retVal) );
		});
	}

	/** Handle a Backend-style event. Conid is last parameter */
	public void handleGetStockWithPrice(String uri) {
		wrap( () -> {
			int conid = Integer.valueOf( Util.getLastToken(uri, "/") );
			respond( new Json( m_main.getStock(conid) ) );
		});
	}
	
	/** Redeem (sell) RUSD 
	 * @param uri */
	public void handleRedeem(String uri) {
		wrap( () -> {
			String userAddr = Util.getLastToken(uri, "/");
			require( Util.isValidAddress(userAddr), RefCode.INVALID_REQUEST, "Wallet address is invalid");

			Rusd rusd = Main.m_config.newRusd();
			Busd busd = Main.m_config.newBusd();

			setTimer( Main.m_config.timeout(), () -> timedOut( "redemption request timed out") );

			double rusdPos = rusd.reqPosition(userAddr);
			require( rusdPos > 0, RefCode.INSUFFICIENT_FUNDS, "No RUSD in user wallet to redeem");
			
			log( LogType.REDEEM, "%s is selling %s RUSD", userAddr, rusdPos);

			double busdPos = busd.reqPosition( Accounts.instance.getAddress("RefWallet") );
			if (busdPos >= rusdPos) {
				rusd.sellRusd(userAddr, Main.m_config.newBusd(), rusdPos);
				// wait for completion
//				respond( code, RefCode.OK);  deal w/ responde codes and messages 
//				respond( )
			}
			else {
				String str = String.format( 
						"Insufficient stablecoin in RefWallet for RUSD redemption  \nwallet=%s  requested=%s  have=%s  need=%s",
						userAddr, rusdPos, busdPos, (rusdPos - busdPos) );
				alert( "MOVE FUNDS NOW TO REDEEM RUSD", str);
				throw new Exception( str);  // will create log entry with ERROR code
			}
		});
	}

	private static double getPrice(HashMap stock) {
		Double bid = (Double)stock.get("bid");
		Double ask = (Double)stock.get("ask");
		
		if (bid != null && ask != null) {
			return (bid + ask) / 2;
		}
		if (bid != null) {
			return bid;
		}
		if (ask != null) {
			return ask;
		}
		return 0;
	}

	/*
	 * signed message looks like this:
	{
	"signature":"0xb704d00b0bd15e789e26e566d668ee03cca287218bd6110e01334f40a38d9a8377eece1d958fff7a72a5b669185729a18c1a253fd0ddcf9711764a761d60ba821b",
	"message":{
		"domain":"usedapp-docs.netlify.app",
		"address":"0xb95bf9C71e030FA3D8c0940456972885DB60843F",
		"statement":"Sign in with Ethereum.",
		"uri":"https://usedapp-docs.netlify.app",
		"version":"1",
		"chainId":5,
		"nonce":"s6BSC0iXede6QSw5D",
		"issuedAt":"2023-04-10T14:40:03.878Z"
	}
	 */

	/** Frontend requests nonce to build SIWE message */
	public void handleSiweInit() {
		S.out( "Handling /siwi/init");
		respond( "nonce", Utils.generateNonce() );
	}
	
	/** Frontend send message and signature; we should verify */
	public void handleSiweSignin() {
		wrap( () -> {
			S.out( "Handling /siwe/signin");
			
			parseMsg();

			String signature = m_map.get( "signature");
			MyJsonObject msgObj = MyJsonObject.parse( m_map.get("message") );
			SiweMessage siweMsg = msgObj.getSiweMessage();
			
			siweMsg.verify(siweMsg.getDomain(), siweMsg.getNonce(), signature);  // we should not take the domain and nonce from here. pas
			
			JSONObject signedMsg = new JSONObject();
			signedMsg.put( "signature", signature);
			signedMsg.put( "message", msgObj);

			String cookie = String.format( "__Host_authToken%s%s=%s",
					siweMsg.getAddress(), 
					siweMsg.getChainId(), 
					URLEncoder.encode(signedMsg.toString() ) 
			);
			
			HashMap<String,String> headers = new HashMap<>();
			headers.put( "Set-Cookie", cookie);
			
			respond( Util.toJsonMsg( code, "OK"), headers);
		});
	}
	
	/** This is a keep-alive, nothing to do */
	public void handleSiweMe() {
		wrap( () -> {
			S.out( "Handling /siwe/me");
			
			S.out( "headers");
			S.out(m_exchange.getRequestHeaders());			

			List<String> headers = m_exchange.getRequestHeaders().get("Cookie");
			Main.require(headers.size() == 1, RefCode.INVALID_REQUEST, "Wrong number of Cookies in header: " + headers.size() );
			
			String cookie = headers.get(0);
			Main.require( cookie.split("=").length == 2, RefCode.INVALID_REQUEST, "Invalid SIWE cookie " + cookie);
			
			MyJsonObject signedMsg = MyJsonObject.parse(
					URLDecoder.decode( cookie.split("=")[1])    // you should URL-decode this first
			);
			
			String signature = signedMsg.getString("signature");
			MyJsonObject msg = signedMsg.getObj("message");
			
			SiweMessage obj = msg.getSiweMessage();
			
			signedMsg.get("address");
			
			
			JSONObject resp = new JSONObject();
			resp.put("loggedIn", true);
			resp.put("message", tempMsg);
//			for (Entry<String, List<String>> a : headers.entrySet() ) {
//				S.out( a);
//			}
			
			respond("loggedIn", true, "message", resp.toString() );
		});

	}

}
