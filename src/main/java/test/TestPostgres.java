package test;

import common.Util;
import http.MyClient;
import reflection.Config;
import testcase.Cookie;
import web3.NodeServer;



/** Just test that you can connect to the database. */
public class TestPostgres {
	static Config m_config;

	static {
		try {
			m_config = Config.read();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		Cookie.setWalletAddr( NodeServer.prod);

		MyClient.postToJson("http://localhost:5000/api/redemptions/redeem/" + Cookie.wallet, Util.toJson( "cookie", Cookie.cookie).toString() )
		.display();

	}

}
