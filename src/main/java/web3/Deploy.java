package web3;


import org.json.simple.JsonObject;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import chain.Chain;
import chain.Chains;
import common.Util;
import io.zksync.protocol.ZkSync;
import io.zksync.protocol.account.Wallet;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;

/* use CreateKey class to generate wallet private keys
 * 
 *  deploys using Web3 classes; you could easily read bytecode from folder if desired */

public class Deploy {
	static Chain chain;
	
	// deploy RUSD, fake BUSD (for test system), and all stock tokens
	
	// NOTE - you must have gas in the admin1, owner, and refWallet
	// NOTE - deployed address for RUSD and BUSD are not entered into the spreadsheet
	public static void main(String[] args) throws Exception {
		chain = Chains.readOne( "Amoy", false);

		String rusdAddress = chain.rusd().address();
		String busdAddress = chain.busd().address();
		
		S.out( "Deploying system");
		
		S.out( "Owner gas: %s", chain.node().getNativeBalance( chain.params().ownerAddr() ) );
		S.out( "RefWallet gas: %s", chain.node().getNativeBalance( chain.params().refWalletAddr() ) );
		S.out( "Admin1 gas: %s", chain.node().getNativeBalance( chain.params().admin1Addr() ) );
		
		boolean deployedOne = false;

		// deploy BUSD? (for testing only)
		// note that the number of decimals is set in the .sol file before the Busd file is generaged */
		if ("deploy".equals( busdAddress) ) {
			busdAddress = deployBusd( chain.blocks(), chain.params().ownerKey(), chain.params().refWalletAddr(), chain.params().admin1Addr() );
			
			S.out( "deployed busd to " + busdAddress);
			chain.setBusdAddress( busdAddress);  // does not update spreadsheet or params
			deployedOne = true;
		}
		else {
			Util.require( Util.isValidAddress( busdAddress), "BUSD must be valid or set to 'deploy'");
		}
		
		// deploy RUSD (if set to "deploy")
		if ("deploy".equalsIgnoreCase( rusdAddress) ) {
			if (chain.params().isZksync() ) {
				rusdAddress = deployRusdZksync();
			}
			else {
				rusdAddress = chain.rusd().deploy( chain.params().ownerKey() );
				S.out( "deployed rusd to " + rusdAddress);
			}
			deployedOne = true;
			chain.setRusdAddress( rusdAddress);  // does not update spreadsheet or params

			// add a sysAdmin (used by Monitor)
			chain.rusd().addOrRemoveAdmin( chain.params().ownerKey(), chain.params().sysAdminAddr(), true)
				.waitForReceipt();
		}
		else {
			Util.require( Util.isValidAddress( rusdAddress), "RUSD must be valid or set to 'deploy'");
		}
		
		// if we deployed either RUSD or BUSD, we must approve or re-approve
		if (deployedOne) {
			// let RefWallet approve RUSD to spend BUSD; this works as of 7/29/24
			chain.busd().approve( chain.params().refWalletKey(), rusdAddress, 1000000) // $1M
				.waitForReceipt();
		}
		
		// deploy stock tokens where address is set to deploy (should be inactive to prevent errors in RefAPI)
		for (ListEntry row : NewSheet.getTab( NewSheet.Reflection, chain.params().symbolsTab() ).fetchRows(false) ) {
			if (row.getString( "Token Address").equalsIgnoreCase("deploy") ) {
				// deploy stock token
				String address = chain.params().isZksync() 
						? deployStockZksync( 
								row.getString( "Token Name"),  // wrong, this should get pulled from master symbols tab
								row.getString( "Token Symbol"),
								rusdAddress
								)
						: deployStock(
								chain,
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
	private static String deployRusdZksync() throws Exception {
		String file = "C:\\Work\\zk3\\artifacts-zk\\contracts\\RUSD.sol\\rusd.json";
		
		String byteCode = JsonObject.readFromFile( file).getString( "bytecode");
		Util.require( S.isNotNull( byteCode), "no bytecode");
		Util.require( (byteCode.length() - 2) / 2 % 32 == 0, "bytecode length must be divisible by 32; make sure to compile for zksync");

		S.out( "deploying RUSD on zksync from " + file);

		String params = Param.encodeParameters(
				Util.toArray( "address", "address"),
				Util.toArray( chain.params().refWalletAddr(), chain.params().admin1Addr() ) );

		String addr = deployZksync( chain.params().ownerKey(), byteCode, params);

		// confirm that we can interact w/ contract 
		Util.require( chain.node().getTokenDecimals( addr) == 6, "wrong number of decimals");

		return addr;
	}

	private static String deployStockZksync( String name, String symbol, String rusdAddr) throws Exception {
		String file = "C:\\Work\\zk3\\artifacts-zk\\contracts\\StockToken.sol\\StockToken.json";
		
		String byteCode = JsonObject.readFromFile( file).getString( "bytecode");
		Util.require( S.isNotNull( byteCode), "no bytecode");
		Util.require( (byteCode.length() - 2) / 2 % 32 == 0, "bytecode length must be divisible by 32; make sure to compile for zksync");

		S.out( "deploying StockToken on zksync from " + file);

		String params = Param.encodeParameters(
				Util.toArray( "string", "string", "address"),
				Util.toArray( name, symbol, rusdAddr) );
		
		String addr = deployZksync( chain.params().ownerKey(), byteCode, params);

		// confirm that we can interact w/ contract 
		Util.require( chain.node().getTokenDecimals( addr) == 18, "wrong number of decimals");

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

	/** move into Busd class */
	private static String deployBusd(Refblocks blocks, String ownerKey, String refWalletAddr, String admin1Addr) throws Exception {
		String file = "C:\\Work\\smart-contracts\\build\\contracts\\busd2.json";
		String bytecode = JsonObject.readFromFile( file).getString( "bytecode");
		Util.require( S.isNotNull( bytecode), "no bytecode");
		Util.require( (bytecode.length() - 2) % 2 == 0, "bytecode length must be divisible by 2");

		S.out( "deploying BUSD from " + file);

		String address = chain.node().deploy( chain.params().ownerKey(), bytecode, "");

		Util.require( chain.node().getTokenDecimals( address) == chain.busd().decimals(), "wrong number of decimals");

		return address;
	}
	
	/** move into stock token */
	private static String deployStock( Chain chain, String name, String symbol, String rusdAddr) throws Exception {
		String file = "C:\\Work\\smart-contracts\\build\\contracts\\StockToken.json";
		
		String bytecode = JsonObject.readFromFile( file).getString( "bytecode");
		Util.require( S.isNotNull( bytecode), "no bytecode");
		Util.require( (bytecode.length() - 2) % 2 == 0, "bytecode length must be divisible by 2");

		S.out( "deploying StockToken from " + file);

		String params = Param.encodeParameters(
				Util.toArray( "string", "string", "address"),
				Util.toArray( name, symbol, rusdAddr) );
		
		String address = chain.node().deploy( chain.params().ownerKey(), bytecode, params);

		// confirm that we can interact w/ contract 
		Util.require( chain.node().getTokenDecimals( address) == 18, "wrong number of decimals");

		return address;
	}
}
