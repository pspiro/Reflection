package test;

import reflection.Config;
import tw.util.OStream;
import tw.util.S;
import web3.NodeInstance;



/** Just test that you can connect to the database. */
public class Bridge {
	
	static String prodOwner = "0x966454dCA56f75aB15Df54cee9033062D331e0d4";      // prod owner
	static String refWallet = "0xF6e9Bff937ac8DF6220688DEa63A1c92b6339510";
	static String usdcOnEther = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48";
	static String usdcOnPulse = "0x15D38573d2feeb82e7ad5187aB8c1D52810B1f07";
	static String pulseNode = "https://rpc.pulsechain.com";
	static String etherNode = "https://ethereum-rpc.publicnode.com";

	public static void main(String[] args) throws Exception {
//		NodeInstance ether = new NodeInstance( etherNode, 10);
//		double v1 = ether.getBalance( usdcOnEther, owner, 6);
//		S.out( v1);

		NodeInstance pulse = new NodeInstance( pulseNode, 10);
		S.out( "Prod owner wallet on PulseChain");
		double usdcBal = pulse.getBalance( usdcOnPulse, prodOwner, 6);
		S.out( "USDC: %s", usdcBal);
		S.out( "PLS: %s", pulse.getNativeBalance( prodOwner) );
		

		//Erc20 usdcPulse = new Erc20( usdcOnPulse, 6, "USDC-PULSE");
//		Config polyConfig = Config.ask( "Prod");
//		Config pulseConfig = Config.ask( "Pulse");
//		pulseConfig.busd().transfer( polyConfig.ownerKey(), pulseConfig.refWalletAddr(), usdcBal).waitForHash();
//		pulseConfig.node().showTrans( "0x966454dCA56f75aB15Df54cee9033062D331e0d4");
//		
//		OStream os = new OStream( "c:/temp/f.t");
//		os.write( pulseConfig.node().getQueuedTrans().toString() );

	}

}


// did 400 on tokensex from ether/owner to pulse
// now you are out of gas
// check the fees afterwards
