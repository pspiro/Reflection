/*
 * $Id: JSONObject.java,v 1.1 2006/04/15 14:10:48 platform Exp $
 * Created on 2006-4-10
 */
package org.json.simple;

import com.moonstoneid.siwe.SiweMessage;

import common.Util;
import reflection.SiweUtil;

/**
 * A JSON object. Key value pairs are unordered. JSONObject supports java.util.Map interface.
 * 
 * Note that null values are supported by put(); use putIf() to avoid null values
 * 
 * @author FangYidong<fangyidong@yahoo.com.cn>
 */
public class JsonObject extends TsonObject<Object> {
	
	private static final long serialVersionUID = -503443796854799292L;
	
	
	public JsonObject() {
		super();
	}
	
	/** A table that maps string to JsonObject or subclass of JsonObject */ 
	static class JsonTable<V extends JsonObject> extends TsonObject<V> {

		public JsonArray toArray() {
			JsonArray ar = new JsonArray();
			for (var obj : values() ) {
				ar.add( obj);
			}
			return ar;
		}
	}

	public SiweMessage getSiweMessage() throws Exception {
		return SiweUtil.toSiweMessage(this);
	}
	/** for testing only */
	public void display2() {
		put( "type", getClass().getName() );
		display( this, 0, false);
		System.out.println();
	}

	/** Can return null; caller should check */
	public JsonObject getObject(String key) throws Exception {
		Object obj = get(key);
		Util.require( obj == null || obj instanceof TsonObject, "Not a json object  key=%s  val=%s", key, obj);
		return (JsonObject)obj;
	}

	/** Throws exception if not found */
	public JsonObject getRequiredObj(String key) throws Exception {
		JsonObject obj = getObject(key);
		Util.require( obj instanceof TsonObject, "Not a json object  key=%s  val=%s", key, obj);
		return obj;
	}
	
	/** Never returns null, returns empty TsonObject  */
	public JsonObject getObjectNN(String key) throws Exception {
		JsonObject obj = getObject(key);
		return obj != null ? obj : new JsonObject();
	}
	
}
