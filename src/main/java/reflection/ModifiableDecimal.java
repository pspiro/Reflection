package reflection;

import tw.util.S;

public class ModifiableDecimal {
	private double value = 0;

	@Override public String toString() {
		return S.fmt3(value);
	}

	public double value() {
		return this.value;
	}

	boolean isZero() {
		return value == 0;
	}

	boolean nonZero() {
		return value != 0;
	}

	public void value(double val) {
		value = val;
	}
	
	public void inc( double val) {
		value += val;
	}
}
