package nl.tno.essim.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.TokenVerifier;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;

class ESDLDriveTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@Test
	void testESDLDriveAccess() throws VerificationException, IOException, InterruptedException {
		String username = "ewoud.werkman@tno.nl";
		System.out.println("Username: " + username);
		String password = readLine("Password:");
		//String jwt = getJWTToken(username, password);
		//System.out.println("JWT:" + jwt);
		//AccessToken token = jwtToToken(jwt);
		//System.out.println(token);
		//System.out.println("Welcome " + token.getName() + " - " + token.getEmail());
		
		String jwt = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJWVkZPMHV1aEN5ejcxZG01NlhVR1psZFc0WXFQYk5kVXZmbnQtRnBVRjBJIn0.eyJqdGkiOiIzMjcxMWMwZC0yN2FjLTQxZGMtYTFkNi1mNGIxOTYxOTI0ODMiLCJleHAiOjE2NjU2NzYyNjcsIm5iZiI6MCwiaWF0IjoxNjY1Njc1OTY3LCJpc3MiOiJodHRwczovL2lkbS5oZXNpLmVuZXJneS9hdXRoL3JlYWxtcy9lc2RsLW1hcGVkaXRvciIsImF1ZCI6WyJlc2RsLW1hcGVkaXRvciIsInJlYWxtLW1hbmFnZW1lbnQiLCJjZG8tbW9uZGFpbmUiLCJhY2NvdW50Il0sInN1YiI6ImJlYTIyNjFkLTZmZGQtNDA2MS05Y2Y2LWIyZDgyNzc1ODBmMCIsInR5cCI6IkJlYXJlciIsImF6cCI6ImN1cmwiLCJhdXRoX3RpbWUiOjAsInNlc3Npb25fc3RhdGUiOiIzMzcxMTZiNS1lNGIwLTQwZmUtOTljOC0xYmIyZTg1NDliNmQiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwiY2RvX3JlYWQiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7InJlYWxtLW1hbmFnZW1lbnQiOnsicm9sZXMiOlsibWFuYWdlLXVzZXJzIiwidmlldy11c2VycyIsInF1ZXJ5LWdyb3VwcyIsInF1ZXJ5LXVzZXJzIl19LCJlc2RsLW1hcGVkaXRvciI6eyJyb2xlcyI6WyJtYXBlZGl0b3ItYWRtaW4iXX0sImNkby1tb25kYWluZSI6eyJyb2xlcyI6WyJlc2RsLWRyaXZlLWFkbWluIiwiZWRyLXB1YmxpYy13cml0ZSJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgdXNlcl9ncm91cF9wYXRoIGVtYWlsIHByb2ZpbGUgZXNkbC1tYXBlZGl0b3IgbWljcm9wcm9maWxlLWp3dCIsImVzZGwtZHJpdmUtcmVhZC1vbmx5IjpbIi9Qcm9qZWN0cy9TaGFyZWQgTW9kZWxzIl0sInVwbiI6ImV3b3VkLndlcmttYW5AdG5vLm5sIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJFd291ZCBXZXJrbWFuIiwiZ3JvdXBzIjpbIm9mZmxpbmVfYWNjZXNzIiwiY2RvX3JlYWQiLCJ1bWFfYXV0aG9yaXphdGlvbiJdLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJld291ZC53ZXJrbWFuQHRuby5ubCIsImdpdmVuX25hbWUiOiJFd291ZCIsImVzZGwtZHJpdmUtd3JpdGUiOlsiL1Byb2plY3RzL1NoYXJlZCBNb2RlbHMiXSwidXNlcl9ncm91cF9wYXRoIjpbIi9Qcm9qZWN0cy9kZXZlbG9wbWVudCIsIi9Qcm9qZWN0cy9FbmVyZ2llIGVuIFJ1aW10ZSIsIi9Qcm9qZWN0cy9FU1NJTS1LUEkgdGVzdCIsIi9Qcm9qZWN0cy9Hby1FIiwiL09yZ2FuaXphdGlvbnMvVE5PL01DUyIsIi9Qcm9qZWN0cy9Nb25kYWluZSIsIi9Qcm9qZWN0cy9Qb2NpdHlmIiwiL1B1YmxpYyIsIi9lZHIvUHVibGljIiwiL1Byb2plY3RzL1NlZWRFUlAgLSBFbmVyZ3kgbW9kZWxsaW5nIiwiL1Byb2plY3RzL1NoYXJlZCBNb2RlbHMiLCIvVGVzdCBncm91cCIsIi9Qcm9qZWN0cy9XYXJtaW5nVXAiXSwiZmFtaWx5X25hbWUiOiJXZXJrbWFuIiwiZW1haWwiOiJld291ZC53ZXJrbWFuQHRuby5ubCJ9.iGKvONgBPgrDqEtUfEjNLjZwZNb6ugPAe7cYJlPnd1EFLpGOUSIPevuTSdvK0819NTryLjBPL0fMXh8u_bSJIX07l-NwE-yNsZ-JqFcgZ0ymw0TIz-vpmRrJ1UutOrIxfSA4tIZO8yyx2Ha2tyU7hZiAt6tZR0bwh_A2vK8kGrWIXRmf9MGiZ_0dKypI7RmjrQuVG5k_O1NuuCU-hrtm2dIavol3URcsQvdAqEQQV6BW-mHr4Stm1SxQSsuxOPmhvsJySikfFfmMTB8bvqt4dVaQIP-eop9OOEhbe16cTRa-AOAYWjrw2HBwwk8g513JCBTY2ikHK4pD3DA0yttn3w";
		
		
		HttpClient client = HttpClient.newBuilder().build();
		HttpRequest httpRequest = HttpRequest.newBuilder()
			.GET()
			.uri(URI.create("https://drive.esdl.hesi.energy/store/resource/Projects/development/ESSIM%20error%20marginal%20costs/ED-Imp-Exp.esdl"))
			.header("Authorization", "Bearer " + jwt)
			.header("Content-Type", "application/xml")
			.build();
		
		HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
		System.out.println(httpResponse);
		String body = httpResponse.body();
		System.out.println(body);
	}
	
	private String getJWTToken(String username, String password) throws VerificationException {
		AuthzClient authzClient = AuthzClient.create();
		AccessTokenResponse response = authzClient.obtainAccessToken(username, password);
		String rpt = response.getToken();
		return rpt;		
	}
	
	private AccessToken jwtToToken(String jwt) throws VerificationException {
		TokenVerifier<AccessToken> verifier = TokenVerifier.create(jwt, AccessToken.class);
		//TokenVerifier<IDToken> verifiedToken = verifier.verify(); // needs public key
		AccessToken token = verifier.getToken();
		return token;
	}
	
	private String readLine(String format, Object... args) throws IOException {
        if (System.console() != null) {
            return System.console().readLine(format, args);
        }
        System.out.print(String.format(format, args));
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                System.in));
        return reader.readLine();
    }

}
