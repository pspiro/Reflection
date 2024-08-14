package tw.util;

import java.awt.Color;

import javax.swing.JEditorPane;

public class HtmlPane extends JEditorPane {
	public HtmlPane() {
		setContentType("text/html");
		setEditable(false);
		setBackground( new Color(238,238,238));
	}
}
