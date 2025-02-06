package web3;

import java.math.BigInteger;

import common.Util;

public class Nonce {
	// for testing only
	public static BigInteger plus = BigInteger.ZERO;
	
	private static final long Interval = Util.MINUTE;

	private String m_wallet;
	private BigInteger m_nonce = BigInteger.ZERO;
	private long m_timestamp; // time of last successful call

	public Nonce(String wallet) {
		m_wallet = wallet;
	}

	public synchronized RetVal callSigned(NodeInstance node, String privateKey, String contractAddr, BigInteger amtToSend, String data, long gasLimit) throws Exception {
		long now = System.currentTimeMillis();

		if (now - m_timestamp >= Interval) {
			m_nonce = node.getNonceLatest( m_wallet);
		}

		try {
			var retVal = node.callSigned( privateKey, contractAddr, amtToSend, data, gasLimit, m_nonce);
			m_nonce = m_nonce.add( BigInteger.ONE);
			m_timestamp = now;
			return retVal;
		}
		catch( Exception e) {
			m_timestamp = 0;
			// there are some errors where we would want to increment the nonce, 
			// such as 'seen that' --OR-- we could say, with any error, let it query next time  
			throw e;
		}
	}
}
