package test;

import org.junit.runner.JUnitCore;

import junit.framework.TestCase;

public class TestAll extends TestCase {
	
	public static void main(String[] args) {
		JUnitCore.runClasses(TestWhatIf.class);
//		JUnitCore.runClasses(TestOrder.class);
//		JUnitCore.runClasses(TestErrors.class);
//		JUnitCore.runClasses(TestPrices.class);
//		JUnitCore.runClasses(TestOutsideHours.class);
	}
}
