package common;

import java.math.BigInteger;
import java.security.SecureRandom;

import tw.util.S;

public class CreateKey {
	static SecureRandom rnd = new SecureRandom();
	
	public static void main(String[] args) {
		for (int i = 0; i < 10; i++) {
			S.out( createPrivateKey() );
		}
	}
	
	public static String createPrivateKey() {
		// Generate a 256-bit (32 bytes) private key
		byte[] privateKeyBytes = new byte[32];
		rnd.nextBytes(privateKeyBytes);

		// Convert the byte array to a hexadecimal string
		String privateKey = new BigInteger(1, privateKeyBytes).toString(16);

		// Ensure the hexadecimal string is 64 characters long (pad with leading zeros if necessary)
		while (privateKey.length() < 64) {
			privateKey = "0" + privateKey;
		}
		
		return privateKey;
	}
}
