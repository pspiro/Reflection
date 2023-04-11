package reflection;

import org.json.simple.JSONObject;

import com.moonstoneid.siwe.SiweMessage;

import json.MyJsonObject;

public class SiweUtil {

	public static JSONObject toJsonObject(SiweMessage siweMsg) {
		JSONObject msg = new JSONObject();
		msg.put( "domain", siweMsg.getDomain() );
		msg.put( "address", siweMsg.getAddress() );
		msg.put( "URI", siweMsg.getUri() );
		msg.put( "version", siweMsg.getVersion() );
		msg.put( "chainId", siweMsg.getChainId() );
		msg.put( "nonce", siweMsg.getNonce() );
		msg.put( "issuedAt", siweMsg.getIssuedAt() );
		msg.put( "statement", siweMsg.getStatement() );
		return msg;
	}

	public static SiweMessage toSiweMessage(MyJsonObject obj) throws Exception {
		return new SiweMessage.Builder(
				obj.getString("domain"), 
				obj.getString("address"), 
				obj.getString("URI"), 
				obj.getString("version"), 
				obj.getInt("chainId"), 
				obj.getString("nonce"), 
				obj.getString("issuedAt")
				)
				.statement(obj.getString("statement"))
				.build();
	}

}
