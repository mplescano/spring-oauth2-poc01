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

import com.fasterxml.jackson.databind.ObjectMapper;

public class GrantByClientCredentialTest extends Oauth2SupportTest {

    private int portApi = 8090;
    
    private int portOauth = 8080;
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void getJwtTokenByTrustedClient() throws Exception {
        ResponseEntity<String> response = new TestRestTemplate("trusted-app", "secret").postForEntity("http://localhost:" + portOauth + "/oauth/token?client_id=trusted-app&grant_type=client_credentials", null, String.class);
        String responseText = response.getBody();
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        HashMap jwtMap = new ObjectMapper().readValue(responseText, HashMap.class);

        Assert.assertEquals("bearer", jwtMap.get("token_type"));
        Assert.assertEquals("read write", jwtMap.get("scope"));
        Assert.assertTrue(jwtMap.containsKey("access_token"));
        Assert.assertTrue(jwtMap.containsKey("expires_in"));
        Assert.assertTrue(jwtMap.containsKey("jti"));
        String accessToken = (String) jwtMap.get("access_token");

        Jwt jwtToken = JwtHelper.decode(accessToken);

        String claims = jwtToken.getClaims();
        logJson(claims);

        HashMap claimsMap = new ObjectMapper().readValue(claims, HashMap.class);
        Assert.assertEquals("spring-boot-application", ((List<String>) claimsMap.get("aud")).get(0));
        Assert.assertEquals("trusted-app", claimsMap.get("client_id"));
        Assert.assertEquals("read", ((List<String>) claimsMap.get("scope")).get(0));
        Assert.assertEquals("write", ((List<String>) claimsMap.get("scope")).get(1));
        List<String> authorities = (List<String>) claimsMap.get("authorities");
        Assert.assertEquals(1, authorities.size());
        Assert.assertEquals("ROLE_TRUSTED_CLIENT", authorities.get(0));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test(expected = ResourceAccessException.class)
    public void accessWithUnknownClientID() throws Exception {
        ResponseEntity<String> response = new TestRestTemplate("trusted-app", "secrets").postForEntity("http://localhost:" + portOauth + "/oauth/token?client_id=trusted-app&grant_type=client_credentials", null, String.class);
    }

    @Test
    public void accessProtectedResourceByJwtToken() throws Exception {
        ResponseEntity<String> response = new TestRestTemplate().getForEntity("http://localhost:" + portApi + "/resources/client", String.class);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        response = new TestRestTemplate("trusted-app", "secret").postForEntity("http://localhost:" + portOauth + "/oauth/token?client_id=trusted-app&grant_type=client_credentials", null, String.class);
        String responseText = response.getBody();
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        HashMap jwtMap = new ObjectMapper().readValue(responseText, HashMap.class);
        String accessToken = (String) jwtMap.get("access_token");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        JwtContext jwtContext = jwtConsumer.process(accessToken);
        logJWTClaims(jwtContext);

        response = new TestRestTemplate().exchange("http://localhost:" + portApi + "/resources/principal", HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
        Assert.assertEquals("trusted-app", response.getBody());

        response = new TestRestTemplate().exchange("http://localhost:" + portApi + "/resources/trusted_client", HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

        response = new TestRestTemplate().exchange("http://localhost:" + portApi + "/resources/roles", HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
        Assert.assertEquals("[{\"authority\":\"ROLE_TRUSTED_CLIENT\"}]", response.getBody());

    }
}
