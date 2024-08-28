package refblocks;

import org.web3j.crypto.Credentials;

import common.MyScanner;
import common.Util;
import web3.Matic;
import web3.RetVal;

public class RbMatic extends Matic {

	/** transfer native token */
	@Override public RetVal transfer(String senderKey, String to, double amt) throws Exception {
		return Refblocks.transfer( senderKey, to, amt);
	}

	/** this is very fast */
	@Override public String getAddress(String key) throws Exception {
		Util.require( Util.isValidKey(key), "not a valid private key");
		return Credentials.create( key ).getAddress();
	}
	
	@Override public void createSystemWallets() throws Exception {
		try (MyScanner scanner = new MyScanner() ) {
			String pw1 = scanner.input( "Enter password: ");
			String pw2 = scanner.input( "Re-enter password: ");
			Util.require( pw1.equals( pw2), "Mismatch");
			String hint = scanner.input( "Enter pw hint: ");
			
			createWallet( pw1, hint, "Owner");
			createWallet( pw1, hint, "RefWallet");
			createWallet( pw1, hint, "Admin1");
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}

	private static void createWallet(String pw, String hint, String name) throws Exception {
//		CreateKey.createProdWallet( 
//				pw, 
//				name, 
//				String.format( 
//						"%s wallet created on %s",
//						name,
//						Util.yToS.format( System.currentTimeMillis() ) ));
	}
}
