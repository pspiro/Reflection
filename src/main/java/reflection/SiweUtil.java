package reflection;

import org.json.simple.JsonObject;

import com.moonstoneid.siwe.SiweMessage;

public class SiweUtil {

	public static JsonObject toJsonObject(SiweMessage siweMsg) {
		JsonObject msg = new JsonObject();
		msg.put( "domain", siweMsg.getDomain() );
		msg.put( "address", siweMsg.getAddress() );
		msg.put( "uri", siweMsg.getUri() );
		msg.put( "version", siweMsg.getVersion() );
		msg.put( "chainId", siweMsg.getChainId() );
		msg.put( "nonce", siweMsg.getNonce() );
		msg.put( "issuedAt", siweMsg.getIssuedAt() );
		msg.put( "statement", siweMsg.getStatement() );
		return msg;
	}

	public static SiweMessage toSiweMessage(JsonObject obj) throws Exception {
		return new SiweMessage.Builder(
				obj.getString("domain"), 
				obj.getString("address"), 
				obj.getString("uri"),
				obj.getString("version"), 
				obj.getInt("chainId"), 
				obj.getString("nonce"), 
				obj.getString("issuedAt")
				)
				.statement(obj.getString("statement"))
				.build();
	}
}
