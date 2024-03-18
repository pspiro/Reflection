package test;

import org.json.simple.JsonObject;

import common.Util;
import reflection.Config;

public class LockAward {
	public static void main(String[] args) throws Exception {
		JsonObject lock = Util.toJson(
				"amount", 10,
				"lockedUntil", System.currentTimeMillis()
		);
		
		
		String wallets = "0xF79Bef5b9Ea9f57B1deD23940522279706b81f9f,0x72176D5F5621b79e8c794CD29FE6B9C8D0Ecf335,0xD6456467cB03a843d7E0bCCf4eBA7295A83bB33a,0x9914026e0aD36aD2f0Dd8fA2875f82f806A10aa6,0x9c1F6FE52378551D88C44739E023fBb1Bf019c7F,0x4837325cE91Dc7f0a57da6eD14056793b8f04a42,0x8e32c893Dd3A96bb4D5Ee4aF2828C3101D578045,0x95fa52573BFA2DB9C670B8d205fecFa04c2AD96f,0x51eeb8e95e6bf6c24d47ffac9903f38d5842fbe3";
		
		Config.ask().useExternalDbUrl().sqlCommand( sql -> {
			for (String wallet : wallets.split(",") ) {
				JsonObject json = Util.toJson( "locked", lock, "wallet_public_key", wallet.toLowerCase() );
				sql.insertOrUpdate("users", json, "wallet_public_key = '%s'", wallet.toLowerCase() );
			}
		});
	}
}
