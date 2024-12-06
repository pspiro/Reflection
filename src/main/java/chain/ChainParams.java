package chain;

import org.json.simple.JsonObject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import common.Util;
import http.MyClient;
import tw.util.IStream;
import tw.util.S;
import web3.CreateKey;
import web3.NodeInstance;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChainParams( // set new params to optional if needed; remember heather's monitor is fixed
		String admin1Addr,
		String admin1RefblocksKey,
		boolean autoFill,
		String alchemyChain,
		String blockchainExpl,
		String busdAddr,
		int busdDecimals,
		String busdName,
		int chainId,
		double faucetAmt,
		int hookServerPort,
		String moralisPlatform,
		String name,
		NodeInstance node,
		String ownerAddr,
		String ownerRefblocksKey,
		String platformBase,  // aka native token name
		String pwName,
		String pwUrl,
		String refWalletAddr,
		String refWalletRefblocksKey,
		int rpcMaxBatchSize,
		String rpcUrl,
		String rusdAddr,
		int rusdDecimals,
		String symbolsTab,
		String web3type,
		double maxAutoRedeem,
		boolean reportTrades,
		double autoReward,
		
		// hook server
		String hookNameSuffix,
		String hookServerUrlBase,
		String hookType,
		double minTokenPosition,
		int myWalletRefresh,
		boolean noStreams
		)
{
		
	NodeInstance createNode() throws Exception {
		return new NodeInstance( rpcUrl, chainId, rpcMaxBatchSize);
	}
	
	public String admin1Key() throws Exception {
		return getKey( admin1RefblocksKey);
	}

	public String refWalletKey() throws Exception {
		return getKey( refWalletRefblocksKey);
	}

	public String ownerKey() throws Exception {
		return getKey( ownerRefblocksKey);
	}

	private String getKey( String key) throws Exception {
		return JsonObject.isObject( key)
				? CreateKey.decryptFromJson( 
						fetchPw(pwUrl).trim(), 
						JsonObject.parse( key) 
						)
				: key;
	}
	
	private String fetchPw(String pwUrl) throws Exception {
		if (!isProduction() ) {
			try {
				String str = IStream.readLine("name.txt");
				if (str.length() > 0) return str;
			} catch (Exception e) {
			}
		}

		// get refblocks pw from pwserver
		var json = Util.toJson( 
				"code", "lwjkefdj827",
				"name", this.pwName);
		
		var ret = MyClient.postToJson( pwUrl + "/getpw", json.toString() );
		String error = ret.getString( "error");
		Util.require( S.isNull( error), "pw server returned error- " + error);
		
		String pw = ret.getString( "pw");
		Util.require( S.isNotNull( pw), "null pw from pw server");
		
		return pw;
	}

	public boolean isProduction() {
		return isPolygon() || isPulseChain() || isZksync();
	}

	public boolean isPulseChain() {
		return chainId == Chains.PulseChain;
	}

	public boolean isPolygon() {
		return chainId == Chains.Polygon;
	}

	public boolean isZksync() {
		return chainId == Chains.ZkSync;
	}
	
	/** e.g. http://localhost:8080/hook/polygon
	 *  for use by RefAPI to reach HookServer directly on local machine */
	public String localHook() {
		return String.format( "http://localhost:%s%s", 
				hookServerPort(),
				hookServerSuffix() );
	}
	
	/** begins with /, e.g. '/hook/polygon' */
	public String hookServerSuffix() {
		return "/hook/" + hookNameSuffix;
	}
	
	/** e.g. https://reflection.trading/hook/polygon */
	public String getHookServerUrl() throws Exception {
		String base = hookServerUrlBase;
		
		if (hookServerUrlBase.equals( "ngrok")) {
			base = Util.getNgrokUrl();
		}
		
		return base + hookServerSuffix();
	}

	/** e.g. https://reflection.trading/hook/polygon/webhook */
	public String getWebhookUrl() throws Exception {
		return getHookServerUrl() + "/webhook";
	}
}
