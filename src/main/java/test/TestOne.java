package test;

import java.util.Date;

import junit.framework.TestCase;
import reflection.RefException;
import reflection.Util;

public class TestOne extends TestCase {
	public void testExchHours() throws RefException {
		String str = "20220807:CLOSED;"
				+ "20220916:0900-20220916:1600;"
				+ "20220918:0930-20220918:1600";
		
		assertFalse( Util.inside( date(2022, 9, 15, 12, 00), 8314, str) ); // prior day
		assertFalse( Util.inside( date(2022, 9, 16,  8, 59), 8314, str) ); // before open
		assertTrue(  Util.inside( date(2022, 9, 16,  9, 00), 8314, str) ); // at open 
		assertTrue(  Util.inside( date(2022, 9, 16, 15, 59), 8314, str) ); // before close 
		assertFalse( Util.inside( date(2022, 9, 16, 16, 00), 8314, str) ); // at close
		assertFalse( Util.inside( date(2022, 9, 17, 12, 00), 8314, str) ); // next day
	}

	private static Date date(int year, int month, int day, int hour, int min) {
		return new Date( year-1900, month-1, day, hour, min);
	}
}
