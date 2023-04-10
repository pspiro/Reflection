package test;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import com.moonstoneid.siwe.SiweMessage;
import com.moonstoneid.siwe.SiweMessage.Builder;
import com.moonstoneid.siwe.error.SiweException;
import com.moonstoneid.siwe.util.Utils;

import tw.util.S;

public class TestSiwe {
	public static void main(String[] args) {
		String message = "example.com wants you to sign in with your Ethereum account:\n" +
			    "0xAd472fbB6781BbBDfC4Efea378ed428083541748\n\n" +
			    "Sign in to use the app.\n\n" +
			    "URI: https://example.com\n" +
			    "Version: 1\n" +
			    "Chain ID: 1\n" +
			    "Nonce: EnZ3CLrm6ap78uiNE0MU\n" +
			    "Issued At: 2022-06-17T22:29:40.065529400+02:00";

			String signature = "0x2ce1f57908b3d1cfece352a90cec9beab0452829a0bf741d26016d60676d63" +
			        "807b5080b4cc387edbe741203387ef0b8a6e79743f636512cc48c80cbb12ffa8261b";
			try {
			    // Parse string to SiweMessage
			    SiweMessage siwe = new SiweMessage.Parser().parse(message);
			    S.out( siwe.getAddress() );

			    // Verify integrity of SiweMessage by matching its signature
			    siwe.verify("example.com", "EnZ3CLrm6ap78uiNE0MU", signature);
			} catch (SiweException e) {
			    // Handle exception
			}		
	}
	
	public static void main2(String[] args) {
		try {
			String domain = "Reflection.trading";
			String address = "0x6Ee9894c677EFa1c56392e5E7533DE76004C8D94";
			String uri = "https://localhost/login";
			String version = "1";
			int chainId = 5;
		    String nonce = Utils.generateNonce();

		    
		    // Create new SiweMessage
		    SiweMessage siwe = new Builder(
		    		domain, 
		    		address, 
		    		uri, 
		    		version, 
		    		chainId, 
		    		nonce, 
		    		DateTimeFormatter.ISO_INSTANT.format( Instant.now() )
		    	)
		    	.statement("Please sign in")
		    	.build();
		    
		    // Create EIP-4361 string from SiweMessage
		    S.out( "*%s*  %s", siwe.toMessage(), siwe.toMessage().length() );
		    
		    siwe.verify(domain, nonce, "abc");
		} catch (SiweException e) {
			e.printStackTrace();
		}
	}
}

// QUESTION: where/how does the client store the signature? should be in a cookie?
//           how does the Backend remember the nonce?

// you should have a long expiration period, because what's the difference?
// we must give them a way to log-out

/*
This is a UML Sequence diagram

client							server

		request siwe message -->
								create siwe message
								map address -> { nonce, time }
			<-- siwe message
			
	sign message
			signed message -->
								validate signed message
			<-- success or failure
			
			
*/
						