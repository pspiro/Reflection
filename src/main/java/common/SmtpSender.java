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
		printServerResponse("connected");

		// HELO command
		writer.println("HELO localhost");
		printServerResponse("helo");

		// AUTH LOGIN
		writer.println("AUTH LOGIN");
		printServerResponse("login");

		// Send base64-encoded username
		writer.println(Base64.getEncoder().encodeToString(username.getBytes()));
		printServerResponse("username");

		// Send base64-encoded password
		writer.println(Base64.getEncoder().encodeToString(password.getBytes()));
		var resp = printServerResponse("password");
		Util.require( code( resp) == 235, "Authentication error  host=%s  username=%s - %s", host, username, resp);
	}

	// Send message method
	public void send(String fromName, String fromEmail, String toEmail, String subject, String body, Type type) throws IOException {
		S.out( "Sending email  from:%s <%s>  to:%s  subject:%s", fromName, fromEmail, toEmail, subject);
		
		writer.println("MAIL FROM:<" + fromEmail + ">");
		printServerResponse("mail from");

		writer.println("RCPT TO:<" + toEmail + ">");
		printServerResponse("receipt to:");

		writer.println("DATA");
		printServerResponse("data");

		writer.println("Subject: " + subject);
		writer.println("From: " + fromName + " <" + fromEmail + ">");
		writer.println("To: " + toEmail);
		writer.println("Content-Type: text/html; charset=UTF-8");
		writer.println("X-SES-CONFIGURATION-SET: MySet");
		writer.println("User: " + toEmail);
		writer.println("Type: " + type);
		writer.println();
		writer.println(body);
		writer.println(".");
		printServerResponse("sent");
	}

	// Close the connection
	@Override public void close() throws IOException {
		writer.println("QUIT");
		printServerResponse("quit");
		socket.close();
	}

	// Utility method to print server response
	private String printServerResponse(String str) throws IOException {
		dbg( "SEND: " + str);
		
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
		S.out( "send US");
		Ses.send("josh", "josh@reflection.trading", "bob@reflection.trading", "sub2", "body", Type.Statement);
		S.out( "");
		S.out( "send Asia");
		SesAsia.send("josh", "josh@reflection.trading", "bob@reflection.trading", "sub2", "body", Type.Statement);
	}
}
