package com.mplescano.oauth.poc.poc01;

import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GrantByResourceOwnerPasswordCredentialTest extends Oauth2SupportTest {

    private int portApi = 8090;
    
    private int portOauth = 8080;
    
    private static final String CLIENT_ID_01 = "my-trusted-app";
    
    private static final String CLIENT_PASS_01 = "secret";
    
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void getJwtTokenByClientCredentialForUser() throws Exception {
        String userName = "bob";//ROLE_USER
        String password = "abc123";
		ResponseEntity<String> response = new TestRestTemplate(CLIENT_ID_01, CLIENT_PASS_01).postForEntity(
				"http://localhost:" + portOauth + "/oauth/token?grant_type=password&username=" + userName + "&password=" + password, null,
				String.class);
		String responseText = response.getBody();
		Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
		HashMap jwtMap = new ObjectMapper().readValue(responseText, HashMap.class);

		Assert.assertEquals("bearer", jwtMap.get("token_type"));
        Assert.assertTrue(((String) jwtMap.get("scope")).contains("read"));
        Assert.assertTrue(((String) jwtMap.get("scope")).contains("write"));
		Assert.assertTrue(jwtMap.containsKey("access_token"));
		Assert.assertTrue(jwtMap.containsKey("expires_in"));
		Assert.assertTrue(jwtMap.containsKey("jti"));
		String accessToken = (String) jwtMap.get("access_token");

		Jwt jwtToken = JwtHelper.decode(accessToken);
		String claims = jwtToken.getClaims();
		HashMap claimsMap = new ObjectMapper().readValue(claims, HashMap.class);
		Assert.assertEquals("spring-oauth2-poc01", ((List<String>) claimsMap.get("aud")).get(0));
		Assert.assertEquals(CLIENT_ID_01, claimsMap.get("client_id"));
		Assert.assertEquals(userName, claimsMap.get("user_name"));
		Assert.assertEquals("read", ((List<String>) claimsMap.get("scope")).get(0));
		Assert.assertEquals("write", ((List<String>) claimsMap.get("scope")).get(1));
		List<String> authorities = (List<String>) claimsMap.get("authorities");
        Assert.assertTrue(authorities.size() >= 1);
        Assert.assertTrue(authorities.contains("ROLE_USER"));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void getJwtTokenByClientCredentialForAdmin() throws Exception {
        String userName = "bill";//ROLE_ADMIN
        String password = "abc123";
		ResponseEntity<String> response = new TestRestTemplate(CLIENT_ID_01, CLIENT_PASS_01).postForEntity(
				"http://localhost:" + portOauth + "/oauth/token?grant_type=password&username="+userName+"&password=" + password, null,
				String.class);
		String responseText = response.getBody();
		Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
		HashMap jwtMap = new ObjectMapper().readValue(responseText, HashMap.class);

		Assert.assertEquals("bearer", jwtMap.get("token_type"));
        Assert.assertTrue(((String) jwtMap.get("scope")).contains("read"));
        Assert.assertTrue(((String) jwtMap.get("scope")).contains("write"));
		Assert.assertTrue(jwtMap.containsKey("access_token"));
		Assert.assertTrue(jwtMap.containsKey("expires_in"));
		Assert.assertTrue(jwtMap.containsKey("jti"));
		String accessToken = (String) jwtMap.get("access_token");

		Jwt jwtToken = JwtHelper.decode(accessToken);
		String claims = jwtToken.getClaims();
		HashMap claimsMap = new ObjectMapper().readValue(claims, HashMap.class);
		Assert.assertEquals("spring-oauth2-poc01", ((List<String>) claimsMap.get("aud")).get(0));
		Assert.assertEquals(CLIENT_ID_01, claimsMap.get("client_id"));
		Assert.assertEquals(userName, claimsMap.get("user_name"));
		Assert.assertEquals("read", ((List<String>) claimsMap.get("scope")).get(0));
		Assert.assertEquals("write", ((List<String>) claimsMap.get("scope")).get(1));
		List<String> authorities = (List<String>) claimsMap.get("authorities");
        Assert.assertTrue(authorities.size() >= 1);
        Assert.assertTrue(authorities.contains("ROLE_ADMIN"));
	}

	@Test
	public void accessProtectedResourceByJwtTokenForUser() throws Exception {
        String userName = "bob";//ROLE_USER
        String password = "abc123";
		ResponseEntity<String> response = new TestRestTemplate()
				.getForEntity("http://localhost:" + portApi + "/resources/user", String.class);
		Assert.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

		response = new TestRestTemplate(CLIENT_ID_01, CLIENT_PASS_01).postForEntity(
				"http://localhost:" + portOauth + "/oauth/token?grant_type=password&username="+userName+"&password=" + password, null,
				String.class);
		String responseText = response.getBody();
		Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
		HashMap jwtMap = new ObjectMapper().readValue(responseText, HashMap.class);
		String accessToken = (String) jwtMap.get("access_token");

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + accessToken);

		response = new TestRestTemplate().exchange("http://localhost:" + portApi + "/resources/user", HttpMethod.GET,
				new HttpEntity<>(null, headers), String.class);
		Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

		response = new TestRestTemplate().exchange("http://localhost:" + portApi + "/resources/principal", HttpMethod.GET,
				new HttpEntity<>(null, headers), String.class);
		Assert.assertEquals("bob", response.getBody());

		response = new TestRestTemplate().exchange("http://localhost:" + portApi + "/resources/roles", HttpMethod.GET,
				new HttpEntity<>(null, headers), String.class);
		Assert.assertTrue(response.getBody().contains("{\"authority\":\"ROLE_USER\"}"));
	}

	@Test
	public void accessProtectedResourceByJwtTokenForAdmin() throws Exception {
        String userName = "bill";//ROLE_ADMIN
        String password = "abc123";
        int statusCode = 0;
        ResponseEntity<String> response = null;
        try {
	        buildTestRestTemplate(restTemplateBuilder)
	        		.getForEntity("http://localhost:" + portApi + "/resources/admin", String.class);
		}
		catch (RestClientResponseException ex) {
			statusCode = ex.getRawStatusCode();
		}
		Assert.assertEquals(HttpStatus.UNAUTHORIZED.value(), statusCode);

		response = new TestRestTemplate(CLIENT_ID_01, CLIENT_PASS_01).postForEntity(
				"http://localhost:" + portOauth + "/oauth/token?grant_type=password&username="  +userName + "&password=" + password, null,
				String.class);
		String responseText = response.getBody();
		Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
		HashMap jwtMap = new ObjectMapper().readValue(responseText, HashMap.class);
		String accessToken = (String) jwtMap.get("access_token");

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + accessToken);

		response = new TestRestTemplate().exchange("http://localhost:" + portApi + "/resources/admin", HttpMethod.GET,
				new HttpEntity<>(null, headers), String.class);
		Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

		response = new TestRestTemplate().exchange("http://localhost:" + portApi + "/resources/principal", HttpMethod.GET,
				new HttpEntity<>(null, headers), String.class);
		Assert.assertEquals(userName, response.getBody());

		response = new TestRestTemplate().exchange("http://localhost:" + portApi + "/resources/roles", HttpMethod.GET,
				new HttpEntity<>(null, headers), String.class);
		Assert.assertTrue(response.getBody().contains("{\"authority\":\"ROLE_ADMIN\"}"));
	}
}