package web3;

import java.math.BigInteger;

import fireblocks.Fireblocks;

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
				data.append(Fireblocks.padAddr( address.value) );  // Address is 20 bytes, but we pad to 32 bytes (64 hex chars)
			}
			else if (param instanceof BigInt intVal) {
				data.append(Fireblocks.padBigInt(intVal.value) );  // BigInt is 32 bytes
			}
		}

		return "0x" + data.toString();  // Prepend 0x to the encoded data
	}


}
