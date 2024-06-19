package monitor;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;

import javax.swing.JTextArea;
import javax.swing.JTextField;

import common.Util;
import monitor.AnyQueryPanel.MyComboBox;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.util.HtmlButton;
import tw.util.VerticalPanel;

public class EmailPanel extends MonPanel {
	JTextArea text = new JTextArea(20, 500);
	JTextArea recips = new JTextArea(20, 300);
	JTextField subject = new JTextField( 50);
	MyComboBox selector = new MyComboBox();
	GTable tab;
	
	EmailPanel() {
		super( new BorderLayout() );
	
		VerticalPanel pan = new VerticalPanel();
		pan.add( "Select email", selector);
//		pan.add( "Email text", new JScrollPane( text) );
//		pan.add( "Recipients", new JScrollPane( recips) );
		panladd( "Email subject", subject);
		pan.add( "Email text", text);
		pan.add( "Recipients", recips);
		panladd( "Send", new HtmlButton( "Send", ev -> onSend() ) );
		
		add( pan);
		
		selector.addActionListener( event -> Util.wrap( () -> onSelected() ) );
	}
	
	private void onSend() {
		String sub = ;
		
		ArrayList<String> ar = toArray( recips.getText() );
		Monitor.m_config.sendEmail("peteraspiro@gmail.com", subject.getText(), 
		
		
		
		private void sendTestEmail() {
			// TODO Auto-generated method stub
			
		}
		
		Util.confirm( this, "Send this text to %s recipients", )
	}
	
	static ArrayList<String> toArray( String text) throws Exception {
		ArrayList<String> ar = new ArrayList<>();
		BufferedReader reader = new BufferedReader( new StringReader( text) );
		String line = reader.readLine();
		while (line != null) {
			ar.add( line);
			line = reader.readLine();
		}
		return ar;
	}
	

	private void onSelected() throws Exception {
		String name = selector.getSelectedItem().toString();
		text.setText( tab.get( name) );
	}

	@Override protected void refresh() throws Exception {
		tab = new GTable( NewSheet.Prefinery, "Templates", "Name", "Text");
		selector.set( tab.keySet().toArray() );
	}

}
