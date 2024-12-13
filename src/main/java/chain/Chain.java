package chain;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import common.Util;
import onramp.Onramp;
import refblocks.Refblocks;
import tw.google.NewSheet.Book;
import tw.util.S;
import web3.Busd;
import web3.MoralisServer;
import web3.NodeInstance;
import web3.Rusd;
import web3.StockToken;
import web3.StockToken.StockTokenRec;

public class Chain {
	private ChainParams params;
	private NodeInstance node;
	private Web3j web3j;
	private Refblocks blocks;
	private Rusd rusd;
	private Busd busd;
	private final HashMap<Integer,StockToken> mapConid = new HashMap<>();
	private final HashMap<String,StockToken> mapAddress = new HashMap<>();
	private String[] allAddresses;
	
	Chain( ChainParams chainIn) throws Exception {
		params = chainIn;
		node = params.createNode();
		web3j = Web3j.build( new HttpService( params.rpcUrl() ) );
		blocks = new Refblocks( params.chainId(), web3j, node);
		rusd = new Rusd( params.rusdAddr(), params.rusdDecimals(), this);
		busd = new Busd( params.busdAddr(), params.busdDecimals(), params.busdName(), this);
	}
	
	public void dump() throws Exception {
		S.out( "------ chain params");
		S.out( params);
	}
	
	public ChainParams params() {
		return params;
	}

	public NodeInstance node() {
		return node;
	}

	public Rusd rusd() {
		return rusd;
	}

	public Busd busd() {
		return busd;
	}
	
	public Web3j web3j() {
		return web3j;
	}
	
	public Refblocks blocks() {
		return blocks;
	}

	public Collection<StockToken> tokens() {
		return mapConid.values();
	}
	
	public double getApprovedAmt() throws Exception {
		return busd().getAllowance( params().refWalletAddr(), params().rusdAddr() );
	}

	public String browseTx(String hash) {
		return String.format( "%s/tx/%s", params().blockchainExpl(), hash);
	}

	public String browseAddress(String address) {
		return String.format( "%s/address/%s", params().blockchainExpl(), address);
	}

	public StockToken getAnyStockToken() {
		return tokens().iterator().next();
	}

	public String[] getAllContractsAddresses() {
		if (allAddresses == null) {
			allAddresses = new String[mapConid.size()];
			
			int i = 0;
			for (StockToken token: mapConid.values() ) {
				allAddresses[i++] = token.address();
			}
		}
		return allAddresses;
	}
	
	/** for the get-all-stocks query which is for dropdown on trading page;
	 *  does not return prices */ 
	public JsonArray getAllStocks(Stocks stocks) throws Exception {
		JsonArray ar = new JsonArray();
		
		for (var token : mapConid.values() ) {
			var stock = stocks.getStockByConid( token.conid() );
			
			// stock could be null if it is not 'Active' on the Master-symbols page
			if (stock != null) {
				ar.add( Util.toJson(
						"smartcontractid", token.address(), 
						"symbol", stock.symbol(),
						"description", stock.rec().description(),
						"conid", "" + token.conid(),
						"exchangeStatus", "unknown", // currently ignore, we could bring it back. 
						"tradingView", stock.rec().tradingView(),
						"tokenSymbol", token.name()
						) );
			}
		}
		
		// sort alpha by symbol
		ar.sort( new Comparator<JsonObject>() {
			@Override public int compare(JsonObject o1, JsonObject o2) {
				return o1.getString( "symbol").compareTo( o2.getString( "symbol") );
			}
			
		});
		
		return ar;
	}

	/** read the symbols and create the stock tokens for this chain */ 
	public void readSymbols(Book book) throws Exception {
		mapConid.clear();
		mapAddress.clear();
		
		var symbolsTab = book.getTab( params.symbolsTab() );
		S.out( "reading %s stocks from tab %s", params().name(), params.symbolsTab() );

		for (var row : symbolsTab.queryToJson() ) {
			String address = row.getString( "TokenAddress");
			
			if (row.getInt( "Conid") > 0 && S.isNotNull( address) ) {
				StockTokenRec rec = new StockTokenRec(
						row.getInt( "Conid"),
						row.getString( "StartDate"),
						row.getString( "EndDate"),
						row.getDouble( "ConvertsToAmt"),
						row.getString( "ConvertsToAddress"),
						Util.getEnum( row.getString( "Allow"), Allow.values(), Allow.All) 
						);
				
				Util.require( Util.isValidAddress(address), "Invalid address '%s' for conid %s on tab %s", 
						address, row.getInt( "Conid"), params.symbolsTab() );
				
				// create the stock token
				StockToken token = new StockToken( address, row.getString( "TokenSymbol"), this, rec);
				
				// add it to the maps
				mapConid.put( row.getInt( "Conid"), token);
				mapAddress.put( address, token);
			}
		}
	}

	/** This is called frequently so a map is good */
	public StockToken getTokenByAddress(String address) {
		return mapAddress.get( address);
	}

	/** This is called frequently so a map is good */
	public StockToken getTokenByConid(int conid) {
		return mapConid.get( conid);
	}
	
	@Override public String toString() {
		return params.name();
	}

	public int chainId() {
		return params.chainId();
	}

	public Onramp onramp() {
		return params.isProduction() ? Onramp.prodRamp : Onramp.devRamp;
	}

	public void setBusdAddress(String busdAddress) {
		S.out( "BUSD ADDRESS IS " + busdAddress);
	}

	public void setRusdAddress(String rusdAddress) {
		S.out( "RUSD ADDRESS IS " + rusdAddress);
	}

	public String[] getStablecoinAddresses() throws Exception {
		return new String[] { params.rusdAddr(), params.busdAddr() };
	}
	
	public void showAdmin1Nonces() throws Exception {
		blocks().showAllNonces( params.admin1Addr() );
	}

	/** 
	 * @param decimals if zero we will look it up from the map or query for it 
	 * @throws Exception */
	public HashMap<String, Double> reqPositionsMap(String walletAddr, String[] contracts, int decimals) throws Exception {
		if (params.isPolygon() ) {
			MoralisServer.setChain( params.moralisPlatform() );  // kind of dangerous and not good; we should create a MoralisServer instance just like the other ones or pass in the chain name; and we should use polymorphism
			return MoralisServer.reqPositionsMap(walletAddr, contracts);
		}
		return node.reqPositionsMap(walletAddr, contracts, decimals);
	}
}
// tokensupply on monitor, do a batch query. bc
// symbol not displaying on Monitor/Token tab