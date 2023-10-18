package testcase;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class TestMail {
    public static void main(String[] args) {
      final String username = "josh@reflection.trading";
      final String password = "KyvuPRpi7uscVE@";
      
      Properties props = new Properties();
      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.host", "smtp.openxchange.eu");
      props.put("mail.smtp.port", "587");

      Session session = Session.getInstance(props,
        new javax.mail.Authenticator() {
          protected PasswordAuthentication getPasswordAuthentication() {
              return new PasswordAuthentication(username, password);
          }
        });

      try {
          Message message = new MimeMessage(session);
          message.setFrom(new InternetAddress(username));
          message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("peteraspiro@gmail.com"));
          message.setSubject("Test Email");
          message.setText("Hello, this is a test email.");

          Transport.send(message);

          System.out.println("Email sent successfully");

      } catch (MessagingException e) {
          e.printStackTrace();
      }
  }

}
