package test;

import java.math.BigInteger;
import java.util.Arrays;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

import http.MyClient;



/** Just test that you can connect to the database. */
public class TestPostgres {



	// Placeholder for obtaining the transaction count (nonce) for the sender
	private BigInteger getTransactionCount(String address) {
		// In a real implementation, this would query the Ethereum node for the nonce of the sender
		return BigInteger.ZERO;  // Dummy value for the sake of this example
	}

	public static void main(String[] args) throws Exception {
		

	}
}
