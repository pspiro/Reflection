package reflection;

import java.util.HashMap;

import json.MyJsonObject;
import reflection.OldStyleTransaction.MsgType;
import tw.util.S;

/** Values are returned lower case, interned, although the intern() didn't seem to work. */
public class ParamMap extends HashMap<String, String> {
	
	/** Returns lower case, interned string. */
	String getParam(String tag) {
		String value = super.get( tag);
		return value != null ? value.toLowerCase().intern() : null;
	}
	
	public String getRequiredParam(String tag) throws RefException {
		String val = getParam( tag);
		Main.require( S.isNotNull( val), RefCode.INVALID_REQUEST, "Param '%s' is missing", tag);
		return val;
	}

	boolean getBool(String tag) throws RefException {
		return Boolean.valueOf( getParam( tag) );
	}
	
	public int getRequiredInt(String tag) throws RefException {
		try {
			return Integer.valueOf( getRequiredParam( tag) );
		}
		catch( NumberFormatException e) {
			throw new RefException( RefCode.INVALID_REQUEST, "Param '%s' must be an integer", tag);
		}
	}

	double getRequiredDouble(String tag) throws RefException {
		try {
			return Double.valueOf( getRequiredParam( tag) );
		}
		catch( NumberFormatException e) {
			throw new RefException( RefCode.INVALID_REQUEST, "Param '%s' must be a number", tag);
		}
	}		

	double getDouble(String tag) throws RefException {
		try {
			String val = getParam(tag);
			return S.isNull(val) ? 0 : Double.valueOf(val);
		}
		catch( NumberFormatException e) {
			throw new RefException( RefCode.INVALID_REQUEST, "Param '%s' must be a number", tag);
		}
	}		

	<T extends Enum<T>> T getEnumParam(String tag, T[] values) throws RefException {
		try {
			return S.getEnum( getRequiredParam( tag), values);
		}
		catch( IllegalArgumentException e) {			
			throw new RefException( RefCode.INVALID_REQUEST, "Param '%s' has invalid value; valid values are %s", tag, MsgType.allValues() );
		}
	}

	public String getLowerCase(String tag) {
		String str = get(tag);
		return str == null ? null : str.toLowerCase();
	}
	
	public MyJsonObject getJsonObject(String tag) throws Exception {
		String str = get(tag);
		return str != null ? MyJsonObject.parse(str) : null;
	}
}
