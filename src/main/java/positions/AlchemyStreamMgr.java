package positions;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import positions.HookServer.StreamMgr;
import tw.util.S;
import web3.Erc20;

class AlchemyStreamMgr extends StreamMgr {
	
	static String base = "https://dashboard.alchemy.com/api";
	
	private JsonObject post( String uri, JsonObject body) throws Exception {
		return MyClient.create( base + uri, body.toString() ).queryAlchemy(); 
	}
	
	private JsonObject patch( String uri, JsonObject body) throws Exception {
		return MyClient.createPatch( base + uri, body.toString() ).queryAlchemy(); 
	}
	
	private JsonObject get( String uri) throws Exception {
		return MyClient.create( base + uri).queryAlchemy(); 
	}
	
	private JsonObject delete( String uri) throws Exception {
		return MyClient.createDelete( base + uri).queryAlchemy(); 
	}
	
	/** create transfer stream for native and ERC-20
	 * 
	 * @return webhook id */
	@Override public String createTransfersStream() throws Exception {
		JsonObject body = Util.toJson(
				"network", HookServer.m_config.alchemyChain(),  
				"webhook_type", "ADDRESS_ACTIVITY",
				"webhook_url", HookServer.m_config.hookServerUrlBase() + "/hook/webhook",
				"addresses", new String[0]
				);

		S.out( "Creating alchemy transfers webhook");
		return createAndActivate( body);
	}
	
	/** create webhook to monitor for approval events
	 * 
	 *  @return webhook id */
	@Override public String createApprovalStream( String address) throws Exception {
		JsonObject body = Util.toJson(
				"network", HookServer.m_config.alchemyChain(),  
				"webhook_type", "ADDRESS_ACTIVITY",
				"webhook_url", HookServer.m_config.hookServerUrlBase() + "/hook/webhook",
				"topics", Util.toArray( "0x8c5be1e5ebec7d5bd14f71443fa28e55bc75e4bba6c7d3e62bd1bcbf2c7c5f4a"),
				"addresses", Util.toArray( address)
				);

		S.out( "Creating alchemy approvals webhook");
		return createAndActivate( body);
		
	}
	
	private JsonArray getAllHooks() throws Exception {
		S.out( "getting all Alchemy webhooks");
		return get( "/team-webhooks").getArray( "data");
	}
	
	public void deleteAllForChain( String chain) throws Exception {
		for (var hook : getAllHooks() ) {
			if (hook.getString( "network").equals( chain) ) {
				deleteHook( hook.getString( "id") );
			}
		}
	}

	public void deleteAllHooks() throws Exception {
		for (var hook : getAllHooks() ) {
			deleteHook( hook.getString( "id") );
		}
	}

	private void deleteHook(String id) throws Exception {
		S.out( "deleting Alchemy hook %s", id);
		delete( "/delete-webhook?webhook_id=" + id);  
	}

	/** Create and activate webhook/stream */
	private String createAndActivate( JsonObject body) throws Exception {
		// create it
		var ret = post( "/create-webhook", body);
		S.out( "  created hook ret=" + ret);
		var data = ret.getObject( "data");
		
		if (data == null) {
			S.out( "Error creating Alchemy webhook: %s", ret);
			throw new Exception( "Could not create Alchemy webhook");
		}

		
		
		String id = data.getString( "id");
		S.out( "  created hook with id %s", id);

		// activate it
		// this doesn't work
//		data.display();
//		post( "/update-webhook", Util.toJson( "webhook_id", id, "is_active", true) );

		return id;
	}

	/** Add wallet address to transfer stream */
	@Override public void addAddressToStream(String id, String address) throws Exception {
		JsonObject body = Util.toJson(
				"webhook_id", id,
				"addresses_to_add", Util.toArray( address),
				"addresses_to_remove", new String[0]
				);

		S.out( "Add address %s to alchemy webhook %s", address, id);
		patch( "/update-webhook-addresses", body).display();
	}

	@Override protected void handleHookWithData(JsonObject hook, HookServer hookServer) throws Exception {
		S.out( "received Alchemy hook: " + hook);

		JsonObject event = hook.getObject( "event");
		if (event != null) {
			for (var trans : event.getArray( "activity") ) {
				var rawContract = trans.getObject( "rawContract");
				Util.require( rawContract != null, "Alchemy hook missing rawContract");

				int decimals = rawContract.getInt( "decimals");

				if (decimals != 0) { // if this can be zero, we should look it up from the map or query it
					double amt = Erc20.fromBlockchain( rawContract.getString( "rawValue"), decimals);

					if (amt != 0) {
						String category = trans.getString( "category");
						String assetName = trans.getString( "asset");
						String from = trans.getString( "fromAddress").toLowerCase();
						String to = trans.getString( "toAddress").toLowerCase();
						String contract = rawContract.getString( "address"); // will be null for native currency

						// native transfer?
						if ( (category.equals( "internal") || category.equals( "external") ) &&
								S.isNull( contract) &&
								assetName.equalsIgnoreCase( HookServer.m_config.nativeTokName() ) ) {

							S.out( "ALCHEMY NATIVE TRANSFER  %s native token was transferred from %s to %s", 
									amt, from, to);
							hookServer.adjustNativeBalance( from, -amt, false);
							hookServer.adjustNativeBalance( to, amt, false);
						}

						// process ERC-20 transfer?
						else if ( (category.equals( "erc20") || category.equals( "token") ) &&
								S.isNotNull( contract) ) {

							S.out( "ALCHEMY ERC20 TRANSFER  %s %s was transferred from %s to %s", 
									amt, assetName, from, to);
							hookServer.adjustTokenBalance( from, contract, -amt, false);
							hookServer.adjustTokenBalance( to, contract, amt, false);
						}
						else {
							S.out( "  ignoring  category=%s  asset=%s  from=%s  to=%s  contract=%s",
									category, assetName, from, to, contract);
						}
					}
					else {
						S.out( "  no quantity, ignoring");
					}
				}
				else {
					S.out( "  WARNING no decimals, ignoring");
				}
			} // for loop over activities
		} // end ADDRESS_ACTIVITY

		// this code is the same as for Moralis and could be shared, but
		// they are separate in case one changes and not the other
		for (var trans : hook.getArray( "erc20Approvals") ) {
			String contract = trans.getString("contract");

			if (contract.equalsIgnoreCase(HookServer.m_config.busd().address() ) ) {
				String owner = trans.getString("owner");
				String spender = trans.getString("spender");
				double amt = trans.getDouble("valueWithDecimals");

				Util.lookup( hookServer.m_hookMap.get(owner), hookWallet -> {
					S.out( "ALCHEMY APPROVAL  %s can spend %s %s on behalf of %s",
							spender, "" + amt, contract, owner);  // use java formatting for amt which can be huge
					hookWallet.approved( amt);	
				});
			}
		}
	}
}
