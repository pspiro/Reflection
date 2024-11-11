package chain;

import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

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
		// read entire Blockchain tab
		Book book = NewSheet.getBook( NewSheet.Reflection);
		var rows = book.getTab( "Blockchain").queryToJson();
		
		readChain( book, rows, "Polygon");
		readChain( book, rows, "PulseChain");
		readChain( book, rows, "Sepolia");
		readChain( book, rows, "zkSync");
	}

	/** Read one column (one chain) from the Blockchain tab, AND
	 *  read in all the symbols for that chain as well */
	private void readChain(Book book, JsonArray rows, String chainName) throws Exception {
		JsonObject json = new JsonObject();
		for (var row : rows) {
			json.put( row.getString( "Tag"), row.getString( chainName) );
		}
		
		// create the Chain and read in the symbols
		var chain = new Chain( json.toRecord( ChainParams.class) );
		chain.readSymbols( book);
		
		// add this new chain to the map
		put( chain.chainId(), chain);
	}
	
	public static void main(String[] args) throws Exception {
		Chains chains = new Chains();
		chains.readAll();
		S.out( chains);
	}
}
