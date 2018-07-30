package com.mplescano.oauth.poc.poc01;

import java.util.HashMap;
import java.util.List;

import org.jose4j.jwt.consumer.JwtContext;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GrantByClientCredentialTest extends Oauth2SupportTest {

    private int portApi = 8090;
    
    private int portOauth = 8080;
    
    private static final String CLIENT_ID_01 = "my-trusted-app";
    
    private static final String CLIENT_PASS_01 = "secret";
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void getJwtTokenByTrustedClient() throws Exception {
        ResponseEntity<String> response = buildTestRestTemplate(restTemplateBuilder, CLIENT_ID_01, CLIENT_PASS_01)
        		.postForEntity("http://localhost:" + portOauth + "/oauth/token?client_id=" + CLIENT_ID_01 + "&grant_type=client_credentials", null, String.class);
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
        logJson(claims);

        HashMap claimsMap = new ObjectMapper().readValue(claims, HashMap.class);
        Assert.assertEquals("spring-oauth2-poc01", ((List<String>) claimsMap.get("aud")).get(0));
        Assert.assertEquals(CLIENT_ID_01, claimsMap.get("client_id"));
        Assert.assertEquals("read", ((List<String>) claimsMap.get("scope")).get(0));
        Assert.assertEquals("write", ((List<String>) claimsMap.get("scope")).get(1));
        List<String> authorities = (List<String>) claimsMap.get("authorities");
        Assert.assertTrue(authorities.size() >= 1);
        Assert.assertTrue(authorities.contains("ROLE_TRUSTED_CLIENT"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test(expected = RestClientException.class)
    public void accessWithUnknownClientID() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Requested-With", "XMLHttpRequest");
        ResponseEntity<String> response = buildTestRestTemplate(restTemplateBuilder, CLIENT_ID_01, "wrongpass")
        		.postForEntity("http://localhost:" + portOauth + "/oauth/token?client_id=" + CLIENT_ID_01 + "&grant_type=client_credentials", new HttpEntity<>(headers), String.class);			
    }

    @Test
    public void accessProtectedResourceByJwtToken() throws Exception {
    	ResponseEntity<String> response = null;
        int statusCode = 0;
        try {
	        buildTestRestTemplate(restTemplateBuilder)
	        		.getForEntity("http://localhost:" + portApi + "/resources/client", String.class);
		}
		catch (RestClientResponseException ex) {
			statusCode = ex.getRawStatusCode();
		}
        Assert.assertEquals(HttpStatus.UNAUTHORIZED.value(), statusCode);

        response = buildTestRestTemplate(restTemplateBuilder, CLIENT_ID_01, CLIENT_PASS_01)
        		.postForEntity("http://localhost:" + portOauth + "/oauth/token?client_id=" + CLIENT_ID_01 + "&grant_type=client_credentials", null, String.class);
        String responseText = response.getBody();
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        HashMap jwtMap = new ObjectMapper().readValue(responseText, HashMap.class);
        String accessToken = (String) jwtMap.get("access_token");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        JwtContext jwtContext = jwtConsumer.process(accessToken);
        logJWTClaims(jwtContext);

        response = new TestRestTemplate().exchange("http://localhost:" + portApi + "/resources/principal", HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
        Assert.assertEquals(CLIENT_ID_01, response.getBody());

        response = new TestRestTemplate().exchange("http://localhost:" + portApi + "/resources/trusted_client", HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

        response = new TestRestTemplate().exchange("http://localhost:" + portApi + "/resources/roles", HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
        Assert.assertTrue(response.getBody().contains("{\"authority\":\"ROLE_TRUSTED_CLIENT\"}"));

    }
}