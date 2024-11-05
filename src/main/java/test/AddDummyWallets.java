package test;

import common.Util;
import reflection.SingleChainConfig;

/** Add wallets to users table with name 'test test' */
public class AddDummyWallets {
	public static void main(String[] args) throws Exception {
		String wallets="""
		0xb8585bd9c93529734ed494098f2dd5e411e123b1,
		0x3c8bf9f92ce2ce964aa6f499a41b4f5a5add5226,
		0xf2bace1473702e60a9f528a861f1dfc3da8b1ee8,
		0xb4a1293eb6b8bed7fa4c2ca856cc8325c0587016,
		0x54c5b52f3b248ae98bea1f4c454d0866ff3b1f56""";
		
		for (String wallet : wallets.split(",") ) {
			SingleChainConfig.ask().sqlCommand( sql -> sql.insertJson("users", Util.toJson( 
					"first_name", "test",
					"last_name", "test",
					"wallet_public_key", wallet.trim() ) ) );
		}
	}

}
