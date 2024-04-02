package reflection;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;

/** Values are returned lower case, interned, although the intern() didn't seem to work. */
public class ParamMap {
	private JsonObject m_obj;

	public ParamMap(JsonObject obj) {
		m_obj = obj;
	}
	
	public ParamMap() {
		m_obj = new JsonObject();
	}

	/** Used for logging */
	JsonObject obj() {
		return m_obj;
	}
	
	/** Returns lower case, interned string. */
	String getParam(String tag) {
		return m_obj.getString(tag).toLowerCase().intern();  // check all uses and get rid of intern(), it's not efficient
	}
	
	/** Returns lower case, interned string. */
	String getRequiredParam(String tag) throws RefException {
		String val = getParam( tag);
		Main.require( S.isNotNull( val), RefCode.INVALID_REQUEST, "Param '%s' is missing", tag);
		return val;
	}

	/** Returns lower case, interned string. */
	String getWalletAddress(String tag) throws RefException {
		String val = getParam( tag);
		Main.require( S.isNotNull( val), RefCode.INVALID_REQUEST, "Param '%s' is missing", tag);
		Main.require( Util.isValidAddress(val), RefCode.INVALID_REQUEST, "Wallet address is invalid");
		return val;
	}

	String getRequiredString(String tag) throws RefException {
		String val = m_obj.getString( tag);
		Main.require( S.isNotNull( val), RefCode.INVALID_REQUEST, "Param '%s' is missing", tag);
		return val;
	}

	/** Never null */
	public String getString(String tag) {
		return m_obj.getString(tag);
	}

	boolean getBool(String tag) {
		return m_obj.getBool(tag);
	}
	
	int getRequiredInt(String tag) throws RefException {
		try {
			return Integer.valueOf( getRequiredString( tag) );
		}
		catch( NumberFormatException e) {
			throw new RefException( RefCode.INVALID_REQUEST, "Param '%s' must be an integer", tag);
		}
	}

	int getInt(String tag) throws RefException {
		try {
			return Integer.valueOf( getString( tag) );
		}
		catch( NumberFormatException e) {
			return 0;
		}
	}

	double getRequiredDouble(String tag) throws RefException {
		try {
			return Double.valueOf( getRequiredString( tag) );
		}
		catch( NumberFormatException e) {
			throw new RefException( RefCode.INVALID_REQUEST, "Param '%s' must be a number", tag);
		}
	}		

	double getDoubleParam(String tag) throws RefException {
		try {
			String val = m_obj.getString(tag);
			return S.isNull(val) ? 0 : Double.valueOf(val);
		}
		catch( NumberFormatException e) {
			throw new RefException( RefCode.INVALID_REQUEST, "Param '%s' must be a number", tag);
		}
	}		

	/** Case-insensitive */
	<T extends Enum<T>> T getEnumParam(String tag, T[] values) throws RefException {
		try {
			return Util.getEnum(getRequiredString(tag), values);
		}
		catch( IllegalArgumentException e) {			
			throw new RefException( RefCode.INVALID_REQUEST, "Param '%s' has an invalid value", 
					tag, getRequiredParam(tag), Util.allEnumValues( values) );
		}
	}

	public void put(String tag, String val) {
		m_obj.put(tag, val);
	}

	/** For backwards compatibility */
	public String get(String tag) {
		return (String)m_obj.get(tag);
	}

	@Override public String toString() {
		return m_obj.toString();
	}
	
	public String getUnescapedString(String tag) {
		return Util.unescHtml( getString( tag) );
	}
}
