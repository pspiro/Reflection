package fireblocks;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import reflection.Config;
import tw.util.S;

/** Get recent Fireblocks transactions. 
 *  You must call Fireblocks.setKeys() before this */
public class Transactions {

	public static void main(String[] args) throws Exception {
		Config.ask();
		//S.out( getLastTransactions(5) );
		getTransaction("a0ad82f8-810b-4a54-8273-49f25658e853").display();

//		Transactions.getSince( System.currentTimeMillis() - 60000 * 3).display();
//		Transactions.getTransactions().display();
	}

	static JsonArray getLastTransactions(int limit) throws Exception {
		return Fireblocks.fetchArray( "/v1/transactions/?limit=" + limit);
	}

	/** This queries based on the createdAt timestamp; doesn't matter when it was updated */
	public static JsonArray getSince(long start) throws Exception {
		return Fireblocks.fetchArray( "/v1/transactions/?after=" + start);
	}

	public static JsonObject getTransaction(String id) throws Exception {
		return Fireblocks.fetchObject( "/v1/transactions/" + id);
	}

	public static void showFor(String wallet) throws Exception {
		S.out( "Looking for " + wallet);
		
		long since = System.currentTimeMillis() - 2 * Util.HOUR;
		
		// String admin = Accounts.instance.getAdmin( wallet);
		// int id = Accounts.instance.getId( admin);		
		String url = String.format("/v1/transactions?sourceId=%s&limit=%s&after=%s",
				5, 9, since);
		
		for (JsonObject obj : Fireblocks.fetchArray( url) ) {
			obj.getObject("source").display();
			S.out( obj.getString("destinationAddress") );
			S.out( obj.getString("note"));
			S.out( obj.getString("status"));
			S.out( obj.getString("subStatus"));
			
			
		}

//		String url = String.format( "/v1/transactions");
//		
//		Fireblocks fb = new Fireblocks();
//		fb.endpoint( url);
//		fb.operation( "POST");
//		fb.body( TestErrors.toJson( "{sourceId='4'}" ) );
//		String ret = fb.transact();
//		MyJsonArray.parse( ret).display();
	}
	
	
	/*
before	[optional] Unix timestamp in milliseconds. Returns only transactions created before the specified date.
after	[optional] Unix timestamp in milliseconds. Returns only transactions created after the specified date.
status	[optional] You can filter by one of the statuses.
orderBy	[optional] The field to order the results by. Available values : createdAt (default), lastUpdated.
sourceType	[optional] The source type of the transaction. Available values: VAULT_ACCOUNT, EXCHANGE_ACCOUNT, INTERNAL_WALLET, EXTERNAL_WALLET, FIAT_ACCOUNT, NETWORK_CONNECTION, COMPOUND, UNKNOWN, GAS_STATION, OEC_PARTNER.
sourceId	[optional] The source ID of the transaction.
destType	[optional] The destination type of the transaction. Available values: VAULT_ACCOUNT, EXCHANGE_ACCOUNT, INTERNAL_WALLET, EXTERNAL_WALLET, ONE_TIME_ADDRESS, FIAT_ACCOUNT, NETWORK_CONNECTION, COMPOUND.
destId	[optional] The destination ID of the transaction.
assets	[optional] A list of assets to filter by, separated by commas.
txHash	[optional] Returns only results with a specified txHash.
limit	[optional] Limits the number of returned transactions. If not provided, a default of 200 will be returned. The maximum allowed limit is 500.
sort
	 * 
	 */
}
