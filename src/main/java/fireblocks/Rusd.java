package fireblocks;

import json.MyJsonObject;

public class Rusd {
	static String refWallet = "0x63196e51854B6E1446eDaafbC401F8c7Afdb33ca"; // this is the address of the primary token on Fireblocks, e.g. ETH or BNB_BSC
	static String userAddress = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";

	static String rusd = "0xc61b328c5a3619299e2714f1d7c2fe896ebeb819"; // contract address deployed with this refWallet
	static String busd = "0x76CBf8325E0cC59AaD46204C80091757B06b54a3";
    
	static String buyRusd = "0x28c4ef43"; // buyRusd(address userAddress, address stableCoinAddress, uint256 amount)
	static String buyStock =  "58e78a85";
	static String sellStock = "5948f1f0";
	
	
	public static void main(String[] args) throws Exception {
		Fireblocks.setVals();
		//deploy();
		call();
	}
	
	// this works
	static void deploy() throws Exception {
		String[] paramTypes = { "address" };
		String[] params = { refWallet };
		Fireblocks.deploy( "c:/work/smart-contracts/rusd.bytecode", paramTypes, params, "Deploy RUSD");
	}
	
	static void call() throws Exception {
		
		
		String[] types = { "address", "address", "uint256" };
		Object[] params = { userAddress, busd, 10 };
		
		MyJsonObject obj = Fireblocks.call( rusd, buyRusd, types, params, "RUSD.buyRusd");
		obj.display();
		
		String id = obj.getString("id");
		MyJsonObject trans = Fireblocks.getTransaction( id);
		trans.display();
	}
}
