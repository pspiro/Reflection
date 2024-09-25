package tw.google;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.api.client.util.Base64;
import com.google.api.client.util.StringUtils;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.Gmail.Users.Messages;
import com.google.api.services.gmail.Gmail.Users.Messages.Get;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.ModifyMessageRequest;

import common.Util;
import tw.util.S;

/** Adds functionality to Gmail; use it. */
public class TwMail {
	private static final String userId = "peteraspiro@gmail.com";
	
	private Gmail m_gmail;
	
	public TwMail(Gmail gmail) {
		m_gmail = gmail;
	}

	public static void main(String[] args) {
		try {
			Auth auth = Auth.auth();
			long t1 = System.currentTimeMillis();
			auth.getMail().showLabels();
			//auth.getMail().showEmails();
			long t2 = System.currentTimeMillis() - t1;
			S.out( "" + t2);
			
//			send( auth.getGmail(), 
//					"", 
//					"heather@tradewindsrealestate.net", 
//					"janespiro@gmail.com", 
//					"test from code",
//					"test from code", 
//					"plain");
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}
	
	public void showEmails() {
		ArrayList<MyMsg> myMsgs = getMessages( "label:inbox", 1);

		for (MyMsg myMsg : myMsgs) {
			S.out( myMsg);
		}
	}
	
	private void showLabels() throws Exception {

		com.google.api.services.gmail.Gmail.Users.Labels.List list = m_gmail.users().labels().list(userId);
		List<Label> labels = list.execute().getLabels();
		for (Label label : labels) {
			S.out( label.toPrettyString() );
		}
	}
	
	
	
	/** @param tos is space-separated
	 *  @param type "plain" or "html" */
	public void send(String from, String fromEmail, String tos, String subject, String text, boolean html) throws Exception {
		Util.require( S.isNotNull( from), "'from' address required");
		Util.require( S.isNotNull( from), "'to' address required");
		Util.require( tos.indexOf( '<') == -1, "display name in 'to' field is not supported"); // doesn't work with gmail
		
		S.out( "Sending email");
		S.out( "  from: %s <%s>", from, fromEmail);
		S.out( "  to: %s", tos);
		S.out( "  subject: %s", subject);

		MimeMessage message = createEmail( from, fromEmail, tos, subject);
		message.setText(text, null, html ? "html" : "plain");
		sendEmail( message);
	}

	/** @param from email address or Name (email) in square brackets
	 * @param tos is a space-separated string of email addresses; if it were comma-separated, you could
	 *  put the emails in this format: Name (email) but with square brackets */
	public static MimeMessage createEmail(String from, String fromEmail, String tos, String subject) throws Exception {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);
		
		MimeMessage email = new MimeMessage(session);
		email.setFrom(new InternetAddress(String.format("%s <%s>", from, fromEmail)));
		email.setSubject(subject);
		StringTokenizer st = new StringTokenizer( tos, " ");
		while( st.hasMoreTokens() ) {
			email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(st.nextToken()));
		}
		return email;
	}

	public void sendEmail( MimeMessage email) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		email.writeTo(bytes);
		String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());

		Message message = new Message();
		message.setRaw(encodedEmail);
		message = m_gmail.users().messages().send(userId, message).execute();
		
		S.out("Message id: " + message.getId());
		S.out(message.toPrettyString());
	}
	
	/** @param query format is "label:inbox" */ 
	public ArrayList<MyMsg> getMessages(String query, int batches) {
		ArrayList<MyMsg> myMsgs = new ArrayList<MyMsg>();
		refreshMessages( query, myMsgs, batches, null);
		return myMsgs;
	}

	public interface IProgress {
		void set( int max);
		void move();
	}
	
	public void refreshMessages(String query, ArrayList<MyMsg> oldMsgs, int batches, IProgress bar) {
		try {
			String pageToken = null;

			ArrayList<Message> newMsgs = new ArrayList<Message>();
			
			for (int i = 0; i < batches; i++) { // limit 100 for now
				// get 100 messages
				com.google.api.services.gmail.Gmail.Users.Messages.List list = m_gmail.users().messages().list(userId);
				list.setMaxResults(Long.valueOf(300));
				ListMessagesResponse res = list.setPageToken(pageToken).setQ( query).execute();
				
				// add to array of messages
				if (res.getMessages() != null) {
					for (Message msg : res.getMessages() ) {
						newMsgs.add( msg);
					}
				}
				
				pageToken = res.getNextPageToken();
			}
			
			if (bar != null) {
				bar.set( newMsgs.size() );
			}
			
			// remove messages that are no longer there
			for (Iterator<MyMsg> iter = oldMsgs.iterator(); iter.hasNext(); ) {
				MyMsg oldMsg = iter.next();
				if (!contains( newMsgs, oldMsg.id() ) ) {
					iter.remove();
					//S.out( "removing " + oldMsg.id() );
				}
			}

			// add messages that were not there before
			for (Message msg : newMsgs) {
				if (!myContains( oldMsgs, msg.getId() ) ) {
					Get get = m_gmail.users().messages().get( userId, msg.getId() );
					Message newMsg = get.setFormat( "metadata").execute(); // try metadata (headers labels and snippet) or minimal (snippet and labels)
					oldMsgs.add( new MyMsg( newMsg) );
					//S.out( "adding " + newMsg.getId() );
					if (bar != null) {
						bar.move();
					}
				}
			}
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean contains(ArrayList<Message> msgs, String id) {
		for (Message msg : msgs) {
			if (msg.getId().equals( id) ) {
				return true;
			}
		}
		return false;
	}

	private boolean myContains(ArrayList<MyMsg> msgs, String id) {
		for (MyMsg msg : msgs) {
			if (msg.id().equals( id) ) {
				return true;
			}
		}
		return false;
	}

	public void removeLabel( MyMsg msg, String label) throws IOException {
		ArrayList<String> list = new ArrayList<String>();
		list.add( label);
		
		ModifyMessageRequest req = new ModifyMessageRequest().setRemoveLabelIds( list);
		
		Messages messages = m_gmail.users().messages();
		messages.modify( userId, msg.id(), req).execute();
	}

	/** See bottom of file for some headers. */
	static String getHeader( Message msg, String key) {
		try {
			for (MessagePartHeader header : msg.getPayload().getHeaders() ) {
				if (header.getName().equalsIgnoreCase( key) ) {
					return header.getValue();
				}
			}
		}
		catch( Exception e) {
			// eat it
		}
		return "";
	}

	private static final SimpleDateFormat fmt = new SimpleDateFormat( "d MMM yyyy kk:mm:ss Z");
	private static final SimpleDateFormat fmt2 = new SimpleDateFormat( "MM/dd kk:mm:ss");
	
	public class MyMsg implements Comparable<MyMsg> {
		//private static final Date now = new Date();

		private Message m_msg;
		private String m_from;
		private String m_to;
		private String m_subject;
		private Date m_date;
		private String m_fullMsg;
		
		public String from() 		{ return m_from; }
		public String to() 			{ return m_to; }
		
		public String subject() 	{ return m_subject; }
		public String snippet() 	{ return m_msg.getSnippet(); }
		public String dateStr() 	{ return fmt2.format( m_date); }
		public Date date() 			{ return m_date; }
		public String id()			{ return m_msg.getId(); }
		
		public MyMsg(Message msg) {
			m_msg = msg;
			m_from = getHeader( msg, "From").replaceAll( "\"", "");
			m_to = getHeader( msg, "To").replaceAll( "\"", "");
			m_subject = getHeader( msg, "Subject");
			
			String dateTime = getHeader( msg, "Date");

			try {
				// format could be: May 2017 13:12:13 +0000
				// work backwards, try string.split
				int i = dateTime.indexOf( ' ');
				m_date = fmt.parse( dateTime.substring( i + 1) );
			}
			catch( Exception e) {
				try {
					m_date = fmt.parse( dateTime);
				}
				catch( Exception e2) {
					S.out( dateTime + " " + m_from + " " + m_subject);
					e.printStackTrace();
					m_date = new Date();
				}
			}
		}

		public String getFullMsg() {
			if (m_fullMsg == null) {
				try {
					Get get = m_gmail.users().messages().get( userId, id() );
					Message newMsg = get.setFormat( "full").execute(); // get full msg including payload
					m_fullMsg = StringUtils.newStringUtf8(Base64.decodeBase64(
				        newMsg.getPayload().getParts().get(0).getBody().getData()));
					// tips here: https://stackoverflow.com/questions/28026099/how-to-get-full-message-body-in-gmail
				}
				catch( Exception e) {
					m_fullMsg = m_msg.getSnippet();
				}
			}
			return m_fullMsg;
		}

		@Override public int compareTo(MyMsg o) {
			return m_from.compareTo( o.m_from);
		}
		
		@Override public String toString() {
			return String.format( "%-60s %s %s", m_from, m_subject, m_msg.getSnippet() );
		}

		/*** For debugging. */
		void show() {
			S.out( "snippet");
			S.out( m_msg.getSnippet() );

			MessagePart payload = m_msg.getPayload();

			S.out( "headers");
			List<MessagePartHeader> headers = payload.getHeaders();
			for (MessagePartHeader header : headers) {
				S.out( "" + header);
			}
			S.out( "");

			S.out( "parts");
			List<MessagePart> parts = payload.getParts();
			for (MessagePart part : parts) {
				S.out( "" + part);
				String filename = part.getFilename();
				MessagePartBody body = part.getBody();
				S.out( "filename=%s  body=%s", filename, body.getData() );
			}
			S.out( "");
		}
		
		/** Get rid of Re: Fwd etc. from the header. */
		public void fixSubject() {
			fixSubject( "Re: ");
			fixSubject( "Fwd: ");
			fixSubject( "Fw: ");
		}

		/** Remove string from beginning of subject. */
		private void fixSubject(String string) {
			if (m_subject == null) {
				return;
			}
			if (m_subject.startsWith( string) || m_subject.startsWith( string.toUpperCase() ) ) {
				m_subject = m_subject.substring(string.length());
			}
		}
	}
}

/* Some headers are:
Delivered-To
Received
X-Received
Return-Path
Received
Received-SPF
Authentication-Results
Received
From
To
Subject
Date
List-Unsubscribe
MIME-Version
Reply-To
x-job
Message-ID
Content-Type
Content-Transfer-Encoding
*/