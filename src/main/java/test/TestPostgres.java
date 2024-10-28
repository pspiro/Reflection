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
		Config c = Config.ask("prod");
		S.out( c.admin1Key() );
		
		Config c2 = Config.ask("pulse");
		S.out( c2.admin1Key() );

		Config c3 = Config.ask("zksync");
		S.out( c3.admin1Key() );
	}
}
