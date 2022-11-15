package test;

import static test.TestErrors.sendData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import junit.framework.TestCase;
import reflection.Config;
import reflection.MySqlConnection;

public class TestConfig extends TestCase {
	
	public void testConnection() throws Exception {
		String data = "{ 'msg': 'getconnectionstatus' }"; 
		HashMap<String, Object> map = sendData( data);
		assertEquals( "true", map.get("mktDataConnectedToTWS") );
		assertEquals( "true", map.get("mktDataConnectedToBroker") );
		assertEquals( "true", map.get("orderConnectedToTWS") );
		assertEquals( "true", map.get("orderConnectedToBroker") );		
	}
	
	public void testConfig() throws Exception {
		String data = "{ 'msg': 'getconfig' }"; 
		HashMap<String, Object> map = sendData( data);
		assertEquals( "paper", map.get( "mode") );
	}

	public void testRefreshConfig() throws Exception {
		String data = "{ 'msg': 'refreshconfig' }"; 
		HashMap<String, Object> map = sendData( data);
		assertEquals( "paper", map.get( "mode") );
	}
	
	public void testBackendConfig() throws Exception {
		String data = "{ 'msg': 'pullbackendconfig' }"; 
		HashMap<String, Object> map = sendData( data);
		assertEquals( "OK", map.get( "code") );
	}
	
	public void testValidDbConfig() throws Exception {
		MySqlConnection con = new MySqlConnection();
		con.connect("jdbc:postgresql://34.86.193.58:5432/reflection", "postgres", "1359");
		             
		ResultSet rs = con.query( "select * from system_configurations");
		
		assertTrue( rs.next() );
		
		double min_order_size = rs.getDouble( "min_order_size");
		double max_order_size = rs.getDouble( "max_order_size");
		double non_kyc_max_order_size = rs.getDouble( "non_kyc_max_order_size");
		double price_refresh_interval = rs.getDouble( "price_refresh_interval");
		double commission = rs.getDouble( "commission");
		double buy_spread = rs.getDouble( "buy_spread");
		double sell_spread = rs.getDouble( "sell_spread");
		
		assertTrue( min_order_size < 100);
		assertTrue( max_order_size >= 100 && max_order_size <= 10000);
		assertTrue( min_order_size < max_order_size);
		assertTrue( non_kyc_max_order_size <= max_order_size);
		assertTrue( price_refresh_interval > 5 && price_refresh_interval < 60);
		assertTrue( commission > 0 && commission < 20);
		assertTrue( buy_spread > .001 && buy_spread < .05);
		assertTrue( sell_spread > .001 && sell_spread < .05);
	}

}
