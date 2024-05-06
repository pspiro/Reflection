package web3;

import java.math.BigDecimal;
import java.math.BigInteger;

import fireblocks.FbErc20;
import tw.util.S;

/** rusd, etc should inherit from this, or Erc20 should inherit from this */
public class MyCoreBase {
	protected static final BigDecimal ten = new BigDecimal(10);

	protected String m_address;
	protected int m_decimals;
	protected String m_name;

	protected MyCoreBase( String address, int decimals, String name) {
		m_address = address;
		m_decimals = decimals;
		m_name = name;
	}
	
	protected BigInteger toBlockchain(double amt) {
		return timesPower( amt, m_decimals); 
	}

	/** Return amt rounded to four decimals * 10^power */
	public static BigInteger timesPower(double amt, int power) {
		return new BigDecimal( S.fmt4( amt) )
				.multiply( ten.pow( power) )
				.toBigInteger();
	}
	
	public String address() {
		return m_address;
	}
	
	public int decimals() {
		return m_decimals;
	}
	
	public String name() {
		return m_name;
	}

}
