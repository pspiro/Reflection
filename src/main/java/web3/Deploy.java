package web3;


import org.json.simple.JsonObject;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import chain.Chain;
import common.MyScanner;
import common.Util;
import io.zksync.protocol.ZkSync;
import io.zksync.protocol.account.Wallet;
import refblocks.MyContract;
import refblocks.Refblocks;
import refblocks.Rusd;
import refblocks.Stocktoken;
import reflection.SingleChainConfig;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;

public class Deploy {
	
	// deploy RUSD, fake BUSD (for test system), and all stock tokens
	
	// NOTE - CREATE THE REFWALLET FIRST AND GIVE IT SOME GAS
	// you must have gas in the admin1, owner, and refWallet
	public static void main(String[] args) throws Exception {
		SingleChainConfig config = SingleChainConfig.ask();
		
		String rusdAddress = config.rusd().address();
		String busdAddress = config.busd().address();
		
		S.out( "Deploying system");

		// deploy BUSD? (for testing only)
		// note that the number of decimals is set in the .sol file before the Busd file is generaged */
		if ("deploy".equals( busdAddress) ) {
			busdAddress = deployRu( config.chain().blocks(), config.ownerKey(), config.refWalletAddr(), config.admin1Addr() );
			
			S.out( "deployed busd to " + busdAddress);
			config.setBusdAddress( busdAddress);  // update spreadsheet with deployed address
		}
		else {
			Util.require( Util.isValidAddress( busdAddress), "BUSD must be valid or set to 'deploy'");
		}
		
		// deploy RUSD (if set to "deploy")
		if ("deploy".equalsIgnoreCase( rusdAddress) ) {
			if (config.chain().params().isZksync() ) {
				rusdAddress = deployRusdZksync(config);
			}
			else {
				rusdAddress = config.chain().rusd().deploy( config.ownerKey(), config.refWalletAddr(), config.admin1Addr() );
				S.out( "deployed rusd to " + rusdAddress);
			}
			config.setRusdAddress( rusdAddress);  // update spreadsheet with deployed address

			// transfer some gas to RefWallet if needed
			//config.matic().transfer( config.ownerKey(), config.refWalletAddr(), .005);

			// let RefWallet approve RUSD to spend BUSD;
			// because the method signature is the same
			// this works as of 7/29/24
			config.chain().busd().approve( config.refWalletKey(), rusdAddress, 1000000); // $1M

			// add a second admin
//			rusd.addOrRemoveAdmin(
//					instance.getAddress( "Admin2"), 
//					true);
		}
		else {
			Util.require( Util.isValidAddress( rusdAddress), "RUSD must be valid or set to 'deploy'");
		}
		
		// deploy stock tokens where address is set to deploy (should be inactive to prevent errors in RefAPI)
		for (ListEntry row : NewSheet.getTab( NewSheet.Reflection, config.chain().params().symbolsTab() ).fetchRows(false) ) {
			if (row.getString( "Token Address").equalsIgnoreCase("deploy") ) {
				MyContract.deployAddress = Util.createFakeAddress();
				
				// deploy stock token
				String address = config.chain().params().isZksync() 
						? deployStockZksync( config, 
								row.getString( "Token Name"),  // wrong, this should get pulled from master symbols tab
								row.getString( "Token Symbol"),
								rusdAddress
								)
						: deploySt(
								config.chain(),
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
	private static String deployRusdZksync(SingleChainConfig config) throws Exception {
		String file = "C:\\Work\\zk3\\artifacts-zk\\contracts\\RUSD.sol\\rusd.json";
		
		String hex = JsonObject.readFromFile( file).getString( "bytecode");
		Util.require( S.isNotNull( hex), "no bytecode");
		Util.require( (hex.length() - 2) / 2 % 32 == 0, "bytecode length must be divisible by 32; make sure to compile for zksync");

		S.out( "deploying RUSD on zksync from " + file);

		String params = Param.encodeParameters(
				Util.toArray( "address", "address"),
				Util.toArray( config.refWalletAddr(), config.admin1Addr() ) );

		String addr = deployZksync( config.ownerKey(), hex, params);

		// confirm that we can interact w/ contract 
		Util.require( config.node().getTokenDecimals( addr) == 6, "wrong number of decimals");

		return addr;
	}

	private static String deployStockZksync(SingleChainConfig config, String name, String symbol, String rusdAddr) throws Exception {
		String file = "C:\\Work\\zk3\\artifacts-zk\\contracts\\StockToken.sol\\StockToken.json";
		
		String hex = JsonObject.readFromFile( file).getString( "bytecode");
		Util.require( S.isNotNull( hex), "no bytecode");
		Util.require( (hex.length() - 2) / 2 % 32 == 0, "bytecode length must be divisible by 32; make sure to compile for zksync");

		S.out( "deploying StockToken on zksync from " + file);

		String params = Param.encodeParameters(
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
	
	public static String deploySt( Chain chain, String ownerKey, String name, String symbol, String rusdAddr) throws Exception {
		S.out( "Deploying stock token  name=%s  symbol=%s  RUSD addr=%s", name, symbol, rusdAddr); 
		
		return Stocktoken.deploy( 
				chain.web3j(),
				chain.blocks().getWaitingTm( ownerKey),
				chain.blocks().getGp( Refblocks.deployGas),
				name, symbol, rusdAddr
				).send().getContractAddress();
	}
	
	private static String deployRu(Refblocks blocks, String ownerKey, String refWallet, String admin1) throws Exception {
		return Rusd.deploy( 
				blocks.web3j(),
				blocks.getWaitingTm( ownerKey),
				blocks.getGp( Refblocks.deployGas),
				refWallet,
				admin1)
			.send().getContractAddress();
	}
	
	public static void createSystemWallets() throws Exception {
		try (MyScanner scanner = new MyScanner() ) {
			String pw1 = scanner.input( "Enter password: ");
			String pw2 = scanner.input( "Re-enter password: ");
			Util.require( pw1.equals( pw2), "Mismatch");
			String hint = scanner.input( "Enter pw hint: ");
			
//			createWallet( pw1, hint, "Owner");
//			createWallet( pw1, hint, "RefWallet");
//			createWallet( pw1, hint, "Admin1");
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}

}
