package monitor;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import common.Util;
import monitor.AnyQueryPanel.MyComboBox;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.util.HtmlButton;
import tw.util.VerticalPanel;

public class EmailPanel extends MonPanel {
	final static String testEmail = "peteraspiro@gmail.com heather@reflection.trading";

	private JTextField testRecipients = new JTextField( testEmail);
	private JTextField subject = new JTextField( 50);
	private MyComboBox selector = new MyComboBox();
	private JTextArea text = new JTextArea(20, 500);
	private JTextArea recips = new JTextArea(20, 300);
	private GTable tab;

	EmailPanel() {
		super( new BorderLayout() );

		VerticalPanel pan = new VerticalPanel();
		pan.add( "Test recipients", testRecipients);
		pan.add( Box.createVerticalStrut(20));
		pan.add( "Email subject", subject);
		pan.add( Box.createVerticalStrut(20));
		pan.add( "Select email", selector);
		pan.add( "Email text", text);
		pan.add( Box.createVerticalStrut(20));
		pan.add( "Recipients (one per line)", recips);
		pan.add( "Send", new HtmlButton( "Send", ev -> Util.wrap( () -> onSend() ) ) );

		add( pan);

		selector.addActionListener( event -> Util.wrap( () -> onSelected() ) );
	}

	private void onSend() throws Exception {
		String emailText = text.getText().replaceAll( "\n", " <br>\n");

		// send test email
		Monitor.m_config.sendEmail( testRecipients.getText(), "[test] " + subject.getText(), emailText);

		// confirm and send all emails
		ArrayList<String> list = toArray( recips.getText() );
		if (Util.confirm( this, "A test email was sent to %s;\n"
				+ "please review.\n\n"
				+ "Send now to %s recipients?", testEmail, list.size() ) ) {
			for (String recip : list) {
				Monitor.m_config.sendEmail(
						recip.indexOf( '<') != -1 ? Util.parseEmailOnly( recip) : recip,  // our current email send function does not support display names in the "to" field, i.e. peter <pet@gmail.com> is not supported
						subject.getText(),
						text.getText()
						);
			}
		}
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
