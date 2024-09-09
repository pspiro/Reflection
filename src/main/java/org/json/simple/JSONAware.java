package org.json.simple;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Beans that support customized output of JSON text shall implement this interface.  
 * @author FangYidong<fangyidong@yahoo.com.cn>
 */
public interface JSONAware {
	/**
	 * @return JSON text
	 */
	String toJSONString();
	
	/** use this for browser and JEditorPane */
	String toHtml( boolean fancy);
	
	/** use this one for tooltips which can't handle the css */
	default String toHtml() {
		return toHtml( false);
	}

	static final String fancyTable = """
		    table {
		        border-collapse: collapse;
		        width: 100%;
		    }
		    table, th, td {
		        border: 1px solid black;
		    }
		    th, td {
		        padding: 6px;
		        text-align: left;
		    }""";
	
	static JSONAware parse( String text) throws ParseException {
		return (JSONAware)new JSONParser().parse( text);
	}
}
