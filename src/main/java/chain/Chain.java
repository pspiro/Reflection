package chain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import common.Util;
import onramp.Onramp;
import tw.google.NewSheet.Book;
import tw.util.S;
import web3.Busd;
import web3.MoralisServer;
import web3.NodeInstance;
import web3.NodeInstance.Transfer;
import web3.NodeInstance.Transfers;
import web3.Refblocks;
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
	private final HashMap<String,StockToken> mapAddress = new HashMap<>();  // maps token address (lower case) to StockToken
	private String[] allAddresses;
	private boolean readSymbols;
	
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

	public Collection<StockToken> getTokens() {
		return mapConid.values();
	}
	
	public ArrayList<StockToken> getTokensList() {
		Util.must( readSymbols, "Error: must read symbols first");

		ArrayList<StockToken> list = new ArrayList<>();
		mapConid.values().forEach( stock -> list.add( stock) );
		return list; 
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
		Util.must( readSymbols, "Error: must read symbols first");
		return getTokens().iterator().next();
	}

	/** returns stocks only; see getAllAddresses */ 
	public String[] getAllContractsAddresses() {
		Util.must( readSymbols, "Error: must read symbols first");

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
		Util.must( readSymbols, "Error: must read symbols first");
		
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
			
			if (row.getInt( "Conid") > 0 && S.isNotNull( address) && row.getBool( "Active") ) {
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
		
		readSymbols = true;
	}

	/** This is called frequently so a map is good */
	public StockToken getTokenByAddress(String address) {
		return mapAddress.get( address.toLowerCase() );
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

	/** WARNING: spreadsheet and params.busdAddress() are not updated 
	 * @throws Exception */
	public void setBusdAddress(String busdAddress) throws Exception {
		S.out( "BUSD ADDRESS IS %s; update spreadsheet", busdAddress);
		busd = new Busd( busdAddress, busd.decimals(), busd.name(), this); 
	}

	/** WARNING: spreadsheet and params.rusdAddress() are not updated 
	 * @throws Exception */
	public void setRusdAddress(String rusdAddress) throws Exception {
		S.out( "RUSD ADDRESS IS %s; update spreadsheet", rusdAddress);
		rusd = new Rusd( rusdAddress, rusd.decimals(), this);
	}

	/** see also getAllAddresses() */
	public String[] getStablecoinAddresses() throws Exception {
		return new String[] { params.rusdAddr(), params.busdAddr() };
	}
	
	public String[] getAllAddresses() throws Exception {
		ArrayList<String> ar = new ArrayList<>();
		ar.addAll( Util.toList( getStablecoinAddresses() ) );
		ar.addAll( Util.toList( getAllContractsAddresses() ) );
		return ar.toArray( new String[0]);
	}
	
	public void showAdmin1Nonces() throws Exception {
		blocks().showAllNonces( params.admin1Addr() );
	}

	/**  NOT SAFE, MoralisServer uses static vars
	 *  
	 * @param decimals if zero we will look it up from the map or query for it 
	 * @throws Exception */
	public HashMap<String, Double> reqPositionsMap(String walletAddr, String[] contracts, int decimals) throws Exception {
		if (params.isPolygon() ) {
			MoralisServer.setChain( params.moralisPlatform() );  // NOT SAFE kind of dangerous and not good; we should create a MoralisServer instance just like the other ones or pass in the chain name; and we should use polymorphism
			return MoralisServer.reqPositionsMap(walletAddr, contracts);
		}
		return node.reqPositionsMap(walletAddr, contracts, decimals);
	}

	/** @param addresses is ignored for Moralis chains */
	public Transfers getWalletTransfers(String wallet, String[] addresses) throws Exception {
		if (params.usesMoralis() ) {
			Transfers transfers = new Transfers();
			MoralisServer.setChain( params.moralisPlatform() );  // NOT SAFE kind of dangerous and not good; we should create a MoralisServer instance just like the other ones or pass in the chain name; and we should use polymorphism
			MoralisServer.getWalletTransfers(wallet, batch -> {
				for (var one : batch) {
					Transfer t = new Transfer(
							one.getString( "address"),							
							one.getString( "from_address"),
							one.getString( "to_address"),
							one.getDouble( "value_decimal"),
							one.getLong( "block_number"),
							one.getString( "transaction_hash"),
							one.getString( "block_timestamp")
							);
					
					// check to see if it is in list of addresses?
					
					transfers.add( t);
				}
			});
			
			return transfers;
		}
		
		// non-moralis, not used
		return node.getWalletTransfers(wallet, addresses);  // this is intensive and times out sometimes
	}

	/** return name for RUSD, BUSD, or any stock token */
	public String getName(String addr) {
		if (addr.equalsIgnoreCase( rusd.address() ) ) {
			return rusd.name();
		}
		if (addr.equalsIgnoreCase( busd.address() ) ) {
			return busd.name();
		}

		var tok = getTokenByAddress(addr);
		return tok != null ? tok.name() : "";
	}

	public String getAdminKey(String addr) throws Exception {
		return 
			addr.equalsIgnoreCase( params.admin1Addr() ) ? params.admin1Key() :
			addr.equalsIgnoreCase( params.sysAdminAddr() ) ? params.sysAdminKey() : null;
	}

	/** check if admin1 wallet is running out of gas */ 
	public void checkAdminBalance() throws Exception {
		int mult = params.isProduction() ? 100 : 10;
		if (node.getNativeBalance( params.admin1Addr() ) < params.faucetAmt() * mult) {
			throw new Exception( "Admin1 wallet is running out of gas on " + params.name() );
		}
	}

}
// tokensupply on monitor, do a batch query. bc
// symbol not displaying on Monitor/Token tab