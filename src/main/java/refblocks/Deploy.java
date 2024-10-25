package refblocks;


import org.json.simple.JsonObject;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import common.Util;
import fireblocks.Fireblocks;
import io.zksync.protocol.ZkSync;
import io.zksync.protocol.account.Wallet;
import reflection.Config;
import reflection.Config.Web3Type;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;

public class Deploy {
	
	// deploy RUSD, fake BUSD (for test system), and all stock tokens
	
	// NOTE - CREATE THE REFWALLET FIRST AND GIVE IT SOME GAS
	// you must have gas in the admin1, owner, and refWallet
	public static void main(String[] args) throws Exception {
		Config config = Config.ask();
		Util.require(config.web3Type() == Web3Type.Refblocks, "Turn on Refblocks");
		
		String rusdAddress = config.rusd().address();
		String busdAddress = config.busd().address();
		
		S.out( "Deploying system");

		// deploy BUSD? (for testing only)
		// note that the number of decimals is set in the .sol file before the Busd file is generaged */
		if ("deploy".equals( busdAddress) ) {
			busdAddress = RbBusd.deploy( config.ownerKey() );
			S.out( "deployed busd to " + busdAddress);
			config.setBusdAddress( busdAddress);  // update spreadsheet with deployed address
		}
		else {
			Util.require( Util.isValidAddress( busdAddress), "BUSD must be valid or set to 'deploy'");
		}
		
		// deploy RUSD (if set to "deploy")
		if ("deploy".equalsIgnoreCase( rusdAddress) ) {
			if (config.isZksync() ) {
				rusdAddress = deployRusdZksync(config);
			}
			else {
				rusdAddress = RbRusd.deploy( config.ownerKey(), config.refWalletAddr(), config.admin1Addr() );
				S.out( "deployed rusd to " + rusdAddress);
			}
			config.setRusdAddress( rusdAddress);  // update spreadsheet with deployed address

			// transfer some gas to RefWallet if needed
			//config.matic().transfer( config.ownerKey(), config.refWalletAddr(), .005);

			// let RefWallet approve RUSD to spend BUSD;
			// because the method signature is the same
			// this works as of 7/29/24
			new RbBusd( busdAddress, config.busd().decimals(), config.busd().name() )
				.approve( config.refWalletKey(), rusdAddress, 1000000); // $1M

			// add a second admin
//			rusd.addOrRemoveAdmin(
//					instance.getAddress( "Admin2"), 
//					true);
		}
		else {
			Util.require( Util.isValidAddress( rusdAddress), "RUSD must be valid or set to 'deploy'");
		}
		
		// deploy stock tokens where address is set to deploy (should be inactive to prevent errors in RefAPI)
		for (ListEntry row : NewSheet.getTab( NewSheet.Reflection, config.symbolsTab() ).fetchRows(false) ) {
			if (row.getString( "Token Address").equalsIgnoreCase("deploy") ) {
				MyContract.deployAddress = Util.createFakeAddress();
				
				// deploy stock token
				String address = config.isZksync() 
						? deployStockZksync( config, 
								row.getString( "Token Name"),  // wrong, this should get pulled from master symbols tab
								row.getString( "Token Symbol"),
								rusdAddress
								)
						: RbStockToken.deploy(
								config.ownerKey(),
								row.getString( "Token Name"),  // wrong, this should get pulled from master symbols tab
								row.getString( "Token Symbol"),
								rusdAddress
								);
				
				// update row on Symbols tab with new stock token address
				S.out( "deployed stock token to " + address);
				row.setValue( "Token Address", address);
				row.update();
			}
		}
		
		//Test.run(config, busd, rusd);
	}
	
	/** @return deployed RUSD contract address */
	private static String deployRusdZksync(Config config) throws Exception {
		String file = "C:\\Work\\zk3\\artifacts-zk\\contracts\\RUSD.sol\\rusd.json";
		
		String hex = JsonObject.readFromFile( file).getString( "bytecode");
		Util.require( S.isNotNull( hex), "no bytecode");
		Util.require( (hex.length() - 2) / 2 % 32 == 0, "bytecode length must be divisible by 32; make sure to compile for zksync");

		S.out( "deploying RUSD on zksync from " + file);

		String params = Fireblocks.encodeParameters(
				Util.toArray( "address", "address"),
				Util.toArray( config.refWalletAddr(), config.admin1Addr() ) );

		String addr = deployZksync( config.ownerKey(), hex, params);

		// confirm that we can interact w/ contract 
		Util.require( config.node().getTokenDecimals( addr) == 6, "wrong number of decimals");

		return addr;
	}

	private static String deployStockZksync(Config config, String name, String symbol, String rusdAddr) throws Exception {
		String file = "C:\\Work\\zk3\\artifacts-zk\\contracts\\StockToken.sol\\StockToken.json";
		
		String hex = JsonObject.readFromFile( file).getString( "bytecode");
		Util.require( S.isNotNull( hex), "no bytecode");
		Util.require( (hex.length() - 2) / 2 % 32 == 0, "bytecode length must be divisible by 32; make sure to compile for zksync");

		S.out( "deploying StockToken on zksync from " + file);

		// set up zkSync and ether providers
		ZkSync zkSync = ZkSync.build(new HttpService("https://mainnet.era.zksync.io"));
		Web3j web3j = Web3j.build( new HttpService( "https://eth.llamarpc.com") );

		String params = Fireblocks.encodeParameters(
				Util.toArray( "string", "string", "address"),
				Util.toArray( name, symbol, rusdAddr) );
		
		String addr = deployZksync( config.ownerKey(), hex, params);

		// confirm that we can interact w/ contract 
		Util.require( config.node().getTokenDecimals( addr) == 18, "wrong number of decimals");

		return addr;
	}

	private static String deployZksync( String ownerKey, String hex, String params) throws Exception {
		ZkSync zkSync = ZkSync.build(new HttpService("https://mainnet.era.zksync.io"));
		Web3j web3j = Web3j.build( new HttpService( "https://eth.llamarpc.com") );

		Credentials credentials = Credentials.create(ownerKey);
		Wallet wallet = new Wallet(web3j, zkSync, credentials);

		// deploy it
		TransactionReceipt retval = wallet.deployAccount(
        		Numeric.hexStringToByteArray(hex), 
        		Numeric.hexStringToByteArray(params)
        		).send();
		
		var txHash = retval.getTransactionHash();
		S.out( "  transHash: " + txHash);
		S.out( "  deployed to: " + retval.getContractAddress() );

		return retval.getContractAddress();
	}
}
