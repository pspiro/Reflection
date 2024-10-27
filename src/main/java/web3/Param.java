package web3;

import java.math.BigInteger;

import common.Util;
import reflection.RefException;

public class Param {

	public static class Address extends Param {
		String value;

		public Address(String value) {
			this.value = value;
		}
	}

	public static class BigInt extends Param {
		BigInteger value;

		public BigInt(BigInteger value) {
			this.value = value;
		}

		public BigInt(int value) {
			this.value = BigInteger.valueOf(value);
		}
	}
	
	public static String encodeData(String keccak, Param[] params) throws Exception {
		StringBuilder data = new StringBuilder(keccak);  // Start with the Keccak hash of the method signature

		for (Param param : params) {
			if (param instanceof Address address) {
				data.append(padAddr( address.value) );  // Address is 20 bytes, but we pad to 32 bytes (64 hex chars)
			}
			else if (param instanceof BigInt intVal) {
				data.append(padBigInt(intVal.value) );  // BigInt is 32 bytes
			}
		}

		return "0x" + data.toString();  // Prepend 0x to the encoded data
	}

	/** For encoding parameters for deployment or contract calls.
	 *  Assume all string length <= 32 bytes
	 *  Encoded as follows: 64 characters per line, one line per parameter
	 *  add the strings at the end: one line for the length, then the data  
	 * @throws RefException */
	public static String encodeParameters( String[] types, Object[] params) throws Exception {
		Util.require( 
				types == null && params == null || 
				types != null && params != null && types.length == params.length, 
				"types and params are out of sync");
		
		// no parameters passed?
		if (types == null) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		
		int offsetBytes = types.length * 32;
		
		// encode the parameters; strings just get a placeholder
		for (int i = 0; i < types.length; i++) {
			String type = types[i];
			Object val = params[i];
			
			if (type.equals("string") ) {
				Util.require( val instanceof String, "Wrong type");
				
				String str = (String)val;
				Util.require( str.length() <= 100, "That's an awfully long string");

				// encode the starting offset for the string
				sb.append( padInt( offsetBytes) );

				offsetBytes += 32 + stringBytes( str.length() );  // add 32 for the row which encodes the string length
			}
			else if (type.equals( "address") ) {
				sb.append( padAddr( (String)val) );
			}
			else if (type.equals( "uint256") ) {
				Util.require( val instanceof Integer || val instanceof BigInteger, "Bad parameter type " + val.getClass() );
				if (val instanceof Integer) {
					sb.append( padInt( (Integer)val) );
				}
				else {
					sb.append( padBigInt( (BigInteger)val ) );
				}
			}
			else {
				Util.require( false, "Unexpected Fireblocks parameter type " + type);
			}
		}
		
		// encode the strings
		for (int i = 0; i < types.length; i++) {
			String type = types[i];
			Object val = params[i];
			if (type.equals("string") ) {
				sb.append( padInt( ((String)val).length() ) );  // encode the length (one row)
				encodeString(sb, (String)val);   // encode the string (could be one or more rows)
			}
		}
		return sb.toString();
	}

	/** Encode the string one row of 32 bytes at a time */
	private static void encodeString(StringBuilder sb, String val) {
		while (val.length() > 0) {
			sb.append( padRight( stringToBytes( Util.left(val, 32) ) ) );
			val = Util.substring(val, 32);
		}
	}
	
	/** Return the number of rows it takes to encode the string * 32
	 *  as there are 32 bytes per row */
	private static int stringBytes(int length) {
		return (int)Math.ceil(length / 32.0) * 32;
	}
	
	/** Return each byte's hex value */
	public static String stringToBytes(String val) {
		StringBuilder sb = new StringBuilder();
		for (byte b : val.getBytes() ) {
			sb.append( String.format( "%2x", b) );
		}
		return sb.toString();
	}

	public static String padInt(int amt) {
		return padLeft( String.format( "%x", amt) );  // hex encoding
	}
	
	public static String padBigInt(BigInteger amt) {
		return padLeft( amt.toString(16) );  // hex encoding
	}
	
	/** Assume address starts with 0x */
	public static String padAddr(String addr) throws Exception {
		Util.reqValidAddress(addr);
		return padLeft( addr.substring(2) );
	}

	/** Pad with left zeros to 64 characters which is 32 bytes */
	public static String padLeft(String str) {
		return Util.padLeft( str, 64, '0'); 
	}
	
	private static String padRight(String str) {
		return Util.padRight( str, 64, '0'); 
	}

}
