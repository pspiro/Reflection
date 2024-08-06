package org.json.simple;

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
}
