package fireblocks;

public class Busd {

	public static void main(String[] args) throws Exception {
		Fireblocks.setVals();
		// this must be initiated and signed by the user wallet
		//approve(Rusd.refWallet, 1000);
	}
	
	static void approve(String address, int amt) throws Exception {
		String keccak  ="095ea7b3";

		String[] paramTypes = { "address", "uint256" };
		Object[] params = { address, amt };
		
		//Fireblocks.call( Rusd.busd, keccak, paramTypes, params, "approve busd");
	}
}
