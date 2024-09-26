package siwe;

import java.util.HashMap;

import org.json.simple.JSONAware;

import com.sun.net.httpserver.HttpExchange;

import http.BaseTransaction;
import http.MyServer;
import reflection.BackendTransaction;
import tw.util.S;

public class Mock {
//	String invalid = """
//			{ "code": "INVALID_REQUEST", "message": "The 'msg' parameter is missing or the URI path is invalid", "error": { "message": "The 'msg' parameter is missing or the URI path is invalid" }, "statusCode": 400 }""";
	String invalid = """
			{ "code": "OK" }""";
	
	String emptyArray = "[]";
	
	String myWallet = """
			{ "refresh": 30000, "tokens": [ { "buttonTooltip": "Click here to exchange your RUSD for BUSD. The BUSD can then be converted to cash on other platforms", "balance": 55.555, "name": "RUSD", "tooltip": "RUSD is the native stablecoin of the Reflection platform. It is what you receive when you sell a stock token, and likewise it can be used to buy more stock tokens. It is backed 1-to-1 with a combination of US dollars and USDC and can be redeemed at no cost for USDC at any time." }, { "buttonTooltip": "Click here to approve your BUSD for use on the Reflection system. You can give approval at the time an order is placed, but approving now makes for a smoother trading experience", "stablecoin": true, "balance": 22.222, "approvedBalance": 33.333, "name": "BUSD", "tooltip": "BUSD is a popular stablecoin that can be used to purchase Reflection stock tokens" }, { "balance": 44.4444, "name": "ETH", "tooltip": "ETH is the native currency of the Goerli network. You will need a little bit (less than $1 worth) to approve your Reflection stock token purchases or to transfer tokens on the Polygon network" } ] }""";
	
	String positions = """
			[ { "symbol": "AMD (Advanced Micro Devices)", "quantity": 1, "price": 150.04, "conId": "4391" }, { "symbol": "COIN (Coinbase)", "quantity": 2.0868, "price": 161.935, "conId": "481691285" }, { "symbol": "COST (Costco)", "quantity": 1, "price": 899.42, "conId": "272997" }, { "symbol": "FSLR (First Solar)", "quantity": 1, "price": 238.42, "conId": "41622169" }, { "symbol": "GME (GameStop)", "quantity": 1, "price": 20.125, "conId": "36285627" }, { "symbol": "GOOG (Google)", "quantity": 1.2702, "price": 159.805, "conId": "208813720" }, { "symbol": "IBIT (Bitcoin ETF)", "quantity": 5.5586, "price": 34.395, "conId": "677037673" }, { "symbol": "INDY (Nifty-50 ETF)", "quantity": 3.3552, "price": 55.445, "conId": "70333958" }, { "symbol": "INTC (Intel)", "quantity": 2.162, "price": 21.435, "conId": "270639" }, { "symbol": "META (Facebook)", "quantity": 0.7709, "price": 534.275, "conId": "107113386" }, { "symbol": "MMYT (Make My Trip)", "quantity": 3, "price": 104.37, "conId": "77879741" }, { "symbol": "MRNA (Moderna)", "quantity": 1.168, "price": 71.985, "conId": "344809106" }, { "symbol": "NFLX (Netflix)", "quantity": 1, "price": 705.33, "conId": "15124833" }, { "symbol": "NVDA (Nvidia)", "quantity": 0.747, "price": 115.315, "conId": "4815747" }, { "symbol": "PYPL (Paypal)", "quantity": 2, "price": 71.405, "conId": "199169591" }, { "symbol": "QQQ (Nasdaq 100 ETF)", "quantity": 0.0416, "price": 472.735, "conId": "320227571" }, { "symbol": "REGN (Regeneron)", "quantity": 0.1552, "price": 1140.025, "conId": "273733" }, { "symbol": "SBUX (Starbucks)", "quantity": 0.131, "price": 96.09, "conId": "274105" }, { "symbol": "TSLA (Tesla)", "quantity": 0.2289, "price": 227.625, "conId": "76792991" }, { "symbol": "ZM (Zoom)", "quantity": 0.4217, "price": 68.04, "conId": "361181057" } ]""";
	
	String sysConfig = """
			{ "max_order_size": 5000, "sell_spread": 0.005, "buy_spread": 0.005, "min_order_size": 3, "non_kyc_max_order_size": 600, "price_refresh_interval": 30, "commission": 0.25 }""";
	
	String config = """
			{ "tds_tooltip_text": "The Indian government requires that we collect 1% TDS on all crypto transactions.", "medium_link": "https://medium.com/@reflectiontradingrusd", "twitter_link": "https://twitter.com/Reflection_toks", "unsupported_country_message": "We're sorry, but Reflection is not yet supported in your area", "support_link": "https://t.me/josh_reflection", "whitepaper_text": "Welcome to Reflection, the only site where you can trade stock tokens that are 100% backed by shares of real stock", "telegram_link": "https://t.me/ReflectionTrading", "whitepaper_link": "https://whitepaper.reflection.trading/", "discord_link": "https://discord.gg/JX4RfPH8fK" }""";
	
	String trans = """
			[ { "symbol": "NVDA (Nvidia)", "country": "NG", "tds": 0, "quantity": 4.2638, "rounded_quantity": 4, "blockchain_hash": "0x8663aad2fd2fdbc6a5e8f7d475f467a62a7f49a53fb768470add11a351491b85", "ref_code": null, "ip_address": "105.112.116.250", "fireblocks_id": null, "uid": "LPPMJVCH", "price": 116.92, "action": "Buy", "wallet_public_key": "0xb42d5513c14151d99d9eb6acdddac9257ad8fbef", "conid": 4815747, "commission": 0.25, "currency": "RUSD", "order_id": null, "status": "COMPLETED", "timestamp": 1726553834 }, { "symbol": "AAPL (Apple)", "country": "PK", "tds": 0, "quantity": 0.0919, "rounded_quantity": 1, "blockchain_hash": "0x1ad59627fadf0e49428d62b5650ecafdb88219cb30c978f754873eb9971d42f2", "ref_code": null, "ip_address": "39.53.217.51", "fireblocks_id": null, "uid": "KYFXASDF", "price": 217.29, "action": "Buy", "wallet_public_key": "0xf595ecdf1b3be287b05b3e20f2e95a471bfb6be4", "conid": 265598, "commission": 0.25, "currency": "RUSD", "order_id": null, "status": "COMPLETED", "timestamp": 1726513868 } ]""";
	
	String pos = """
			[ { "symbol": "AMD (Advanced Micro Devices)", "quantity": 1, "price": 150.71, "conId": "4391" }, { "symbol": "COIN (Coinbase)", "quantity": 2.0868, "price": 162.62, "conId": "481691285" }, { "symbol": "COST (Costco)", "quantity": 1, "price": 897.39, "conId": "272997" }, { "symbol": "FSLR (First Solar)", "quantity": 1, "price": 240.085, "conId": "41622169" }, { "symbol": "GME (GameStop)", "quantity": 1, "price": 20.19, "conId": "36285627" }, { "symbol": "GOOG (Google)", "quantity": 1.2702, "price": 160.37, "conId": "208813720" } ]""";
	
	String stocks = """
			[ { "smartcontractid": "0xad7244b5be15e038f592f3748b4eeaa67966a1fb", "allow": "All", "symbol": "AAPL (Apple)", "tokenSymbol": "AAPL.r", "last": 216.75, "endDate": "", "description": "Apple Inc - Maker of iPhone, iPad, MacBook, and AirPods", "type": "Stock", "tradingView": "NASDAQ:AAPL", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 216.76, "conid": "265598", "exchange": "SMART", "convertsToAddress": "", "bid": 216.7, "startDate": "", "isHot": true }, { "smartcontractid": "0x7e34f86085fe9cab36083364e1d26a852eaeeae1", "allow": "", "symbol": "ADBE (Adobe)", "tokenSymbol": "ADBE.r", "last": 515.03, "endDate": "", "description": "Adobe Inc - Leading producer of computer graphics software; creator of Photoshop and Illustrator", "type": "Stock", "tradingView": "NASDAQ:ADBE", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 515.3, "conid": "265768", "exchange": "SMART", "convertsToAddress": "", "bid": 514.98, "startDate": "", "isHot": true }, { "smartcontractid": "0xf3206bd1e31b071f93d5b46abf28c8fa52a124ae", "allow": "", "symbol": "AMD (Advanced Micro Devices)", "tokenSymbol": "AMD.r", "last": 150.71, "endDate": "", "description": "Advanced Micro Devices Inc - One of the top computer chip makers in the world", "type": "Stock", "tradingView": "NASDAQ:AMD", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 150.74, "conid": "4391", "exchange": "SMART", "convertsToAddress": "", "bid": 150.68, "startDate": "", "isHot": true }, { "smartcontractid": "0x637245ce1c35abdcefc3cc33074562312ad1112d", "allow": "", "symbol": "AMZN (Amazon)", "tokenSymbol": "AMZN.r", "last": 187.08, "endDate": "", "description": "Amazon.com Inc - Largest online retailer in the world", "type": "Stock", "tradingView": "NASDAQ:AMZN", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 187.08, "conid": "3691937", "exchange": "SMART", "convertsToAddress": "", "bid": 187, "startDate": "", "isHot": true } ]""";
	
	String liveOrders = """
			{ "orders": [], "messages": [] }
			""";
	
	String stockWithPrice = """  
			{ "smartcontractid": "0x2fa250dc78dce5d6031f30fb8b45a66e986b6551", "allow": "", "symbol": "FSLR (First Solar)", "tokenSymbol": "FSLR.r", "last": 242.95, "endDate": "", "description": "First Solar Inc - The top producer of solar panels and photovoltaic power plants in the world", "type": "Stock", "tradingView": "NASDAQ:FSLR", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 244.94, "conid": "41622169", "exchange": "SMART", "convertsToAddress": "", "bid": 242.75, "startDate": "", "isHot": true }
			""";  // first solar
	
	String onrampQuote = """
			{"status":1,"code":200,"data":{"fromCurrency":"EUR","toCurrency":"USDT","toAmount":"3252.52","fromAmount":"3000","rate":"0.92","fees":[{"type":"fiat","onrampFee":"7.5","clientFee":"0","gatewayFee":"0","gasFee":"0.18"}]}}
			""";
			
	String onrampConvert = """
			{ "createdAt" : "2024-09-26 16:16:01", "bank" : "somebank", "amount" : 3000, "code" : "OK", "iban" : "TR700005901010130101011089", "name" : "somebank name", "type" : "TRY_BANK_TRANSFER", "message" : "The transaction has been accepted" }
			""";
			
	HashMap<String,String> map = new HashMap<>(); // key must be lower case

	public static void main(String[] args) {
		try {
			Thread.currentThread().setName("Mock");

			int port = args.length == 1 ? Integer.parseInt( args[0]) : 8383;
			new Mock(port);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(2);  // we need this because listening on the port will keep the app alive
		}
	}

	public Mock(int port) throws Exception {
		map.put( "mywallet", myWallet);
		map.put( "positions-new", positions);
		map.put( "system-configurations", sysConfig); 
		map.put( "configurations", config);
		map.put( "crypto-transactions", trans);
		map.put( "get-all-stocks", stocks);
		map.put( "live-orders", liveOrders);
		map.put( "working-orders", liveOrders);
		map.put( "hot-stocks", emptyArray);
		map.put( "get-all-stocks", emptyArray);
		map.put( "get-stocks-with-prices", emptyArray);
		map.put( "all-live-orders", emptyArray);
		map.put( "faqs", emptyArray);
		map.put( "get-stock-with-price", stockWithPrice); // first solar
		map.put( "onramp-get-quote", onrampQuote); // first solar
		map.put( "onramp-convert", onrampConvert); // first solar
		
		BaseTransaction.setDebug( true);
				
		// start Siwe thread to periodically save siwe cookies
		SiweTransaction.startThread();
		MyServer.listen( port, 5, server -> {
			server.createContext("/siwe/init", exch -> new SiweTransaction( exch).handleSiweInit() );
			server.createContext("/siwe/signin", exch -> new SiweTransaction( exch).handleSiweSignin() );
			server.createContext("/siwe/me", exch -> new SiweTransaction( exch).handleSiweMe() );
			server.createContext("/siwe/signout", exch -> new SiweTransaction( exch).handleSiweSignout() );

			// for testing wagmi server
//			server.createContext("/siwe/nonce", exch -> new SiweTransaction( exch).handleSiweInit() );
//			server.createContext("/siwe/verify", exch -> new SiweTransaction( exch).handleSiweSignin() );
//			server.createContext("/siwe/session", exch -> new SiweTransaction( exch).handleSiweMe() );

			server.createContext("/api/siwe/init", exch -> new SiweTransaction( exch).handleSiweInit() );
			server.createContext("/api/siwe/signin", exch -> new SiweTransaction( exch).handleSiweSignin() );
			server.createContext("/api/siwe/me", exch -> new SiweTransaction( exch).handleSiweMe() );
			server.createContext("/api/siwe/signout", exch -> new SiweTransaction( exch).handleSiweSignout() );
			
			
			server.createContext("/", exch -> dummy( exch) );
		});
	}

	private void dummy(HttpExchange exch) {
		BaseTransaction t = new BaseTransaction( exch, false);
		String key = t.getPostApiToken();
		String val = S.notNull( map.get( key), invalid);
		
		if (val == invalid) {
			S.out( "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		}
		
		t.wrap( () -> {
			t.respond( JSONAware.parse( val) );
		});
	}
}
