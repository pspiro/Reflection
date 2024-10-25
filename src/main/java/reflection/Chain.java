package reflection;

import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import refblocks.RbBusd;
import refblocks.RbRusd;
import tw.util.IStream;
import tw.util.S;
import web3.Busd;
import web3.CreateKey;
import web3.NodeInstance;
import web3.Rusd;

public record Chain(
		NodeInstance node,
		String name,
		int chainId,
		String blockchainExpl,
		String busdAddr,
		String rusdAddr,
		int hookServerPort,
		String moralisPlatform,
		String alchemyChain,
		String platformBase,
		double faucetAmt,
		int rpcMaxBatchSize,
		String rpcUrl,
		String web3type,
		int busdDecimals,
		String busdName,
		int rusdDecimals,
		String admin1Addr,
		String admin1RefblocksKey,
		String ownerAddr,
		String ownerRefblocksKey,
		String refWalletAddr,
		String refWalletRefblocksKey,
		String pwName,
		String pwUrl
		)
{
	
	
	public Rusd rusd() throws Exception {
		return new Rusd( rusdAddr, rusdDecimals, this, 
				new RbRusd( rusdAddr, rusdDecimals) );
	}
	
	public NodeInstance createNode() throws Exception {
		return new NodeInstance( rpcUrl, chainId, rpcMaxBatchSize);
	}

	public Busd busd() throws Exception {
		return new Busd( busdAddr, busdDecimals, busdName, 
				new RbBusd( busdAddr, busdDecimals, busdName) );
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
		return chainId == 369;
	}

	public boolean isPolygon() {
		return chainId == 137;
	}

	public boolean isZksync() {
		return chainId == 324;
	}

	static class ChainWrapper {
		Chain chain;
		NodeInstance node;

		ChainWrapper( Chain chainIn) throws Exception {
			chain = chainIn;
			node = chain.createNode();
		}

		public Chain chain() {
			return chain;
		}

		public NodeInstance node() {
			return node;
		}
	}
	
}
