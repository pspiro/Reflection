package test;

import java.math.BigInteger;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

import common.Util;
import io.zksync.protocol.ZkSync;
import io.zksync.wrappers.ERC20;
import refblocks.RbBusd;
import reflection.Config;
import tw.util.S;
import web3.Erc20;
import web3.Param;
import web3.Param.Address;
import web3.Param.BigInt;



/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		Config c = Config.ask("zksync");
		ZkSync zkSync = ZkSync.build(new HttpService("https://mainnet.era.zksync.io"));

//		ERC20 er = ERC20.load( 
//				c.busdAddr(), 
//				zkSync, 
//				Credentials.create( c.refWalletKey()),
//				new DefaultGasProvider()
//				);
//		TransactionReceipt ret = er.approve( c.rusdAddr(), BigInteger.valueOf( 1000000) ).send();
//		S.out( ret);
		
		new RbBusd( c.busdAddr(), c.busd().decimals(), c.busd().name() )
			.approve( c.refWalletKey(), c.rusdAddr(), 1000000)
			.waitForReceipt();


		S.out( c.node().getDecimals( c.busdAddr() ) );
				
		Param[] params = {
				new Address( c.rusdAddr() ),
				new BigInt( Erc20.toBlockchain( 1000000, 6) )
		};
		

		// this works
//		c.node().callSigned(
//				c.refWalletKey(),
//				c.busdAddr(),
//				Erc20.Approve,
//				params,
//				200000)  // 500k works, 100k does not
//			.displayHash();
		
		S.out( c.getApprovedAmt() );
		
//		c.node().getRevertReason(
//				c.refWalletAddr(),
//				c.busdAddr(),
//				Erc20.Approve,
//				params
//				);
		

	}
}
