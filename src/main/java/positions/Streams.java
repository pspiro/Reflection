package positions;


import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;
import web3.MoralisServer;

// Q: does JsonObject translate // back and forth to \/\/?
// Q my question is, if allAddresses is set to false, how do I specify which contract it listens to?

public class Streams {

	public static void main(String[] args) throws Exception {
		displayStreams();
	}
	static void addAddressToStream(String id, String... list) throws Exception {
		if (list.length > 0) {
			S.out( "Stream %s: adding addresses [%s]", id, String.join(", ", list) );
			
		     MoralisServer.post(
		    		 String.format( "https://api.moralis-streams.com/streams/evm/%s/address", id), 
		    		 Util.toJson( "address", list).toString() );
		}
	}

	/** Display stream and up to five addresses */
	public static void displayStreams() throws Exception {
		S.out( "Existing streams");
		JsonObject obj = MoralisServer.queryObject( "https://api.moralis-streams.com/streams/evm?limit=5");
		int total = obj.getInt("total");
		JsonArray ar = obj.getArray("result");
		
		for (JsonObject stream : ar) {
			S.out( "Stream " + stream.getString("description") );
			stream.display();
			//S.out( stream);
			displayAddresses( stream.getString("id"), 5);
		}
	}

	private static void displayAddresses(String id, int max) throws Exception {
		JsonObject obj = MoralisServer.queryObject( 
			String.format( "https://api.moralis-streams.com/streams/evm/%s/address?limit=%s", id, max) );
		S.out( "  " + obj);
	}

	/** @param chain is hex chain id 
	 *  @return stream id */
	public static String createStream(String stream, String name, String webhookUrl, String chain, String... addresses) throws Exception {
		Util.require( chain.startsWith( "0x"), "chain must be hex"); 
		
		JsonObject json = JsonObject.parse( stream);
		json.put( "description", name);
		json.put( "tag", name);
		json.put( "webhookUrl", webhookUrl);
		json.put( "chainIds", new String[] { chain } );
		
		S.out( "Creating stream '%s' on chain %s with URL %s", name, chain, webhookUrl);
		S.out( "Full stream: " + json);
		
		deleteStreamByName( name);

		JsonObject obj = JsonObject.parse(
				MoralisServer.put( "https://api.moralis-streams.com/streams/evm", json.toString() ) );
		String id = obj.getString("id");

		addAddressToStream(id, addresses);
		setStatus( id, true);
		return id;
	}

	/** Delete string where description equals name 
	 * @throws Exception */
	public static void deleteStreamByName(String name) throws Exception {
		for (JsonObject stream : MoralisServer.queryObject( "https://api.moralis-streams.com/streams/evm?limit=10")
				.getArray("result") ) {
			
			if (stream.getString("description").equals( name) ) {
				S.out( "Deleting existing stream '%s'", name);
				deleteStream( stream.getString("id") );
			}
		}
	}

	/** This is dangerour as it deletes all live streams */
	public static void deleteAll() throws Exception {		
		MoralisServer.queryObject( "https://api.moralis-streams.com/streams/evm?limit=10")
			.getArray("result").forEach( stream -> Util.wrap( () ->
				deleteStream( stream.getString("id") ) ) );
	}

	public static void deleteStream(String id) throws Exception {
		S.out( "Deleting stream " + id);
		String url = "https://api.moralis-streams.com/streams/evm/" + id;
		S.out( MoralisServer.delete( url) );
	}

	public static void setStatus(String id, boolean active) throws Exception {
		S.out( "Stream %s: setting status to %s", id, active ? "active" : "paused");
		String url = String.format( "https://api.moralis-streams.com/streams/evm/%s/status", id);
		MoralisServer.post( url, Util.toJson( "status", active ? "active" : "paused").toString() );
	}

	public static String erc20Transfers = """
	{
		"getNativeBalances" : [ ],
		"triggers" : [ ],
		"includeContractLogs" : true,
		"includeNativeTxs": true,
		"includeAllTxLogs" : false,
		"includeInternalTxs" : false,
		"allAddresses" : false,	

		"topic0" : [  "Transfer(address,address,uint256)" ],
	
		"abi" : [
			{
				"inputs" : [
			{
				"indexed" : true,
				"name" : "from",
				"type" : "address"
			},
			{
				"indexed" : true,
				"name" : "to",
				"type" : "address"
			},
			{
				"indexed" : false,
				"name" : "value",
				"type" : "uint256"
			}
			],
			"name" : "Transfer",
			"anonymous" : false,
			"type" : "event"
			}
		]
	}
	""";

	public static String approval = """
	{
		"includeNativeTxs" : false,
		"includeContractLogs" : true,
		"includeAllTxLogs" : false,
		"allAddresses" : false,
		"includeInternalTxs" : false,
	
		"topic0" : [
			"Approval(address,address,uint256)"
		],
	
		"abi" : [
			{
				"inputs" : [
					{
						"indexed" : true,
						"name" : "owner",
						"type" : "address",
					},
					{
						"indexed" : true,
						"name" : "spender",
						"type" : "address"
					},
					{
						"indexed" : false,
						"name" : "value",
						"type" : "uint256"
					}
				],
				"name" : "Approval",
				"anonymous" : false,
				"type" : "event"
			}
		]
	}
	""";

	/** Return the status of the first matching stream */
	public static String getStreamStatus(String name) throws Exception {
		for (JsonObject stream : MoralisServer.queryObject( "https://api.moralis-streams.com/streams/evm?limit=10")
				.getArray("result") ) {
			
			if (stream.getString("description").equals( name) ) {
				return stream.getString("statusMessage");
			}
		}
		return "Stream not found";
	}

}
