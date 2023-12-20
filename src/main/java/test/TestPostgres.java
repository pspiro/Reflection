package test;

import org.json.simple.JsonObject;

import reflection.Config;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		Config config = Config.ask();
		config.useExteranDbUrl();
//		config.readStocks().getStock("AAPL").getToken().showAllTransactions();
		
		String text = 
"""
{"kyc_status":"VERIFIED","address":null,"cookie":"__Host_authToken0x96531A61313FB1bEF87833F38A9b2Ebaa6EA57ce137=%7B%22message%22%3A%7B%22address%22%3A%220x96531A61313FB1bEF87833F38A9b2Ebaa6EA57ce%22%2C%22chainId%22%3A137%2C%22domain%22%3A%22reflection.trading%22%2C%22statement%22%3A%22Sign%20in%20with%20Ethereum.%22%2C%22issuedAt%22%3A%222023-12-20T18%3A23%3A20.068Z%22%2C%22uri%22%3A%22https%3A%2F%2Freflection.trading%22%2C%22version%22%3A%221%22%2C%22nonce%22%3A%22MuMTvRCb82SjROLuJ6Yh%22%7D%7D","phone":null,"active":true,"persona_response":"{\\"fields\\":{\\"address-city\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"address-country-code\\":{\\"type\\":\\"string\\",\\"value\\":\\"IN\\"},\\"address-postal-code\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"address-street-1\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"address-street-2\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"address-subdivision\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"birthdate\\":{\\"type\\":\\"date\\",\\"value\\":\\"1993-03-31\\"},\\"current-government-id\\":{\\"type\\":\\"government_id\\",\\"value\\":{\\"id\\":\\"doc_1Q9vaBXZ7FyzumutN7g7oAbf\\",\\"type\\":\\"Document::GovernmentId\\"}},\\"current-selfie\\":{\\"type\\":\\"selfie\\",\\"value\\":{\\"id\\":\\"self_6HH4Jb8JBCnKh89QXU9vpKCw\\",\\"type\\":\\"Selfie::ProfileAndCenter\\"}},\\"email-address\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"identification-class\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"identification-number\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"name-first\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"name-last\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"name-middle\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"phone-number\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"selected-country-code\\":{\\"type\\":\\"string\\",\\"value\\":\\"IN\\"},\\"selected-id-class\\":{\\"type\\":\\"string\\",\\"value\\":\\"id\\"}},\\"inquiryId\\":\\"inq_nyovESzuEhKh5zrkokjDHV69\\",\\"status\\":\\"completed\\"}","wallet_public_key":"0x96531A61313FB1bEF87833F38A9b2Ebaa6EA57ce","email":null,"is_black_listed":false}
""";
		
		JsonObject obj = JsonObject.parse(text);
		
		String m_walletAddr = "0xb016711702d3302cef6ceb62419abbef5c44450e";

		JsonObject user = new JsonObject();
		user.put( "wallet_public_key", m_walletAddr.toLowerCase() );
		user.copyFrom( obj, "kyc_status", "persona_response");

		config.sqlCommand( sql -> 
			sql.insertOrUpdate("users", user, "wallet_public_key = '%s'", m_walletAddr.toLowerCase() ) );
		
	}
	
	
}
