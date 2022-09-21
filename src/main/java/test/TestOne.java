package test;

import static test.TestErrors.sendData;

import java.util.Date;
import java.util.HashMap;

import junit.framework.TestCase;
import reflection.RefException;
import reflection.Util;

public class TestOne extends TestCase {
	@SuppressWarnings("deprecation")
	public void testExchHours() throws RefException {
		String str = "20220807:CLOSED;"
				+ "20220916:0900-20220916:1600;"
				+ "20220918:0930-20220918:1600";
		
		assertTrue(  Util.inside( date(2022, 9, 16,  9, 00), 8314, str) );
		assertTrue(  Util.inside( date(2022, 9, 16, 12, 00), 8314, str) );
		assertFalse( Util.inside( date(2022, 9, 15, 12, 00), 8314, str) );
		assertFalse( Util.inside( date(2022, 9, 17, 12, 00), 8314, str) );
		assertFalse( Util.inside( date(2022, 9, 16,  8, 59), 8314, str) );
		assertFalse( Util.inside( date(2022, 9, 16, 16, 00), 8314, str) );
		assertFalse( Util.inside( date(2022, 9, 16, 16, 10), 8314, str) );
	}

	private Date date(int year, int month, int day, int hr, int min) {
		return new Date( year - 1900, month - 1, day, hr, min);
	}
	
	
	
} 	
