package common;

import common.SmtpSender.Type;
import reflection.Config;
import tw.util.S;

public class SendEmailToAll {
	public static void main(String[] args) throws Exception {
		boolean send = false;
		
		if (send) {
			sendAll();
		}
		else {
			test();
		}
		
		S.out( "done");
	}
	
	static void test() throws Exception {
		send( "peteraspiro@gmail.com", "Peter");
	}
	
	static void sendAll() throws Exception {
		var config = Config.ask("Prod");
		for (var user : config.sqlQuery( "select first_name, email from users") ) {
			
			String email = user.getString( "email");
			if (Util.isValidEmail( email) ) {
				send( email, user.getString( "first_name"));
			}
		}
	}

	private static void send(String email, String name) throws Exception {
		String text = templ.replaceAll( "#firstname#", name);
	
		SmtpSender.Josh.send("Josh", "josh@reflection.trading", email, "Reflection trading promotion has concluded", text, Type.Message);
	}
	
	static String templ = """
<body style="font-family: Arial, sans-serif; background-color: #E2E0F9; margin: 0; padding: 10px;">
	<div style="max-width: 600px; margin: 20px auto; background-color: #ffffff; padding: 10px; border-radius: 8px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);">
		<div style="background-color: #8775E6; color: #ffffff; padding: 5px; text-align: center; border-top-left-radius: 8px; border-top-right-radius: 8px;">
			<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border-collapse: collapse;">
				<tr>
				<td style="background-color: #8775E6; color: #ffffff; padding: 15px; text-align: left; border-top-left-radius: 8px; border-top-right-radius: 8px;">
				<img src="https://i.ibb.co/m9F84CY/Logo-with-bg-icon.png" alt="Reflection Logo" style="width: 50px; height: 50px; display: inline-block; vertical-align: middle; margin-right: 10px;">
				<h1 style="margin: 0; font-size: 24px; display: inline-block; vertical-align: middle; padding: 0;">Reflection</h1>
				</td>
				</tr>
			</table>
		</div>		
	
		<div>
			Dear #firstname#,<br>
			<br>
			The Reflection $500 trading promotion has concluded. This is your last chance to cash out your reward!<br>
			You must:<br>
			* Close out all open positions<br>
			* Submit a Redemption request from the Dashboard<br>
			<br>
			Remember, you are only entitled to a reward if you earned a profit while trading.<br>
			If you had a loss--we hope you enjoyed trading, and better luck next time!
			<br>
			Thank you for participating in our promotion. We hope to see you back trading on Reflection soon!<br>
			<br>
			-The Reflection Team<br>
		</div>
	</div>

	<div style="margin-top: 20px; font-size: 12px; color: #555555; text-align: center;">
		Reflection.Trading Inc<br>
		6th Floor, Water's Edge Building 1<br>
		Wickham's Cay II, Road Town<br>
		Tortola, British Virgin Islands<br><br>
		You are receiving this email because you registered on Reflection<br>
	</div>
</body>
""";
}
