package web3;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import tw.util.S;

public class Encrypt {
	static String password = "hello";
	static String salt = "uiowehfjxncvksufhgrwejgfuy";  // this is combined w/ the password to make it unique
	static String algorithm = "AES/CBC/PKCS5Padding";

	
	// I want: 
	// RS256 (RSASSA-PKCS1-v1_5 using SHA-256 hash)
	public static void main(String[] args) throws Exception {
		S.out( encode(
				getKeyFromPassword("hello", "ab").getEncoded() ) );

		String input = "baeloiweukjlnsdkjljweoirjsldhfwqeoirjlsjlksddung";
		SecretKey key = getKeyFromPassword(password, salt);
		IvParameterSpec iv = generateIv();
		String ivstr = encode( iv.getIV() );  // this is data added to the input to make it unique
		
		String encrypted = encrypt( input, key, iv);
		S.out( "iv: %s", encode( iv.getIV()) );
		S.out( "encrypted: %s", encrypted);

		String plainText = decrypt( encrypted, key, ivstr);
		S.out( "decrypted: %s", plainText);
	}

	public static SecretKey getKeyFromPassword(String password, String salt) throws Exception {
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
		return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
	}

	public static IvParameterSpec generateIv() {
		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		return new IvParameterSpec(iv);
	}	

	public static String encrypt( String input, SecretKey key, IvParameterSpec ivspec) throws Exception {
		Cipher cipher = Cipher.getInstance(algorithm);
		cipher.init(Cipher.ENCRYPT_MODE, key, ivspec);
		return encode( cipher.doFinal(input.getBytes()) );
	}

	public static String decrypt( String cipherText, SecretKey key, String ivstr) throws Exception {
		IvParameterSpec ivspec = new IvParameterSpec( decode(ivstr) );
		Cipher cipher = Cipher.getInstance(algorithm);
		cipher.init(Cipher.DECRYPT_MODE, key, ivspec);
		byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));
		return new String(plainText);
	}
	
	public static String encode( String in) {
		return encode( in.getBytes() );
	}
	static String encode( byte[] ar) {
		return strip( Base64.getEncoder().encodeToString(ar) );	
	}
	static byte[] decode( String str) {
		return Base64.getDecoder().decode(str);	
	}
	static String strip( String str) {
		while (str.charAt( str.length()-1) == '=') {
			str = str.substring(0, str.length() - 1);
		}
		return str;
	}

	/** Returns result as hex; you might want it in Base64, see below */
	public static String getSHA256(String input) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
		return bytesToHex( bytes);
	}
	
	public static String getSHA1(String input) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(bytes);
	}
	
	public static String bytesToHex(byte[] hash) {
		StringBuilder hexString = new StringBuilder(2 * hash.length);
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			if(hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}

	/** RSASSA-PKCS1-v1_5 using SHA-256 hash */
	public static String signRSA(String input, String key) throws Exception {
		byte[] b1 = Base64.getDecoder().decode(key);
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(b1);

		Signature sig = Signature.getInstance("SHA256withRSA");
		sig.initSign(KeyFactory.getInstance("RSA").generatePrivate(spec));
		sig.update(input.getBytes("UTF-8"));
		return Encrypt.encode( sig.sign() );
	}
	
}
