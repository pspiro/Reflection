package util;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
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
	static String salt = "uiowehfjxncvksufhgrwejgfuy";
	static String algorithm = "AES/CBC/PKCS5Padding";

	public static void main(String[] args) throws Exception {
		S.out( tostring(
				getKeyFromPassword("hello", "ab").getEncoded() ) );

		String input = "baeloiweukjlnsdkjljweoirjsldhfwqeoirjlsjlksddung";
		SecretKey key = getKeyFromPassword(password, salt);
		IvParameterSpec iv = generateIv();
		String ivstr = tostring( iv.getIV() );
		
		String encrypted = encrypt( input, key, iv);
		S.out( "iv: %s", tostring( iv.getIV()) );
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
		return tostring( cipher.doFinal(input.getBytes()) );
	}

	public static String decrypt( String cipherText, SecretKey key, String ivstr) throws Exception {
		IvParameterSpec ivspec = new IvParameterSpec( tobytes(ivstr) );
		Cipher cipher = Cipher.getInstance(algorithm);
		cipher.init(Cipher.DECRYPT_MODE, key, ivspec);
		byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));
		return new String(plainText);
	}
	
	static String tostring( byte[] ar) {
		return Base64.getEncoder().encodeToString(ar);	
	}
	static byte[] tobytes( String str) {
		return Base64.getDecoder().decode(str);	
	}
}
/*
	public String encrypt(String data) {
		String encryptedText = "";

		if (data == null || secretAes256Key == null) {
			return encryptedText;
		}

		try {
			Cipher encryptCipher = Cipher.getInstance(AES_TRANSFORMATION_MODE);
			encryptCipher.init(Cipher.ENCRYPT_MODE, secretAes256Key, new SecureRandom());//new IvParameterSpec(getIV()) - if you want custom IV

			//encrypted data:
			byte[] encryptedBytes = encryptCipher.doFinal(data.getBytes("UTF-8"));

			//take IV from this cipher
			byte[] iv = encryptCipher.getIV();

			//append Initiation Vector as a prefix to use it during decryption:
			byte[] combinedPayload = new byte[iv.length + encryptedBytes.length];

			//populate payload with prefix IV and encrypted data
			System.arraycopy(iv, 0, combinedPayload, 0, iv.length);
			System.arraycopy(encryptedBytes, 0, combinedPayload, iv.length, encryptedBytes.length);

			encryptedText = Base64.encodeToString(combinedPayload, Base64.DEFAULT);

		} 
		catch (Exception e) {
			e.printStackTrace();
		}

		return encryptedText;
	}

	public String decrypt(String encryptedString) {
		String decryptedText = "";

		if (encryptedString == null || secretAes256Key == null) 
			return decryptedText;
	}

	try {
		//separate prefix with IV from the rest of encrypted data
		byte[] encryptedPayload = Base64.decode(encryptedString, Base64.DEFAULT);
		byte[] iv = new byte[16];
		byte[] encryptedBytes = new byte[encryptedPayload.length - iv.length];

		//populate iv with bytes:
		System.arraycopy(encryptedPayload, 0, iv, 0, 16);

		//populate encryptedBytes with bytes:
		System.arraycopy(encryptedPayload, iv.length, encryptedBytes, 0, encryptedBytes.length);

		Cipher decryptCipher = Cipher.getInstance(AES_TRANSFORMATION_MODE);
		decryptCipher.init(Cipher.DECRYPT_MODE, secretAes256Key, new IvParameterSpec(iv));

		byte[] decryptedBytes = decryptCipher.doFinal(encryptedBytes);
		decryptedText = new String(decryptedBytes);

	} catch (NoSuchAlgorithmException | BadPaddingException | NoSuchPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException | InvalidKeyException e) {
		e.printStackTrace();
	}

	return decryptedText;
}
*/