package reflection;

import com.moonstoneid.siwe.error.SiweException;

import http.MyHttpClient;
import tw.util.S;

public class TestSiwe {
	public static void main(String[] args) throws Exception {
		try {
//			String message: "{'address':'0xb95bf9C71e030FA3D8c0940456972885DB60843F','chainId':5,'domain':'localhost','statement':'Sign in with Ethereum.','issuedAt':'2023-04-10T16:08:25.624Z','uri':'http:\/\/localhost','version':'1','nonce':'C0Uxu0FiEUjGfODWSNVH'}";
			String data =      "{'signature':'0xc8c73481ad0a0670d9d08dd490071502cb72e9c7dd9f82577764a9adc9c085fc2325166fbae9eb035dd167ae8e96900ec648d7696c1c8f3986d8696b1a9096951b','message':{'domain':'localhost','address':'0xb95bf9C71e030FA3D8c0940456972885DB60843F','statement':'Sign in with Ethereum.','uri':'http://localhost','version':'1','chainId':5,'nonce':'C0Uxu0FiEUjGfODWSNVH','issuedAt':'2023-04-10T16:08:25.624Z'}}";
			String signedMsg = "{ 'signature':'0xb704d00b0bd15e789e26e566d668ee03cca287218bd6110e01334f40a38d9a8377eece1d958fff7a72a5b669185729a18c1a253fd0ddcf9711764a761d60ba821b', 'message':{'domain':'usedapp-docs.netlify.app', 'address':'0xb95bf9C71e030FA3D8c0940456972885DB60843F', 'statement':'Sign in with Ethereum.', 'uri':'https://usedapp-docs.netlify.app', 'version':'1', 'chainId':5, 'nonce':'s6BSC0iXede6QSw5D', 'issuedAt':'2023-04-10T14:40:03.878Z'} }";
			
			
			MyHttpClient cli = new MyHttpClient("localhost", 8383);
			cli.post("/siwe/signin", Util.toJson( data) );
			S.out( cli.readString() );
			
//		    String nonce = Utils.generateNonce();
		} catch (SiweException e) {
		    // Handle exception
		}
	}
}
