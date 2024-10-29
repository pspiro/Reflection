package common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Base64;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import tw.util.S;

/** This class sends one or more SMTP emails on a single connection.
 * 
 * EuroDNS limits:
 * 500 Messages per hour and user
 * 5000 messages per day and user
 *
 */

// ses-smtp-user.20241015-104833
// smtp username: AKIA3GXEBTOE7ZUQKYWF
// smtp pw: BKH2OchBydo3BfNhbwjpK+/BpTMMTB7Q6799mjiViKMa
// host: email-smtp.us-east-1.amazonaws.com
public class SmtpSender implements AutoCloseable {
	public enum Type { Statement, Newsletter, Message, Trade };  // for the Type SES metric
	
	public static record SmtpUser( String host, String username, String password) {
		/** Send one email  and close connection */
		public void send( String fromName, String fromEmail, String toEmail, String subject, String body, Type type) throws Exception {
			sendOne( host, username, password, fromName, fromEmail, toEmail, subject, body, type);
		}
	}

	private static final String MyGmail = "peteraspiro@gmail.com";
	private static final String MyRefl = "peter@reflection.trading";
	private static final String OpenXchange = "smtp.openxchange.eu";
	public static boolean debug;
	
	public static SmtpUser Josh = new SmtpUser( OpenXchange, "josh@reflection.trading", "KyvuPRpi7uscVE@");
	public static SmtpUser Peter = new SmtpUser( OpenXchange, "peter@reflection.trading", "mvLYAnCr4*7)");
	public static SmtpUser Ses = new SmtpUser( "email-smtp.us-east-1.amazonaws.com", "AKIA3GXEBTOE7ZUQKYWF", "BKH2OchBydo3BfNhbwjpK+/BpTMMTB7Q6799mjiViKMa");
	public static SmtpUser SesAsia = new SmtpUser( "email-smtp.ap-south-1.amazonaws.com", "AKIA3GXEBTOEYUZ6RL6G", "BNgHTgIJkzxY/i/b6TGTEUV5mjbWG0eDJF7PFEpIp0GR");
	
	private SSLSocket socket;
	private PrintWriter writer;
	private BufferedReader reader;
	
	public SmtpSender( String host, String username, String password) throws Exception {
		var sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

		int port = 465;  // SSL port for SMTPS

		this.socket = (SSLSocket) sslSocketFactory.createSocket(host, port);

		writer = new PrintWriter(socket.getOutputStream(), true);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
		// receive the first response after connecting
		receive();

		// HELO command
		send("HELO reflection.trading");
		receive();

		// AUTH LOGIN
		send("AUTH LOGIN");
		receive();

		// Send base64-encoded username
		send(Base64.getEncoder().encodeToString(username.getBytes()));
		receive();

		// Send base64-encoded password
		send(Base64.getEncoder().encodeToString(password.getBytes()));
		var resp = receive();
		Util.require( code( resp) == 235, "Authentication error  host=%s  username=%s - %s", host, username, resp);
	}

	// Send message method
	public void send(String fromName, String fromEmail, String toEmail, String subject, String body, Type type) throws IOException {
		S.out( "Sending email  from:%s <%s>  to:%s  subject:%s", fromName, fromEmail, toEmail, subject);
		
		send("MAIL FROM:<" + fromEmail + ">");
		receive();

		send("RCPT TO:<" + toEmail + ">");
		receive();

		send("DATA");
		receive();

		send("Subject: " + subject);
		send("From: " + fromName + " <" + fromEmail + ">");
		send("To: " + toEmail);
		send("Content-Type: text/html; charset=UTF-8");
		send("X-SES-CONFIGURATION-SET: MySet");
		send("User: " + toEmail);
		send("Type: " + type);
		send("");
		send(body);
		send(".");
		receive();
	}

	// Close the connection
	@Override public void close() throws IOException {
		send("QUIT");
		receive();
		socket.close();
	}
	
	private void send( String str) {
		dbg( "SEND: " + str);
		writer.println( str);
	}

	// Utility method to print server response
	private String receive() throws IOException {
		String responseLine;
		while ((responseLine = reader.readLine()) != null) {
			dbg( "  REC: " + responseLine);
			if (responseLine.charAt(3) == ' ') {
				dbg( "  END");
				break;
			}
		}
		return responseLine;
	}

	private void dbg(String string) {
		if (debug) {
			S.out( string);
		}
	}
	
	/** Send one email  and close connection */
	public static void sendOne( String host, String username, String password, String fromName, String fromEmail, String toEmail, String subject, String body, Type type) throws Exception {
		try (SmtpSender email = new SmtpSender( host, username, password) ) {
			email.send( fromName, fromEmail, toEmail, subject, body, type);
		}
	}
	
	static int code( String str) {
		return Integer.parseInt( firstTok( str) );
	}
	
	static String firstTok( String str) {
		return str.split( " ")[0];
	}
	
	public static void main(String[] args) throws Exception {
		debug = true;
		S.out( "sending through US");
		Ses.send("josh", "josh@reflection.trading", "bob@reflection.trading", "sub2", "body", Type.Statement);
		S.out( "");
		S.out( "sending through Asia");
		SesAsia.send("josh", "josh@reflection.trading", "bob@reflection.trading", "sub2", "body", Type.Statement);
	}
}
