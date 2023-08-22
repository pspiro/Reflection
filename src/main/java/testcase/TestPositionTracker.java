package testcase;

import reflection.PositionTracker;

public class TestPositionTracker extends MyTestCase {
	int conid = 8314;
	
	public void test() {
		assertEquals( 2, PositionTracker.roundUp( 1.9) ); 
		assertEquals( 2, PositionTracker.roundUp( 2) ); 
		assertEquals( 2, PositionTracker.roundUp( 2.00000001) ); 
		assertEquals( 3, PositionTracker.roundUp( 2.1) ); 

		assertEquals( 2, PositionTracker.roundDown( 2) );
		assertEquals( 2, PositionTracker.roundDown( 1.999999999) );
		assertEquals( 2, PositionTracker.roundDown( 2.1) );
		assertEquals( 1, PositionTracker.roundDown( 1.9) );

		// test buy
		PositionTracker t1 = new PositionTracker();
		assertEquals( 0, t1.buyOrSell(conid, true, .4) );
		assertEquals( 1, t1.buyOrSell(conid, true, .2) );
		assertEquals( 0, t1.buyOrSell(conid, true, .1) );

		// test sell
		PositionTracker t2 = new PositionTracker();
		assertEquals( 3, t2.buyOrSell(conid, false, 3.4) );
		assertEquals( 3, t2.buyOrSell(conid, false, 2.2) );
		assertEquals( 4, t2.buyOrSell(conid, false, 4.9) );
		assertEquals( 3, t2.buyOrSell(conid, false, 2.2) );

		// test undo buy - undo is called when an order was not filled
		PositionTracker t3 = new PositionTracker();
		t3.buyOrSell(conid, true, .3);
		assertEquals( 0, t3.buyOrSell(conid, true, .1) );
		t3.undo(conid, true, .1, 0);
		assertEquals( 0, t3.buyOrSell(conid, true, .1) );
		t3.undo(conid, true, .1, 0);
		assertEquals( 1, t3.buyOrSell(conid, true, .2) );
		t3.undo(conid, true, .2, 1);
		assertEquals( 1, t3.buyOrSell(conid, true, .2) );
		
		// test undo sell
		PositionTracker t4 = new PositionTracker();
		t3.buyOrSell(conid, true, 1); // buy
		t4.buyOrSell(conid, false, .35);
		assertEquals( 0, t4.buyOrSell(conid, false, .1) );
		t4.undo(conid, false, .1, 0);
		assertEquals( 0, t4.buyOrSell(conid, false, .1) );
		t4.undo(conid, false, .1, 0);
		assertEquals( 1, t4.buyOrSell(conid, false, .2) );
		t4.undo(conid, false, .2, 1);
		assertEquals( 1, t4.buyOrSell(conid, false, .2) );
		
//		S.out( t.buy( 8314, .5) );
//		S.out( t.sell( 8314, .5) );
//		S.out( t.sell( 8314, .5) );
//		S.out( t.sell( 8314, .001) );
//		S.out( t.buy( 8314, 2.25) );
//		S.out( t.buy( 8314, 2.25) );
//		S.out( t.buy( 8314, 2.25) );
//		S.out( t.buy( 8314, 3.1) );
//		S.out( t.buy( 8314, 3.1) );
//		S.out( t.buy( 8314, 3.1) );
//		S.out( t.buy( 8314, 3.1) );
//		S.out( t.buy( 8314, 3.1) );
		
	}
}
