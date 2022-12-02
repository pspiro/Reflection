package fireblocks;

import java.math.BigDecimal;
import java.math.BigInteger;

import json.MyJsonObject;
import reflection.Main;
import reflection.RefCode;
import reflection.RefException;
import reflection.Util;
import tw.util.S;

public class Rusd {
	static String rusdAddr = "0x8a694956F724097ecE8Bf9A5B9d80ed8e05b66e2"; // contract address
	static String qqq = "0xb402C11973Bcb15149b765e93E2553a688668f93";
	
	static int decimals = 5;
	static BigDecimal mult = new BigDecimal( 10).pow(decimals);
	static String buyKeccak =  "58e78a85";
	static String sellKeccak = "5948f1f0";
	
	// keccaks calculated as:
	// buyStock(address,address,address,uint256,uint256)
	// sellStock(address,address,address,uint256,uint256)
// do i need 0x for the contract call data?
	public static void main(String[] args) throws Exception {
		Fireblocks.setVals();
		
		String myWallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
		
		String[] types = { "address", "address", "address", "uint256", "uint256" };
		Object[] params = { myWallet, rusdAddr, qqq, 5, 6 };
		
		MyJsonObject obj = Fireblocks.call( rusdAddr, buyKeccak, types, params, "RUSD.buyStock");
		obj.display();
		
		String id = obj.getString("id");
		MyJsonObject trans = Fireblocks.getTransaction( id);
		trans.display();
	}
}
