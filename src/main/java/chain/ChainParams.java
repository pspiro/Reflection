package chain;

import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.IStream;
import tw.util.S;
import web3.CreateKey;
import web3.NodeInstance;

public record ChainParams(
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
		String platformBase,
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
	
	public String getWebhookUrl() throws Exception {
		String base = hookServerUrlBase;
		
		if (hookServerUrlBase.equals( "ngrok")) {
			base = Util.getNgrokUrl();
		}
		
		return String.format( "%s/%s", base, hookNameSuffix);
	}
	
}
