package zksync;

import java.math.BigInteger;
import java.util.HexFormat;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import common.Util;
import fireblocks.Fireblocks;
import io.zksync.crypto.signer.PrivateKeyEthSigner;
import io.zksync.protocol.ZkSync;
import io.zksync.protocol.core.Token;
import io.zksync.transaction.fee.DefaultTransactionFeeProvider;
import io.zksync.transaction.manager.ZkSyncTransactionManager;
import io.zksync.utils.ContractDeployer;
import reflection.Config;

public class TestZkSync {


	public static void main(String[] args) throws Exception {
		
		// Step 1: Set up zkSync provider
		var zkSync = ZkSync.build(new HttpService("https://mainnet.era.zksync.io"));

		// Step 2: Load credentials (private key of the deployer)
		Config config = Config.ask( "zksync");

		Credentials credentials = Credentials.create(config.ownerKey());
		PrivateKeyEthSigner ethSigner = new PrivateKeyEthSigner(credentials, 324);
		
		DefaultTransactionFeeProvider feeProvider = new DefaultTransactionFeeProvider(zkSync, Token.ETH);

		ZkSyncTransactionManager transactionManager = new ZkSyncTransactionManager(zkSync, ethSigner, feeProvider);

		String params = Fireblocks.encodeParameters(
				Util.toArray( "address", "address"),
				Util.toArray( config.refWalletAddr(), config.admin1Addr() ) );

		// pick one!
//		var bytecode = HexFormat.of().parseHex(hex); 
//		Numeric.hexStringToByteArray(hex);
//		Encrypt.hexToBytes(hex);

		Function createFunction = ContractDeployer.encodeCreate(  // for CREATE2, use encodeCreate2()
				Numeric.hexStringToByteArray(Zksync.rusd), 
				Numeric.hexStringToByteArray(params)
				);  

		String encodedData = FunctionEncoder.encode(createFunction);
		
		var newAddr = ContractDeployer.computeL2CreateAddress(
				new Address( config.ownerAddr() ),
				config.node().getNonce( config.ownerAddr() ) );
		
		long max = 110000000L;
		
		EthSendTransaction transactionResponse = transactionManager.sendTransaction(
				BigInteger.valueOf( max),
				null, 
				newAddr.toString(),  // No 'to' address for contract creation
				encodedData, 
				BigInteger.ZERO,  // No Ether value
				false // Indicates contract creation
				);

		// Step 12: Retrieve transaction hash
		String txHash = transactionResponse.getTransactionHash();
		System.out.println("Transaction Hash: " + txHash);

		// Step 13: Wait for the contract to be mined
		EthGetTransactionReceipt receipt = zkSync.ethGetTransactionReceipt(txHash).send();
		if (receipt.getTransactionReceipt().isPresent()) {
			System.out.println("Contract deployed at address: " + receipt.getTransactionReceipt().get().getContractAddress());
		} else {
			System.out.println("Transaction pending...");
		}
	}
}
