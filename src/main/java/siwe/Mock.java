package siwe;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JSONAware;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import http.BaseTransaction;
import http.MyServer;
import tw.util.S;

public class Mock {

    //	String invalid = """
//			{ "code": "INVALID_REQUEST", "message": "The 'msg' parameter is missing or the URI path is invalid", "error": { "message": "The 'msg' parameter is missing or the URI path is invalid" }, "statusCode": 400 }""";
	String invalid = """
			{ "code": "OK" }""";
	
	String emptyArray = "[]";
	
	String myWallet = """
			{ "refresh" : 5000, "tokens" : [ { "buttonTooltip" : "Click here to exchange your RUSD for stablecoin. The stablecoin can then be converted to cash on other platforms.", "balance" : 10852.370764, "name" : "RUSD", "tooltip" : "RUSD is the native stablecoin of the Reflection platform. It is what you receive when you sell a stock token, and likewise it can be used to buy more stock tokens. It is backed 1-to-1 with a combination of US dollars and other US-dollar-backed stablecoins and can be redeemed at no cost at any time." }, { "buttonTooltip" : "Click here to approve your stablecoin for use on the Reflection system. You can give approval at the time an order is placed, but approving now makes for a smoother trading experience", "stablecoin" : true, "balance" : 3353.482698, "approvedBalance" : 333, "name" : "USDC", "tooltip" : "This is the stablecoin that can be used to purchase Reflection stock tokens" }, { "balance" : 17521630.35548528, "name" : "PLS", "tooltip" : "This is the native token of your selected blockchain. You only need native token to 'approve' your first transaction. After that, Reflection pays all the gas fees." } ] } """;

	String  signup = """
			{ "refresh": 30000, "tokens": [ { "buttonTooltip": "Click here to exchange your RUSD for BUSD. The BUSD can then be converted to cash on other platforms", "balance": 55.555, "name": "RUSD", "tooltip": "RUSD is the native stablecoin of the Reflection platform. It is what you receive when you sell a stock token, and likewise it can be used to buy more stock tokens. It is backed 1-to-1 with a combination of US dollars and USDC and can be redeemed at no cost for USDC at any time." }, { "buttonTooltip": "Click here to approve your BUSD for use on the Reflection system. You can give approval at the time an order is placed, but approving now makes for a smoother trading experience", "stablecoin": true, "balance": 22.222, "approvedBalance": 33.333, "name": "BUSD", "tooltip": "BUSD is a popular stablecoin that can be used to purchase Reflection stock tokens" }, { "balance": 44.4444, "name": "ETH", "tooltip": "ETH is the native currency of the Goerli network. You will need a little bit (less than $1 worth) to approve your Reflection stock token purchases or to transfer tokens on the Polygon network" } ] }""";
	
	String positions = """
			[ { "symbol": "AMD (Advanced Micro Devices)", "quantity": 1, "price": 150.044, "pnl": 15.71, "conId": "4391" }, { "symbol": "COIN (Coinbase)", "quantity": 2.0868, "pnl": 5.71, "price": 161.935, "conId": "481691285" }, { "symbol": "COST (Costco)", "quantity": 1, "pnl": 3.84, "price": 899.42, "conId": "272997" }, { "symbol": "FSLR (First Solar)", "pnl": 12.8, "quantity": 1, "price": 238.42, "pnl": 15, "conId": "41622169" }, { "symbol": "GME (GameStop)", "quantity": 1, "price": 20.125, "conId": "36285627" }, { "symbol": "GOOG (Google)", "quantity": 1.2702, "price": 159.805, "conId": "208813720" }, { "symbol": "IBIT (Bitcoin ETF)", "quantity": 5.5586, "price": 34.395, "conId": "677037673" }, { "symbol": "INDY (Nifty-50 ETF)", "quantity": 3.3552, "price": 55.445, "conId": "70333958" }, { "symbol": "INTC (Intel)", "quantity": 2.162, "price": 21.435, "conId": "270639" }, { "symbol": "META (Facebook)", "quantity": 0.7709, "price": 534.275, "conId": "107113386" }, { "symbol": "MMYT (Make My Trip)", "quantity": 3, "price": 104.37, "conId": "77879741" }, { "symbol": "MRNA (Moderna)", "quantity": 1.168, "price": 71.985, "conId": "344809106" }, { "symbol": "NFLX (Netflix)", "quantity": 1, "price": 705.33, "conId": "15124833" }, { "symbol": "NVDA (Nvidia)", "quantity": 0.747, "price": 115.315, "conId": "4815747" }, { "symbol": "PYPL (Paypal)", "quantity": 2, "price": 71.405, "conId": "199169591" }, { "symbol": "QQQ (Nasdaq 100 ETF)", "quantity": 0.0416, "price": 472.735, "conId": "320227571" }, { "symbol": "REGN (Regeneron)", "quantity": 0.1552, "price": 1140.025, "conId": "273733" }, { "symbol": "SBUX (Starbucks)", "quantity": 0.131, "price": 96.09, "conId": "274105" }, { "symbol": "TSLA (Tesla)", "quantity": 0.2289, "price": 227.625, "conId": "76792991" }, { "symbol": "ZM (Zoom)", "quantity": 0.4217, "price": 68.04, "conId": "361181057" } ]""";
	
	String sysConfig = """
			{ "max_order_size": 5000, "sell_spread": 0.005, "buy_spread": 0.005, "min_order_size": 3, "non_kyc_max_order_size": 600, "price_refresh_interval": 30, "commission": 0.25 }""";
	
	String config = """
			{ "tds_tooltip_text": "The Indian government requires that we collect 1% TDS on all crypto transactions.", "medium_link": "https://medium.com/@reflectiontradingrusd", "twitter_link": "https://twitter.com/Reflection_toks", "unsupported_country_message": "We're sorry, but Reflection is not yet supported in your area", "support_link": "https://t.me/josh_reflection", "whitepaper_text": "Welcome to Reflection, the only site where you can trade stock tokens that are 100% backed by shares of real stock", "telegram_link": "https://t.me/ReflectionTrading", "whitepaper_link": "https://whitepaper.reflection.trading/", "discord_link": "https://discord.gg/JX4RfPH8fK" }""";
	
	String trans = """
			[ { "symbol": "NVDA (Nvidia)", "country": "NG", "tds": 0, "quantity": 4.2638, "rounded_quantity": 4, "blockchain_hash": "0x8663aad2fd2fdbc6a5e8f7d475f467a62a7f49a53fb768470add11a351491b85", "ref_code": null, "ip_address": "105.112.116.250", "fireblocks_id": null, "uid": "LPPMJVCH", "price": 116.92, "action": "Buy", "wallet_public_key": "0xb42d5513c14151d99d9eb6acdddac9257ad8fbef", "conid": 4815747, "commission": 0.25, "currency": "RUSD", "order_id": null, "status": "COMPLETED", "timestamp": 1726553834 }, { "symbol": "AAPL (Apple)", "country": "PK", "tds": 0, 
			"quantity": 0.0919, 
			"rounded_quantity": 1, 
			"chainid": 11155111, 
			"blockchain_hash": "0x1ad59627fadf0e49428d62b5650ecafdb88219cb30c978f754873eb9971d42f2", 
			"ref_code": null, 
			"ip_address": "39.53.217.51", 
			"fireblocks_id": null, "uid": "KYFXASDF", "price": 217.29, "action": "Buy", "wallet_public_key": "0xf595ecdf1b3be287b05b3e20f2e95a471bfb6be4", "conid": 265598, "commission": 0.25, "currency": "RUSD", "order_id": null, "status": "COMPLETED", "timestamp": 1726513868 } ]""";
	
	String pos = """
			[ { "symbol": "AMD (Advanced Micro Devices)", "quantity": 1, "price": 150.71, "conId": "4391" }, { "symbol": "COIN (Coinbase)", "quantity": 2.0868, "price": 162.62, "conId": "481691285" }, { "symbol": "COST (Costco)", "quantity": 1, "price": 897.39, "conId": "272997" }, { "symbol": "FSLR (First Solar)", "quantity": 1, "price": 240.085, "conId": "41622169" }, { "symbol": "GME (GameStop)", "quantity": 1, "price": 20.19, "conId": "36285627" }, { "symbol": "GOOG (Google)", "quantity": 1.2702, "price": 160.37, "conId": "208813720" } ]""";
	
	String watchList = """
			[
			{ 
			"bid": 1,
			"ask": 2,
			"symbol": "AAPL",
			"conid": "265598"
			},
			{ 
			"bid": 1,
			"ask": 2,
			"symbol": "ABC",
			"conid": "8314"
			}
			]			
			""";
	
	String stocks = """
			[ { "smartcontractid": "0xad7244b5be15e038f592f3748b4eeaa67966a1fb", "allow": "All", "symbol": "AAPL (Apple)", "tokenSymbol": "AAPL.r", "last": 216.75, "endDate": "", "description": "Apple Inc - Maker of iPhone, iPad, MacBook, and AirPods", "type": "Stock", "tradingView": "NASDAQ:AAPL", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 216.76, "conid": "265598", "exchange": "SMART", "convertsToAddress": "", "bid": 216.7, "startDate": "", "isHot": true }, { "smartcontractid": "0x7e34f86085fe9cab36083364e1d26a852eaeeae1", "allow": "", "symbol": "ADBE (Adobe)", "tokenSymbol": "ADBE.r", "last": 515.03, "endDate": "", "description": "Adobe Inc - Leading producer of computer graphics software; creator of Photoshop and Illustrator", "type": "Stock", "tradingView": "NASDAQ:ADBE", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 515.3, "conid": "265768", "exchange": "SMART", "convertsToAddress": "", "bid": 514.98, "startDate": "", "isHot": true }, { "smartcontractid": "0xf3206bd1e31b071f93d5b46abf28c8fa52a124ae", "allow": "", "symbol": "AMD (Advanced Micro Devices)", "tokenSymbol": "AMD.r", "last": 150.71, "endDate": "", "description": "Advanced Micro Devices Inc - One of the top computer chip makers in the world", "type": "Stock", "tradingView": "NASDAQ:AMD", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 150.74, "conid": "4391", "exchange": "SMART", "convertsToAddress": "", "bid": 150.68, "startDate": "", "isHot": true }, { "smartcontractid": "0x637245ce1c35abdcefc3cc33074562312ad1112d", "allow": "", "symbol": "AMZN (Amazon)", "tokenSymbol": "AMZN.r", "last": 187.08, "endDate": "", "description": "Amazon.com Inc - Largest online retailer in the world", "type": "Stock", "tradingView": "NASDAQ:AMZN", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 187.08, "conid": "3691937", "exchange": "SMART", "convertsToAddress": "", "bid": 187, "startDate": "", "isHot": true } ]""";
	
	String liveOrders = """
			{ "orders": [], "messages": [] }""";
	
	String stockWithPrice = """  
			{ "smartcontractid": "0x2fa250dc78dce5d6031f30fb8b45a66e986b6551", "allow": "", "symbol": "FSLR (First Solar)", "tokenSymbol": "FSLR.r", "last": 242.95, "endDate": "", "description": "First Solar Inc - The top producer of solar panels and photovoltaic power plants in the world", "type": "Stock", "tradingView": "NASDAQ:FSLR", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 244.94, "conid": "41622169", "exchange": "SMART", "convertsToAddress": "", "bid": 242.75, "startDate": "", "isHot": true }""";  // first solar
	
	String onrampQuote = """
			{"recAmt": 67.89}""";

	String fundWallet = """
			{"code": "OK"}""";

	String profile = """
			{"first_name": "peter", "last_name": "spiro"} """;
	
	String showFaucet = """
			{ "code": "OK", "amount": 123 }""";
	
	String turnFaucet = """
			{ "code": "OK", "message": "Your wallet has been funded!" }""";
	
	String hotStocks = """
			[ { "smartcontractid": "0xad7244b5be15e038f592f3748b4eeaa67966a1fb", "allow": "All", "symbol": "AAPL (Apple)", "tokenSymbol": "AAPL.r", "last": 235.83, "endDate": "", "description": "Apple Inc - Maker of iPhone, iPad, MacBook, and AirPods", "type": "Stock", "tradingView": "NASDAQ:AAPL", "convertsToAmt": 0, "is24hour": true, "ask": 235.84, "conid": "265598", "exchange": "SMART", "convertsToAddress": "", "bid": 235.82, "startDate": "", "isHot": true }, { "smartcontractid": "0x7e34f86085fe9cab36083364e1d26a852eaeeae1", "allow": "", "symbol": "ADBE (Adobe)", "tokenSymbol": "ADBE.r", "last": 499.5, "endDate": "", "description": "Adobe Inc - Leading producer of computer graphics software; creator of Photoshop and Illustrator", "type": "Stock", "tradingView": "NASDAQ:ADBE", "convertsToAmt": 0, "is24hour": true, "ask": 499.66, "conid": "265768", "exchange": "SMART", "convertsToAddress": "", "bid": 498.92, "startDate": "", "isHot": true }, { "smartcontractid": "0xf3206bd1e31b071f93d5b46abf28c8fa52a124ae", "allow": "", "symbol": "AMD (Advanced Micro Devices)", "tokenSymbol": "AMD.r", "last": 156.56, "endDate": "", "description": "Advanced Micro Devices Inc - One of the top computer chip makers in the world", "type": "Stock", "tradingView": "NASDAQ:AMD", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 156.62, "conid": "4391", "exchange": "SMART", "convertsToAddress": "", "bid": 156.58, "startDate": "", "isHot": true }, { "smartcontractid": "0x637245ce1c35abdcefc3cc33074562312ad1112d", "allow": "", "symbol": "AMZN (Amazon)", "tokenSymbol": "AMZN.r", "last": 188.48, "endDate": "", "description": "Amazon.com Inc - Largest online retailer in the world", "type": "Stock", "tradingView": "NASDAQ:AMZN", "convertsToAmt": 0, "is24hour": true, "ask": 188.49, "conid": "3691937", "exchange": "SMART", "convertsToAddress": "", "bid": 188.48, "startDate": "", "isHot": true }, { "smartcontractid": "0xb4806b6c86b1f7c9d7291bbb992512b58b5953e2", "allow": "", "symbol": "AVGO (Broadcom)", "tokenSymbol": "AVGO.r", "last": 181.51, "endDate": "", "description": "Broadcom Inc - Global supplier of semiconductors and communications-related software", "type": "Stock", "tradingView": "NASDAQ:AVGO", "convertsToAmt": 0, "is24hour": true, "ask": 181.56, "conid": "313130367", "exchange": "SMART", "convertsToAddress": "", "bid": 181.5, "startDate": "", "isHot": true }, { "smartcontractid": "0x20a1b004df9f5463dd65a6598d9fe451f8b3e494", "allow": "", "symbol": "COIN (Coinbase)", "tokenSymbol": "COIN.r", "last": 215.12, "endDate": "", "description": "Coinbase Global Inc - One of the largest and most respected crypto exchanges in the world", "type": "Stock", "tradingView": "NASDAQ:COIN", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 215.19, "conid": "481691285", "exchange": "SMART", "convertsToAddress": "", "bid": 215, "startDate": "", "isHot": true }, { "smartcontractid": "0x68486eee94e87b6542ec6864c9056b65f83503fa", "allow": "", "symbol": "COST (Costco)", "tokenSymbol": "COST.r", "last": 890.65, "endDate": "", "description": "Costco Wholesale Corporation - One of the largest consumer warehouse clubs in the world", "type": "Stock", "tradingView": "NASDAQ:COST", "exchangeStatus": "closed", "convertsToAmt": 0, "is24hour": true, "ask": 891.3, "conid": "272997", "exchange": "SMART", "convertsToAddress": "", "bid": 890.57, "startDate": "", "isHot": true }, { "smartcontractid": "0x377e867d2375fd2426f0efea9d4bd91253d2e16b", "allow": "", "symbol": "CSCO (Cisco)", "tokenSymbol": "CSCO.r", "last": 57.02, "endDate": "", "description": "Cisco Systems Inc - One of the leading global suppliers of computer networking hardware and software in the world", "type": "Stock", "tradingView": "NASDAQ:CSCO", "convertsToAmt": 0, "is24hour": true, "ask": 57.02, "conid": "268084", "exchange": "SMART", "convertsToAddress": "", "bid": 57.01, "startDate": "", "isHot": true }, { "smartcontractid": "0x2fa250dc78dce5d6031f30fb8b45a66e986b6551", "allow": "", "symbol": "FSLR (First Solar)", "tokenSymbol": "FSLR.r", "last": 197.4, "endDate": "", "description": "First Solar Inc - The top producer of solar panels and photovoltaic power plants in the world", "type": "Stock", "tradingView": "NASDAQ:FSLR", "convertsToAmt": 0, "is24hour": true, "ask": 197.55, "conid": "41622169", "exchange": "SMART", "convertsToAddress": "", "bid": 197.29, "startDate": "", "isHot": true }, { "smartcontractid": "0xf679c92559da7dbd24b9c26cce08b14f1caf1a79", "allow": "", "symbol": "GME (GameStop)", "tokenSymbol": "GME.r", "last": 20.93, "endDate": "", "description": "GameStop Corp. - The ultimate destination for gamers, offering an expansive selection of video games, cutting-edge electronics, and exclusive collectibles. Also the original 'Meme' stock which made headlines and millionaires!", "type": "Stock", "tradingView": "NYSE:GME", "convertsToAmt": 0, "is24hour": true, "ask": 20.95, "conid": "36285627", "exchange": "SMART", "convertsToAddress": "", "bid": 20.93, "startDate": "", "isHot": true }, { "smartcontractid": "0x4470033bd3cbf4f4f6ac4076b1085f819c7d0844", "allow": "", "symbol": "GOOG (Google)", "tokenSymbol": "GOOG.r", "last": 166.04, "endDate": "", "description": "Alphabet Inc - Owner of Google; top search engine company in the world", "type": "Stock", "tradingView": "NASDAQ:GOOG", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 166.07, "conid": "208813720", "exchange": "SMART", "convertsToAddress": "", "bid": 166.05, "startDate": "", "isHot": true }, { "smartcontractid": "0x11f8b8c15da8e020745490a2de9bc157e27d84b1", "allow": "", "symbol": "IBIT (Bitcoin ETF)", "tokenSymbol": "IBIT.r", "last": 38.44, "endDate": "", "description": "Bitcoin ETF from BlackRock - A great way to invest in Bitcoin", "type": "ETF", "tradingView": "NASDAQ:IBIT", "convertsToAmt": 0, "is24hour": true, "ask": 38.44, "conid": "677037673", "exchange": "SMART", "convertsToAddress": "", "bid": 38.43, "startDate": "", "isHot": true }, { "smartcontractid": "0xf1bbf8d18c5ec8cba3670bc17d38e874d3de17e2", "allow": "", "symbol": "INDY (Nifty-50 ETF)", "tokenSymbol": "INDY.r", "last": 54.2, "endDate": "", "description": "IShares Nifty-50 ETF - Exchange-traded fund containing the top 50 Indian companies", "type": "ETF", "tradingView": "NASDAQ:INDY", "convertsToAmt": 0, "is24hour": true, "ask": 54.22, "conid": "70333958", "exchange": "SMART", "convertsToAddress": "", "bid": 54.11, "startDate": "", "isHot": true }, { "smartcontractid": "0xbd87d38e075bf7a1dfe9005d072b93d3f2b94fa3", "allow": "", "symbol": "INTC (Intel)", "tokenSymbol": "INTC.r", "last": 22.6, "endDate": "", "description": "Intel Corp - One of the top computer chip makers in the world", "type": "Stock", "tradingView": "NASDAQ:INTC", "convertsToAmt": 0, "is24hour": true, "ask": 22.61, "conid": "270639", "exchange": "SMART", "convertsToAddress": "", "bid": 22.6, "startDate": "", "isHot": true }, { "smartcontractid": "0x2bb4bfe474c3a1c0748816ff7fa2376d0f28cc64", "allow": "", "symbol": "KO (Coca-Cola)", "tokenSymbol": "KO.r", "last": 69.92, "endDate": "", "description": "The Coca-Cola Company - An iconic beverage titan known for its world-famous Coca-Cola and an extensive array of irresistible drinks that quench thirsts globally.", "type": "Stock", "tradingView": "NYSE:KO", "convertsToAmt": 0, "is24hour": true, "ask": 69.93, "conid": "8894", "exchange": "SMART", "convertsToAddress": "", "bid": 69.92, "startDate": "", "isHot": true }, { "smartcontractid": "0xcc944c6f60b372866304599223a6fc0a7c9ab7b1", "allow": "", "symbol": "LYFT (Lyft)", "tokenSymbol": "LYFT.r", "last": 13.94, "endDate": "", "description": "Lyft Inc - One of the top online community ridesharing platforms in the world", "type": "Stock", "tradingView": "NASDAQ:LYFT", "exchangeStatus": "closed", "convertsToAmt": 0, "is24hour": true, "ask": 13.94, "conid": "359130923", "exchange": "SMART", "convertsToAddress": "", "bid": 13.93, "startDate": "", "isHot": true }, { "smartcontractid": "0x164cc4b71c733bc475f6a3a0811e506fa42e9918", "allow": "", "symbol": "MCD (McDonald's)", "tokenSymbol": "MCD.r", "last": 316.73, "endDate": "", "description": "McDonald's Corporation - A global fast-food powerhouse serving beloved classics like Big Macs and Happy Meals to millions of loyal customers every day.", "type": "Stock", "tradingView": "NYSE:MCD", "convertsToAmt": 0, "is24hour": true, "ask": 316.93, "conid": "9408", "exchange": "SMART", "convertsToAddress": "", "bid": 316.73, "startDate": "", "isHot": true }, { "smartcontractid": "0x3d204653fd7d20c283f2d5972363be881f7034f7", "allow": "", "symbol": "META (Facebook)", "tokenSymbol": "META.r", "last": 573.47, "endDate": "", "description": "Meta Platforms Inc - Owner of Facebook and Instagram", "type": "Stock", "tradingView": "NASDAQ:META", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 573.5, "conid": "107113386", "exchange": "SMART", "convertsToAddress": "", "bid": 573.29, "startDate": "", "isHot": true }, { "smartcontractid": "0x6d7161852f797425c5abcfebd2dec2cb30031f32", "allow": "", "symbol": "MMYT (Make My Trip)", "tokenSymbol": "MMYT.r", "last": 102.79, "endDate": "", "description": "Make My Trip Limited - India's leading online travel company which revolutionizes the way people travel with a one-stop-shop for all travel needs", "type": "Stock", "tradingView": "NASDAQ:MMYT", "convertsToAmt": 0, "is24hour": true, "ask": 103.21, "conid": "77879741", "exchange": "SMART", "convertsToAddress": "", "bid": 102.72, "startDate": "", "isHot": true }, { "smartcontractid": "0xaaff8b2f28726591b24c25f7e807496753070f17", "allow": "", "symbol": "MRNA (Moderna)", "tokenSymbol": "MRNA.r", "last": 54.17, "endDate": "", "description": "Moderna Inc - The leader in messenger RNA (mRNA) technology and one of the most profitable pharmaceutical companies in the world", "type": "Stock", "tradingView": "NASDAQ:MRNA", "exchangeStatus": "closed", "convertsToAmt": 0, "is24hour": true, "ask": 54.2, "conid": "344809106", "exchange": "SMART", "convertsToAddress": "", "bid": 54.16, "startDate": "", "isHot": true }, { "smartcontractid": "0x2f1213e738404b71cd4f16d04de761ce1bc804cb", "allow": "", "symbol": "MSFT (Microsoft)", "tokenSymbol": "MSFT.r", "last": 417.53, "endDate": "", "description": "Microsoft Corp - Maker of Windows software, owner of ChatGPT", "type": "Stock", "tradingView": "NASDAQ:MSFT", "convertsToAmt": 0, "is24hour": true, "ask": 417.62, "conid": "272093", "exchange": "SMART", "convertsToAddress": "", "bid": 417.54, "startDate": "", "isHot": true }, { "smartcontractid": "0x98f209a8b154e758a812b556dc8f50a21defff91", "allow": "", "symbol": "NFLX (Netflix)", "tokenSymbol": "NFLX.r", "last": 761.3, "endDate": "", "description": "Netflix Inc - Top video streaming company in the world", "type": "Stock", "tradingView": "NASDAQ:NFLX", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 761.65, "conid": "15124833", "exchange": "SMART", "convertsToAddress": "", "bid": 760.91, "startDate": "", "isHot": true }, { "smartcontractid": "0x51cca1091a01d2d002d2e9dab6a2c45a24fcf6e8", "allow": "", "symbol": "NVDA (Nvidia)", "tokenSymbol": "NVDA.r", "last": 141.48, "endDate": "", "description": "NVIDIA Corp - Top maker of artificial intelligence hardware in the world", "type": "Stock", "tradingView": "NASDAQ:NVDA", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 141.48, "conid": "4815747", "exchange": "SMART", "convertsToAddress": "", "bid": 141.47, "startDate": "", "isHot": true }, { "smartcontractid": "0x35372177ff0d22f397b5f820b9b00061e00ccbda", "allow": "", "symbol": "PEP (Pepsico)", "tokenSymbol": "PEP.r", "last": 177.39, "endDate": "", "description": "PepsiCo Inc - A global leader in the food and beverage industry, offering a dynamic portfolio of beloved brands like Pepsi, Lay's, and Gatorade that delight consumers worldwide.", "type": "Stock", "tradingView": "NASDAQ:PEP", "convertsToAmt": 0, "is24hour": true, "ask": 177.45, "conid": "11017", "exchange": "SMART", "convertsToAddress": "", "bid": 177.42, "startDate": "", "isHot": true }, { "smartcontractid": "0x63cf0848627b847dede34ba8d7e9c0368aaef920", "allow": "", "symbol": "PYPL (Paypal)", "tokenSymbol": "PYPL.r", "last": 80.57, "endDate": "", "description": "Paypal Holdings Inc - One of the top online payment processors in the world", "type": "Stock", "tradingView": "NASDAQ:PYPL", "exchangeStatus": "closed", "convertsToAmt": 0, "is24hour": true, "ask": 80.58, "conid": "199169591", "exchange": "SMART", "convertsToAddress": "", "bid": 80.57, "startDate": "", "isHot": true }, { "smartcontractid": "0x55c524d51ba67a794bd9f0eac746724f4e24dd83", "allow": "", "symbol": "QCOM (Qualcomm)", "tokenSymbol": "QCOM.r", "last": 169.61, "endDate": "", "description": "Qualcomm Inc - One of the top producers of wireless communications technology", "type": "Stock", "tradingView": "NASDAQ:QCOM", "convertsToAmt": 0, "is24hour": true, "ask": 169.65, "conid": "273544", "exchange": "SMART", "convertsToAddress": "", "bid": 169.57, "startDate": "", "isHot": true }, { "smartcontractid": "0xc02c202d5df124ed3fc899b00c012b11bd43768a", "allow": "", "symbol": "QQQ (Nasdaq 100 ETF)", "tokenSymbol": "QQQ.r", "last": 496.05, "endDate": "", "description": "Invesco QQQ Trust - Exchange-traded fund containing the top 100 stocks on NASDAQ", "type": "ETF", "tradingView": "NASDAQ:QQQ", "convertsToAmt": 0, "is24hour": true, "ask": 496.06, "conid": "320227571", "exchange": "SMART", "convertsToAddress": "", "bid": 496.05, "startDate": "", "isHot": true }, { "smartcontractid": "0xb5b873b691c6860018ae3430393b23f43b508163", "allow": "", "symbol": "RDDT (Reddit)", "tokenSymbol": "RDDT.r", "last": 79.55, "endDate": "", "description": "Reddit Inc - A dynamic social news aggregation and discussion platform where millions of users share the latest news, viral trends, and engage in vibrant communities.", "type": "Stock", "tradingView": "NYSE:RDDT", "convertsToAmt": 0, "is24hour": true, "ask": 79.61, "conid": "692025016", "exchange": "SMART", "convertsToAddress": "", "bid": 79.55, "startDate": "", "isHot": true }, { "smartcontractid": "0xb2c0aee0cf5aa19c6ac45eae0f2b330445a2556d", "allow": "", "symbol": "REGN (Regeneron)", "tokenSymbol": "REGN.r", "last": 987.8, "endDate": "", "description": "Regeneron Pharmaceuticals Inc - A leader in biotechnology and one of the most profitable pharmaceutical companies in the world", "type": "Stock", "tradingView": "NASDAQ:REGN", "exchangeStatus": "closed", "convertsToAmt": 0, "is24hour": true, "ask": 987.75, "conid": "273733", "exchange": "SMART", "convertsToAddress": "", "bid": 985.52, "startDate": "", "isHot": true }, { "smartcontractid": "0xcfcc0d7a4fc6a6595833b4497c028d64a40c6b81", "allow": "", "symbol": "SBUX (Starbucks)", "tokenSymbol": "SBUX.r", "last": 96.86, "endDate": "", "description": "Starbucks Corporation - The premier coffeehouse brand offering a premium experience with rich, flavorful coffees, teas, and delectable food items in cozy, inviting settings.", "type": "Stock", "tradingView": "NASDAQ:SBUX", "convertsToAmt": 0, "is24hour": true, "ask": 96.84, "conid": "274105", "exchange": "SMART", "convertsToAddress": "", "bid": 96.81, "startDate": "", "isHot": true }, { "smartcontractid": "0x52ca9697dffc190dac961e4aa4453e8c42b8688c", "allow": "", "symbol": "SPY (S&P 500 ETF)", "tokenSymbol": "SPY.r", "endDate": "", "description": "SPDR S&P 500 ETF Trust - Your gateway to owning a slice of America's 500 biggest and most dynamic companies, giving you instant, diversified access to the heart of the U.S. stock market.", "type": "ETF", "tradingView": "NYSE:SPY", "convertsToAmt": 0, "is24hour": true, "ask": 0, "conid": "756733", "exchange": "SMART", "convertsToAddress": "", "bid": 0, "startDate": "", "isHot": true }, { "smartcontractid": "0x073d30e23adeef3d0e0cfe30d8f9f9b84c2155b4", "allow": "", "symbol": "TSLA (Tesla)", "tokenSymbol": "TSLA.r", "last": 220.06, "endDate": "", "description": "Tesla Inc - Leading producer of electric vehicles and solar energy products", "type": "Stock", "tradingView": "NASDAQ:TSLA", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 220.09, "conid": "76792991", "exchange": "SMART", "convertsToAddress": "", "bid": 220.02, "startDate": "", "isHot": true }, { "smartcontractid": "0x629c53ce821353d8961931fbb390eece2fd7aaf6", "allow": "", "symbol": "UBER (Uber)", "tokenSymbol": "UBER.r", "last": 80.46, "endDate": "", "description": "Uber Technologies Inc - A revolutionary urban mobility company providing seamless ride-sharing, speedy food delivery, and innovative freight solutions.", "type": "Stock", "tradingView": "NYSE:UBER", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 80.46, "conid": "365207014", "exchange": "SMART", "convertsToAddress": "", "bid": 80.45, "startDate": "", "isHot": true }, { "smartcontractid": "0x3186a1875bd67168642c0725f3f7dea7bbac752c", "allow": "", "symbol": "ZM (Zoom)", "tokenSymbol": "ZM.r", "last": 70.72, "endDate": "", "description": "Zoom Video Communications - Creator of the most widely used video conferencing platform in the world", "type": "Stock", "tradingView": "NASDAQ:ZM", "exchangeStatus": "open", "convertsToAmt": 0, "is24hour": true, "ask": 70.73, "conid": "361181057", "exchange": "SMART", "convertsToAddress": "", "bid": 70.69, "startDate": "", "isHot": true } ] """;
	
	String faqs = """
			[ { "question": "What is Reflection?", "answer": "Reflection is the only platform where you can buy and sell stock tokens that are 100% backed by shares of real stock." }, { "question": "What are stock tokens?", "answer": "Stock tokens are crypto tokens which represent shares of stock in publically traded companies." }, { "question": "After purchasing a stock token, do I actually own the underlying stock?", "answer": "No, you do not own the stock and you are not entitled to any of the benefits of stock ownership, such as voting rights. Your only right is to redeem the token for stablecoin at whatever price the stock is trading at the time of redemption." }, { "question": "What is RUSD?", "answer": "RUSD is the Reflection US dollar stablecoin. It is backed one-to-one by a combination of US dollars and USDT stablecoin." }, { "question": "What stablecoins and crypto platforms do you support?", "answer": "At the moment, we operate on the Polygon network chain and support the USDT stablecoin, but support for more platforms and stablecoins is planned." }, { "question": "Which crypto wallets do you support?", "answer": "We support MetaMask and WalletConnect. If you do not already have a wallet, we suggest you use MetaMask." }, { "question": "How do I create a MetaMask wallet?", "answer": "Follow this guide: https://reflection.trading/create-wallet" }, { "question": "How do I get USDT stablecoin in my wallet?", "answer": "USDT can be purchased on many crypto exchanges; we suggest Binance." }, { "question": "Will I receive dividends while holding a stock token?", "answer": "Not yet, but support for dividends is planned." } ] """;
			
	String tradStatic = """
			{ "symbol": "AAPL (Apple)", "tokenSymbol": "AAPL.r", "tradingView": "NASDAQ:AAPL", "description": "Apple Inc - Maker of iPhone, iPad, MacBook, and AirPods", "conid": "265598" }""";

	String tradDynamic = """
			{ "nonRusdApprovedAmt": 10, "askPrice": 224.02455, "stockTokenBalance": 1.0856, "rusdBalance": 1129.3953, "exchangeStatus": "open", "exchangeTime": "n/a", "nonRusdBalance": 45.261599, "bidPrice": 221.7855 }""";
	
	String onrampConvert = """
			{ 
			"createdAt" : "2024-09-26 16:16:01",
			"amount" : 3000, 
			"bank" : {
				"bank": "Chase",
				"name": "Chase Bank",
				"iban" : "TR700005901010130101011089",
				"type" : "AED Transfer"
				}
			}
			""";
//	String onrampConvert = """
//			{ "url": "https://www.ibm.com" }
//			""";
			
	HashMap<String,String> map = new HashMap<>(); // key must be lower case

	static int count = 10;		// total
	static int subcount = 3;	// mods
	
    JsonArray ar = new JsonArray();

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
	    for (int i = 1; i <= count; i++) {
	    	ar.add( new Stock( i, "Stock " + i, 100.0 + Util.rnd.nextDouble() * 10, 101.0 + Util.rnd.nextDouble() * 10));
	    }

	    map.put( "mywallet", myWallet);
		map.put( "positions-new", positions);
		map.put( "system-configurations", sysConfig); 
		map.put( "configurations", config);
		map.put( "signup", signup);
		map.put( "crypto-transactions", trans);
		map.put( "get-all-stocks", stocks);
		map.put( "live-orders", liveOrders);
		map.put( "working-orders", liveOrders);
		map.put( "hot-stocks", hotStocks);
		map.put( "get-all-stocks", hotStocks);
		map.put( "get-profile", profile);
		map.put( "get-stocks-with-prices", hotStocks);
		map.put( "all-live-orders", emptyArray);
		map.put( "faqs", faqs);
		map.put( "get-stock-with-price", stockWithPrice); // first solar
		map.put( "onramp-get-quote", onrampQuote); // first solar
		map.put( "onramp-convert", onrampConvert); // first solar
		map.put( "show-faucet", showFaucet);
		map.put( "turn-faucet", turnFaucet);
		map.put( "get-watch-list", watchList);
		map.put( "trading-screen-static", tradStatic);
		map.put( "trading-screen-dynamic", tradDynamic);
		map.put( "fund-wallet", fundWallet);

		//BaseTransaction.setDebug( true);
				
		// start Siwe thread to periodically save siwe cookies
		SiweTransaction.startThread();
		MyServer.listen( port, 5, server -> {
			server.createContext("/api/siwe/init", exch -> new SiweTransaction( exch).handleSiweInit() );
			server.createContext("/api/siwe/signin", exch -> new SiweTransaction( exch).handleSiweSignin() );
			server.createContext("/api/siwe/me", exch -> new SiweTransaction( exch).handleSiweMe() );
			server.createContext("/api/siwe/signout", exch -> new SiweTransaction( exch).handleSiweSignout() );
			server.createContext("/api/prices", Mock.this::handleSseRequest);			
			server.createContext("/api/fundWallet", Mock.this::fundWallet);			
			
			server.createContext("/", exch -> dummy( exch) );
		});
	}

	boolean blockIp = true;
	
	private void dummy(HttpExchange exch) {
		BaseTransaction t = new BaseTransaction( exch, false);
		String key = t.getPostApiToken();
		String val = S.notNull( map.get( key), invalid);
		
		if (!key.equals( "myWallet")) {
			S.out( "----- " + key);
		}
		
		if (val == invalid) {
			S.out( "  no support for " + key);
		}
		
		if (key.equals( "turn-faucet")) {
			S.sleep( 3000);
		}
		
		t.wrap( () -> {
			var json = key.equals( "onramp-convert")  // special case, must be ordered
					? JsonObject.parseOrdered(val)
					: JSONAware.parse( val);
			
			HashMap<String,String> headers = new HashMap<>();
			
			if (blockIp) {
				headers.put( "X-Block", "1");
			}
			
			t.respondFull( json, 200, headers); 
		});
	}

	private void handleSseRequest(HttpExchange exch) {
		try {
			handleSseRequest_( exch);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void handleSseRequest_(HttpExchange exch) throws Exception {
		int id = Util.rnd.nextInt();
		
		S.out( "received request " + id);
		
        exch.getResponseHeaders().set("Content-Type", "text/event-stream");
        exch.getResponseHeaders().set("Cache-Control", "no-cache");
        exch.getResponseHeaders().set("Connection", "keep-alive");
        exch.sendResponseHeaders(200, 0);

        OutputStream outputStream = exch.getResponseBody();

        // Send periodic stock updates every 2 seconds
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    JsonArray ar = getRandomStockUpdates();
                    String sseMessage = "data: " + ar.toString() + "\n\n";
                    S.out( "Sending %s %s", id, sseMessage);
                    outputStream.write(sseMessage.getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    timer.cancel();
                    S.out("Client disconnected %s", id);
                    try {
                        outputStream.close();
                    } catch (IOException ignored) {}
                }
            }
        }, 0, 2000);
    }


    // Helper method to get random stock updates
    private JsonArray getRandomStockUpdates() {
    	JsonArray mod = new JsonArray();
    	
        for (int i = 0; i < subcount; i++) {
            int index = Util.rnd.nextInt(count);
            
            Stock stock = (Stock)ar.get(index);
            stock.put( "bid", Util.rnd.nextDouble(1, 999) );
            stock.put( "ask", Util.rnd.nextDouble(1, 999) );
            
            mod.add(stock);
        }
        return mod;
    }

    static class Stock extends JsonObject {
        Stock(int conid, String name, double bid, double ask) {
        	put( "conid", conid);
        	put( "name", name);
        	put( "bid", bid);
        	put( "ask", ask);
        }
    }

	private void fundWallet(HttpExchange exch) {
		BaseTransaction t = new BaseTransaction( exch, false);
		t.respondNotFound();
//		S.out( "received fund wallet");
//		S.sleep( 2000); // simulate a delay
//		dummy( exch);
//		S.out( "returned");
	}
 }
