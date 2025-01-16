package chain;

import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import tw.google.NewSheet;
import tw.google.NewSheet.Book;
import tw.util.S;

/** maps chainId to Chain */
public class Chains extends HashMap<Integer,Chain> {
	public static final int Polygon = 137;
	public static final int ZkSync = 324;
	public static final int Sepolia = 11155111;
	public static final int PulseChain = 369;

	public Chain chain( int chainId) {
		return get( chainId);
	}

	public ChainParams params( int chainId) {
		return get( chainId).params();
	}

	/** Used for requesting market data */
	public Chain polygon() {
		return chain( Polygon);
	}

	public Chain pulseChain() {
		return chain( PulseChain);
	}

	public void checkKeys() throws Exception {
		for (var chain : values() ) {
			S.out( "  checking key for " + chain.params().name() );
			chain.params().admin1Key();
		}
	}

	void dump() throws Exception {
		for (var chain : values() ) {
			chain.dump();
		}
	}

	public void readAll() throws Exception {
		read( Util.toArray( "Polygon", "PulseChain", "Sepolia", "zkSync"), true );
	}

	public static Chain readOne( String name, boolean readSymbols) throws Exception {
		Chains chains = new Chains();
		chains.read( Util.toArray( name), readSymbols);
		return chains.values().iterator().next();
	}

	public void read(String[] names, boolean readSymbols) throws Exception {
		// read entire Blockchain tab
		Book book = NewSheet.getBook( NewSheet.Reflection);
		var rows = book.getTab( "Blockchain").queryToJson();
		
		for (String name : names) {
			readChain( book, rows, name, readSymbols);
		}
	}

	/** Read one column (one chain) from the Blockchain tab, AND
	 *  read in all the symbols for that chain as well */
	private void readChain(Book book, JsonArray rows, String chainName, boolean readSymbols) throws Exception {
		S.out( "reading blockchain %s", chainName);
		
		JsonObject json = new JsonObject();
		for (var row : rows) {
			// read tag and CORRECT value and add the pair to json object
			Util.iff( row.getString( "Tag"), tag -> json.put( tag, row.getString( chainName) ) );
		}
		
		Util.require( json.getInt( "chainId") > 0, "Error: the chain name '%s' is invalid", chainName);
		
		// create the Chain and read in the symbols
		var chain = new Chain( json.toRecord( ChainParams.class) );
		
		if (readSymbols) {
			chain.readSymbols( book);
		}
		
		S.out( "  read %s settings and %s symbols", json.size(), chain.getTokens().size() );
		
		// add this new chain to the map
		put( chain.chainId(), chain);
	}
	
	public static void main(String[] args) throws Exception {
		Chains chains = new Chains();
		chains.readAll();
		S.out( chains);
	}

	/** Called by RefAPI; all other use sysAdmin */
	public void useAdmin1() {
		values().forEach( chain -> chain.rusd().useAdmin1() );
	}

	/** make sure admin1 wallet is not running out of gas 
	 * @throws Exception */
	public void checkAdminBalance() throws Exception {
		for (var chain : values() ) {
			chain.checkAdminBalance();
		}
	}
}
