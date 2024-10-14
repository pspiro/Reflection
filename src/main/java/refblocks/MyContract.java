package refblocks;

import java.io.IOException;
import java.math.BigInteger;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import common.Util;
import common.Util.ExSupplier;
import tw.util.S;
import web3.RetVal;

/** this class adds TWO pieces of functionality:
 * 
 *  1. calling eth_call BEFORE the real call to anticipate any errors, which we might not want
 *  2. calling eth_call AFTER the call to find out what went wrong  */
public class MyContract extends Contract {
	private Function m_function;
	
	public MyContract(String bin, String addr, Web3j web3j, TransactionManager tm, ContractGasProvider gp) {
		super( bin, addr, web3j, tm, gp);
	}

	public MyContract(String bin, String addr, Web3j web3j, TransactionManager tm, BigInteger gasPrice, BigInteger gasLimit) {
		super( bin, addr, web3j, tm, gasPrice, gasLimit);
	}

	public MyContract(String bin, String addr, Web3j web3j, Credentials credentials, ContractGasProvider gp) {
		super( bin, addr, web3j, credentials, gp);
	}

	public MyContract(String bin, String addr, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
		super( bin, addr, web3j, credentials, gasPrice, gasLimit);
	}

	protected Function function() {
		return m_function;
	}
	
    @Override 
    protected RemoteFunctionCall<TransactionReceipt> executeRemoteCallTransaction( Function function) {
    	m_function = function;
        return super.executeRemoteCallTransaction( function);
    }

    /** @param method is smart contract method */
    protected RetVal exec( ExSupplier<RemoteFunctionCall<TransactionReceipt>> method) throws Exception {
    	return exec2( method, false);
	}

    /** call ethCall BEFORE transaction to anticipate an error
     * 
     * @param method is smart contract method */
    protected RetVal exec2( ExSupplier<RemoteFunctionCall<TransactionReceipt>> method, boolean preTest) throws Exception {
		Util.require( !preTest, "this needs testing before usage");
			
		// call ethCall BEFORE the transaction to anticipate an error
		if (preTest) {
			preTest();
		}
			
		// make the actual method call with signature
		try {
	        var receipt = method.get().send();  // << m_function gets set here
	        return new RbRetVal( receipt, m_function);
		}
		catch( Exception e) {
			S.out( "Error occurred while calling smart contract method '%s' - %s",
					name(), e.getMessage() ); 
			throw e;
		}
	}

	/** pre-test the method by calling eth_call on latest block with same parameters */
    private void preTest() throws Exception {
		String reason;

		try {
    		reason = web3j.ethCall(
    				Transaction.createEthCallTransaction(
    						transactionManager.getFromAddress(),
    						contractAddress,
    						FunctionEncoder.encode(m_function)
    						),
    				DefaultBlockParameter.valueOf( DefaultBlockParameterName.LATEST.getValue() )
    				)
    				.send()
    				.getRevertReason();
		}
    	catch( Exception e) {
    		S.out( "Error occurred while pre-testing smart contract method '%s' - %s",
    				name(), e.getMessage() ); 
    		throw e;
    	}

		if (S.isNotNull( reason) ) {
			S.out( "we anticipate an error");
			throw new Exception( reason);
		}
	}

	private String name() {
		return m_function != null ? m_function.getName() : "unknown";
	}

	public static String deployAddress;  // used only during deployment; not needed or used, i think
	
    protected TransactionReceipt sendEIP1559(
            long chainId,
            String to,
            String data,
            BigInteger value,
            BigInteger gasLimit,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas,
            boolean constructor)
            throws IOException, TransactionException {

    	return super.sendEIP1559(
                chainId,
                S.isNull( to) && S.isNotNull( deployAddress) ? deployAddress : to,
                data,
                value,
                gasLimit,
                maxPriorityFeePerGas,
                maxFeePerGas,
                constructor);
   	}


}
