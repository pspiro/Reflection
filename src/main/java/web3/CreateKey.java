package web3;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Scanner;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.json.simple.JsonObject;
import org.web3j.crypto.Credentials;

import common.Util;
import tw.util.IStream;
import tw.util.S;

public class CreateKey {
	static SecureRandom rnd = new SecureRandom();
	
	public static void main(String[] args) throws Exception {
//		createWalletsFromKey();
//		decrypt();
//		createSystemWallets();
//		verifyKey();
		showPks();
	}
	
	private static void showPks() throws Exception {
		;

		try(	Scanner scanner = new Scanner( System.in);
				IStream is = new IStream( "c:/temp/f.t") 
				) {
			
			String pw = input( scanner, "Enter password: ");
			
			String ln = is.readln();
			while (ln != null) {
				JsonObject obj = JsonObject.parse( ln);
				S.out( decryptFromJson(pw, obj));
				ln = is.readln();
			}
		}
	}



	static void verifyKey() throws Exception {
		try( Scanner scanner = new Scanner( System.in) ) {
			String pw = input( scanner, "Enter password: ");
			JsonObject json = JsonObject.parse( input( scanner, "Enter json: ") );
			String pk = decryptFromJson( pw, json);
			S.out( Util.getPublicKey(pk).equalsIgnoreCase(json.getString( "address")));
		}
	}
	
	private static void decrypt() throws Exception {
		try( Scanner scanner = new Scanner( System.in) ) {
			String pk = decryptFromJson(
					input( scanner, "Enter password: "),
					JsonObject.parse( input( scanner, "Enter json: ") )	);
			S.out( pk);
		}
	}

	/** Create all three system wallets from a single password */
	public static void createSystemWallets() throws Exception {
		try( Scanner scanner = new Scanner( System.in) ) {
			String pw1 = input( scanner, "Enter password: ");
			String pw2 = input( scanner, "Re-enter password: ");
			Util.require( pw1.equals( pw2), "Mismatch");

			createAndShowJson( pw1, "owner");
			createAndShowJson( pw1, "refWallet");
			createAndShowJson( pw1, "admin1");
		}
	}

	public static void createWalletsFromKey() throws Exception {
		try( Scanner scanner = new Scanner( System.in) ) {
			String pw1 = input( scanner, "Enter password: ");
			String pw2 = input( scanner, "Re-enter password: ");
			Util.require( pw1.equals( pw2), "Mismatch");

			for (int i = 0; i < 8; i++) {
				String privateKey = input( scanner, "Enter private key: ");
				Util.require( Util.isValidKey(privateKey), "Invalid key");
				
				createAndShowJson( pw1, "" + i, privateKey);
			}
		}
	}

	/** Create a single wallet from name entered */
	public static void createWalletGenKey() throws Exception {
		try( Scanner scanner = new Scanner( System.in) ) {
			String pw1 = input( scanner, "Enter password: ");
			String pw2 = input( scanner, "Re-enter password: ");
			Util.require( pw1.equals( pw2), "Mismatch");

			String name = input( scanner, "Enter wallet name: ");
			createAndShowJson( pw1, name);
		}
	}
	
	public static void createAndShowJson(String pw, String name, String privateKey) throws Exception {
		JsonObject json = getEncryptedPrivateKey( pw, privateKey);
		showWallet( json, name);
	}
	
	public static void createAndShowJson(String pw, String name) throws Exception {
		JsonObject json = getEncryptedPrivateKey( pw);
		showWallet( json, name);
	}

	/** formatted for prod wallets tab */
	public static void showWallet(JsonObject json, String name) throws Exception {
		System.out.println( String.format( "%s\t%s\t%s", name, json.getString( "address"), json.toString() ) );
	}
	
	/** fields in json are address, salt, data, ivstr;
	 *  it verifies that the correct password is used */ 
	public static String decryptFromJson( String pw, JsonObject json) throws Exception {
		try {
			String pk = Encrypt.decrypt( 
					json.getString( "data"),
					Encrypt.getKeyFromPassword( pw, json.getString( "salt") ),
					json.getString( "ivstr") );
			Util.require( Util.getPublicKey( pk).equalsIgnoreCase( json.getString( "address") ), "Password is incorrect");

			return Util.right( pk, 64); // some private keys (prod only) were encrypted with 0x at the beginning, so shave it off
		}
		catch( javax.crypto.BadPaddingException e) {  // this happens when we use the wrong password
			throw new Exception( "Private key encryption password is incorrect");
		}
	}

	/** returns
	"Name", name,
	"Address", json.getString( "address"),
	"Key", json.toString() */
	public static JsonObject getEncryptedPrivateKey(String pw) throws Exception {
		return getEncryptedPrivateKey( pw, createPrivateKey());
	}

	public static JsonObject getEncryptedPrivateKey(String pw, String privateKey) throws Exception {
		String address = Credentials.create( privateKey ).getAddress();
		String salt = Util.uid( 8);
		SecretKey secretKey = Encrypt.getKeyFromPassword(pw, salt);
		IvParameterSpec iv = Encrypt.generateIv();
		String ivstr = Encrypt.encode( iv.getIV() );  // this is data added to the input to make it unique
		String encrypted = Encrypt.encrypt( privateKey, secretKey, iv);
		
		return Util.toJson( 
				"data", encrypted,
				"address", address,
				"salt", salt,
				"ivstr", ivstr);
	}


	public static String input(Scanner scanner, String string) {
		S.out( string);
		return scanner.nextLine();
	}

	/** same as Encrypt.bytesToHex()? */
	public static String createPrivateKey() {
		// Generate a 32-bytes private key
		byte[] bytes = new byte[32];
		rnd.nextBytes(bytes);

		// Convert the byte array to a hexadecimal string
		String key = new BigInteger(1, bytes).toString(16);

		// Ensure the hexadecimal string is 64 characters long (pad with leading zeros if necessary)
		while (key.length() < 64) {
			key = "0" + key;
		}
		
		return key;
	}
}
