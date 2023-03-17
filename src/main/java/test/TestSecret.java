package test;


import java.io.IOException;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.ProjectName;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersion;

import tw.util.S;

public class TestSecret {

	public static void main(String[] args) throws IOException {
		String projectId = "my-windows-project-361112";
		String secretId = "projects/552719427171/secrets/Daddy/versions/1";

		ProjectName projectName = ProjectName.of(projectId);

		// Initialize client that will be used to send requests. This client only needs to be created
		// once, and can be reused for multiple requests. After completing all of your requests, call
		// the "close" method on the client to safely clean up any remaining background resources.
		try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
			SecretVersion sec = client.getSecretVersion("projects/552719427171/secrets/Daddy/versions/1");
			S.out( "Got sec %s", sec);
			
			AccessSecretVersionResponse response = client.accessSecretVersion(sec.getName());
			String data = response.getPayload().getData().toStringUtf8();
			S.out( data);
		}
	}
}
