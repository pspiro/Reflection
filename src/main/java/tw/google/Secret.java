package tw.google;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretManagerServiceSettings;
import com.google.common.collect.Lists;

import tw.util.S;

/** The credentials.json file contains the credentials for a specific google service account.
 *  This account must have google secret "Secret Accessor" role.
 *  The VM service account need not have this role.
 *  Note that the service account must have the Editor role as well, or, at least, it doesn't 
 *  work without that.
 *  
 *  Reading the credentials here, as opposed to granting secret access to the VM, also allows 
 *  this to work when non running on the VM, such as when running on PC */
public class Secret {
	public static void main(String[] args) throws Exception {
		String dad = "projects/552719427171/secrets/Daddy/versions/1";
		String apiKey = "projects/552719427171/secrets/fireblocksApiKey/versions/1";
		
		S.out( readValue(apiKey) );
	}

	public static String readValue( String secretId) throws Exception {
		GoogleCredentials credentials = GoogleCredentials
				.fromStream(Secret.class.getClassLoader().getResourceAsStream("credentials.json"))
				.createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));

		SecretManagerServiceSettings settings = SecretManagerServiceSettings
				.newBuilder()
				.setCredentialsProvider(FixedCredentialsProvider.create(credentials))
				.build();

		// Initialize client that will be used to send requests. This client only needs to be created
		// once, and can be reused for multiple requests. After completing all of your requests, call
		// the "close" method on the client to safely clean up any remaining background resources.
		try (SecretManagerServiceClient client = SecretManagerServiceClient.create(settings) ) {

			// or you could use SecretVersionName.of( "")...
			// you can pass versions/latest to get latest version
			String name = client.getSecretVersion(secretId).getName();
			return client
					.accessSecretVersion(name)
					.getPayload()
					.getData()
					.toStringUtf8();
		}
	}
}
