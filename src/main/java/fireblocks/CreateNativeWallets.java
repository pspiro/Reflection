package fireblocks;

import reflection.Config;
import tw.util.S;

/** Use this to create the native wallets when you want to use a new testnet */
public class CreateNativeWallets {
	public static void main(String[] ar) throws Exception {
		Config.ask();
		
		create( "Owner");
		create( "RefWallet");
		create( "Admin1");
//		create( "Admin2");
	}

	private static void create(String account) throws Exception {
		Fireblocks.createWallet( Accounts.instance.getId(account), Fireblocks.platformBase);
		S.out( "created wallet " + Accounts.instance.getAddress(account));
	}
}
