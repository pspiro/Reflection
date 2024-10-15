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
public class SmtpSender implements AutoCloseable {
	public static record SmtpUser( String host, String username, String password) {
		/** for sending multiple emails; didn't work well, needs extensive testing */
		public SmtpSender sender() throws Exception { 
			return new SmtpSender( host, username, password); 
		}

		/** Send one email  and close connection */
		public void send( String fromName, String fromEmail, String toEmail, String subject, String body) throws Exception {
			sendOne( host, username, password, fromName, fromEmail, toEmail, subject, body);
		}
	}

	private static final String MyGmail = "peteraspiro@gmail.com";
	private static final String OpenXchange = "smtp.openxchange.eu";
	
	public static SmtpUser Josh = new SmtpUser( OpenXchange, "josh@reflection.trading", "KyvuPRpi7uscVE@");
	public static SmtpUser Peter = new SmtpUser( OpenXchange, "peter@reflection.trading", "mvLYAnCr4*7)");

	private SSLSocket socket;
	private PrintWriter writer;
	private BufferedReader reader;
	private boolean debug;

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
	public void send(String fromName, String fromEmail, String toEmail, String subject, String body) throws IOException {
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
		String responseLine;
		while ((responseLine = reader.readLine()) != null) {
			if (debug) {
				System.out.println(str + ": " + responseLine);
			}
			if (responseLine.charAt(3) == ' ') {
				break;
			}
		}
		return responseLine;
	}
	
	/** Send one email  and close connection */
	public static void sendOne( String host, String username, String password, String fromName, String fromEmail, String toEmail, String subject, String body) throws Exception {
		try (SmtpSender email = new SmtpSender( host, username, password) ) {
			email.send( fromName, fromEmail, toEmail, subject, body);
		}
	}
	
	public static void main(String[] args) throws Exception {
		test();
	}
	
	public static void test() throws Exception {
		// sending multiple on a single connection worked here but not elsewhere and would need extensive testing */
		try (SmtpSender email = Josh.sender() ) { 
			email.send( "Josh", Josh.username(), MyGmail, "hello from josh 1", "hello there");
			email.send( "Josh", Josh.username(), MyGmail, "hello from josh 2", "hello there");
		}
		
		try (SmtpSender email = Peter.sender() ) { 
			email.send( "Peter", Peter.username, MyGmail, "hello from peter", "");
		}
	}
	
	static int code( String str) {
		return Integer.parseInt( firstTok( str) );
	}
	
	static String firstTok( String str) {
		return str.split( " ")[0];
	}
}
