package testcase;

import tw.util.S;

public class TestTdx extends MyTestCase {
	public void test() throws Exception {
		cli().get( "/tdxrefl/get-stocks");
		var list = cli.readJsonArray();
		list.display();
		assertTrue( list.size() > 0);
		
		var item = list.get( 0);
		assertTrue( S.isNotNull( item.getString( "symbol") ) );
		assertTrue( S.isNotNull( item.getString( "conid") ) );
		assertTrue( S.isNotNull( item.getString( "bid") ) );
		assertTrue( S.isNotNull( item.getString( "ask") ) );
	}

}
