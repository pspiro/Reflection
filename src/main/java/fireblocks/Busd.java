package fireblocks;

import tw.util.S;

public class Busd {

	private static final String mintKeccak = "40c10f19";

	static String mywallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	
	public static void main(String[] args) throws Exception {
		Fireblocks.setProdValsPolygon();
		mint(mywallet, 2000);
		//deploy();
		//approveToSpendBusd(Fireblocks.userAcctId, Fireblocks.rusdAddr, 1000);
	}
	
	
	private static void mint(String address, double amt) throws Exception {
		String[] types = {"address", "uint256"};
		Object[] vals = {
				address,
				Rusd.toStablecoin(Fireblocks.busdAddr, amt)
		};
		
		Fireblocks.call( Fireblocks.refWalletAcctId1, Fireblocks.busdAddr, mintKeccak, types, vals, "BUSD.mint()");
	}
	
	public static String approveToSpendBusd(int account, String spenderAddr, double amt) throws Exception {
		return Rusd.approve( account, spenderAddr, Fireblocks.busdAddr, amt);
	}

	static void deploy() throws Exception {
		String[] types = { "address" };
		Object[] vals = { Fireblocks.refWalletAddr1 };
		
		String addr = Deploy.deploy("c:/work/smart-contracts.OLD/BUSD.bytecode", Fireblocks.ownerAcctId, types, vals, "deploy BUSD");
		S.out( "Deployed to %s", addr);
		
		// this must be initiated and signed by the user wallet
		//approve(Rusd.refWallet, 1000);
	}
	
}
