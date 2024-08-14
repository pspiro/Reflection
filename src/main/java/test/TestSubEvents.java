package test;

import java.net.ConnectException;
import java.util.Arrays;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.filters.LogFilter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketService;

import io.reactivex.disposables.Disposable;
import tw.util.S;

public class TestSubEvents {
	public static void main(String[] args) throws Exception {
		observe( "0x2703161D6DD37301CEd98ff717795E14427a462B");
	}

	static void observe( String contractAddr) throws Exception {
		// Connect to the Ethereum node
		String pulse = "https://rpc.pulsechain.com";
		String poly = "https://polygon-rpc.com/";
		String poly2 = "https://polygon.llamarpc.com";
		
//		var service = new HttpService(pulse);
			
		// Connect to Ethereum node via WebSocket
		var service = new WebSocketService("wss://rpc.pulsechain.com", true);
		service.connect();

		Web3j web3j = Web3j.build( service); // or try wsService 

		// The event signature for Transfer(address,address,uint256)
		String transferEventSignature = "0xddf252ad"; // 1be2c89b69c2b068fc378daa952ba7f163c4a11628f7e4d8a7f77e1a";

		var filter = new EthFilter(
				DefaultBlockParameterName.LATEST, 
				DefaultBlockParameterName.LATEST, 
				contractAddr);

		// Subscribe to Transfer events
		S.out( "subscribing");
		
		Disposable subscription = web3j.ethLogFlowable(filter)
				//.filter(log -> true) //log.getTopics().get(0).equals(transferEventSignature))
				.subscribe(
					log -> {
						System.out.println("Transfer event detected: ");
						System.out.println("From: " + "0x" + log.getTopics().get(1).substring(26));
						System.out.println("To: " + "0x" + log.getTopics().get(2).substring(26));
						System.out.println("Value: " + new java.math.BigInteger(log.getData().substring(2), 16));
					},
					throwable -> {
						System.err.println("Error: " + throwable.getMessage());
					}
				);

		S.out( "waiting");
		// Keep the subscription alive (for example, you might keep the application running)
		Thread.sleep(1000000);
		S.out( "disposing");
		subscription.dispose();
	}
	
}