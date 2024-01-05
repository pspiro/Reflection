package test;

import http.MyClient;

public class TestKyc {
	static public void main( String[] args) throws Exception {
//		Config c = Config.ask();
		
String persona =
"""
{
"kyc_status":"VERIFIED",
"address":null,
"cookie":"__Host_authToken0x2703161D6DD37301CEd98ff717795E14427a462B137=%7B%22message%22%3A%7B%22address%22%3A%220x2703161D6DD37301CEd98ff717795E14427a462B%22%2C%22chainId%22%3A137%2C%22domain%22%3A%22reflection.trading%22%2C%22statement%22%3A%22Sign%20in%20with%20Ethereum.%22%2C%22issuedAt%22%3A%222023-12-20T22%3A40%3A34.493Z%22%2C%22uri%22%3A%22https%3A%2F%2Freflection.trading%22%2C%22version%22%3A%221%22%2C%22nonce%22%3A%225ODc3nk4DOr3fwXiqyUz%22%7D%7D",
"phone":null,
"active":true,
"persona_response":"{\\"fields\\":{\\"address-city\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"address-country-code\\":{\\"type\\":\\"string\\",\\"value\\":\\"IN\\"},\\"address-postal-code\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"address-street-1\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"address-street-2\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"address-subdivision\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"birthdate\\":{\\"type\\":\\"date\\",\\"value\\":\\"1993-03-31\\"},\\"current-government-id\\":{\\"type\\":\\"government_id\\",\\"value\\":{\\"id\\":\\"doc_1Q9vaBXZ7FyzumutN7g7oAbf\\",\\"type\\":\\"Document::GovernmentId\\"}},\\"current-selfie\\":{\\"type\\":\\"selfie\\",\\"value\\":{\\"id\\":\\"self_6HH4Jb8JBCnKh89QXU9vpKCw\\",\\"type\\":\\"Selfie::ProfileAndCenter\\"}},\\"email-address\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"identification-class\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"identification-number\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"name-first\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"name-last\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"name-middle\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"phone-number\\":{\\"type\\":\\"string\\",\\"value\\":null},\\"selected-country-code\\":{\\"type\\":\\"string\\",\\"value\\":\\"IN\\"},\\"selected-id-class\\":{\\"type\\":\\"string\\",\\"value\\":\\"id\\"}},\\"inquiryId\\":\\"inq_nyovESzuEhKh5zrkokjDHV69\\",\\"status\\":\\"completed\\"}",
"wallet_public_key":"0x2703161d6dd37301ced98ff717795e14427a462b",
"email":null,
"is_black_listed":false
}
""";

		MyClient.postToJson(Cookie.base + "/api/users/register", persona).display();
		

	}
}
