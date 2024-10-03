package reflection;

public class Text {
	
	static String portRow = """
		<tr>
			<td style="border: 1px solid #ddd; padding: 8px;">#token#</td>
			<td style="border: 1px solid #ddd; padding: 8px;">#quantity#</td>
			<td style="border: 1px solid #ddd; padding: 8px;">$#price#</td>
			<td style="border: 1px solid #ddd; padding: 8px;">$#value#</td>
		</tr>
		""";
	
	static String actRow = """
		<tr>
			<td style="border: 1px solid #ddd; padding: 8px;">#date#</td>
			<td style="border: 1px solid #ddd; padding: 8px;">#description#</td>
			<td style="border: 1px solid #ddd; padding: 8px;">$#amount#</td>
		</tr>
		""";
	
	
	static String email = """
<body style="font-family: Arial, sans-serif; background-color: #E2E0F9; margin: 0; padding: 10px;">
	<div style="max-width: 600px; margin: 20px auto; background-color: #ffffff; padding: 10px; border-radius: 8px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);">
		<div style="background-color: #8775E6; color: #ffffff; padding: 5px; text-align: center; border-top-left-radius: 8px; border-top-right-radius: 8px;">
			<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border-collapse: collapse;">
				<tr>
					<td style="background-color: #8775E6; color: #ffffff; padding: 15px; text-align: left; border-top-left-radius: 8px; border-top-right-radius: 8px;">
						<img src="https://i.ibb.co/MPTVLX4/Logo-with-bg-icon.png" alt="Reflection Logo" style="width: 50px; height: 50px; display: inline-block; vertical-align: middle; margin-right: 10px;">
						<h1 style="margin: 0; font-size: 24px; display: inline-block; vertical-align: middle; padding: 0;">Your Daily Reflection</h1>
					</td>
				</tr>
			</table>
		</div>
		
		<div style="margin-bottom: 30px;">
			<h2 style="font-size: 18px; margin-bottom: 5px; padding-bottom: 5px; text-decoration: underline;">Customer Info</h2>
			<p style="margin: 1px 0;"><strong>Name:</strong> #name#</p>
			<p style="margin: 7px 0;"><strong>Wallet:</strong> #wallet#</p>
		</div>

		<div style="margin-bottom: 30px;">
			<h2 style="font-size: 18px; margin-bottom: 5px; padding-bottom: 5px; text-decoration: underline;">Portfolio Overview</h2>
			<table style="width: 100%; border-collapse: collapse; margin-bottom: 20px;">
				<thead>
					<tr>
						<th style="background-color: #f2f2f2; border: 1px solid #ddd; padding: 8px; text-align: left;">Token</th>
						<th style="background-color: #f2f2f2; border: 1px solid #ddd; padding: 8px; text-align: left;">Quantity</th>
						<th style="background-color: #f2f2f2; border: 1px solid #ddd; padding: 8px; text-align: left;">Price</th>
						<th style="background-color: #f2f2f2; border: 1px solid #ddd; padding: 8px; text-align: left;">Value</th>
					</tr>
				</thead>
				<tbody>
					#portrows#
					<tr>
						<td colspan="3" style="text-align: right; font-weight: bold; border: 1px solid #ddd; padding: 8px;">Total:</td> <!-- Merge first 3 columns, align right -->
						<td style="font-weight: bold; border: 1px solid #ddd; padding: 8px;">#total#</td>
					</tr>
				</tbody>
			</table>
		</div>
		<div style="margin-bottom: 30px;">
			<h2 style="font-size: 18px; margin-bottom: 5px; padding-bottom: 5px; text-decoration: underline;">Recent Transactions</h2>
			<table style="width: 100%; border-collapse: collapse; margin-bottom: 20px;">
				<thead>
					<tr>
						<th style="background-color: #f2f2f2; border: 1px solid #ddd; padding: 8px; text-align: left;">Date</th>
						<th style="background-color: #f2f2f2; border: 1px solid #ddd; padding: 8px; text-align: left;">Description</th>
						<th style="background-color: #f2f2f2; border: 1px solid #ddd; padding: 8px; text-align: left;">Amount</th>
					</tr>
				</thead>
				<tbody>
					#actrows#
				</tbody>
			</table>
		</div>
		<div style="font-size: 14px; color: black; padding: 5px; text-align: center;">
			<p style="margin: 0;">Thank you for using Reflection. Have a great day!</p>
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
