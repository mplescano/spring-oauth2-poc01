package com.mplescano.oauth.poc.poc01;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;


public class TokenRevocationLiveTest extends Oauth2SupportTest {
    
    private String refreshToken;
    
    private int portOauth = 8080;
    
    private static final String CLIENT_ID_01 = "my-trusted-app";
    
    private static final String CLIENT_PASS_01 = "secret";
    
    private String obtainAccessToken(String clientId, String username, String password) throws Exception {
        ResponseEntity<String> response = buildTestRestTemplate(restTemplateBuilder, clientId, "secret")
        		.postForEntity("http://localhost:" + portOauth + "/oauth/token?client_id=" + clientId + "&grant_type=password&username=" + username + "&password=" + password, null, String.class);
        String responseText = response.getBody();
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        
        HashMap jwtMap = new ObjectMapper().readValue(responseText, HashMap.class);
     
        Assert.assertTrue(jwtMap.containsKey("access_token"));
        Assert.assertTrue(jwtMap.containsKey("refresh_token"));
        
        refreshToken = (String) jwtMap.get("refresh_token");
         
        return (String) jwtMap.get("access_token");
    }
     
    private String obtainRefreshedAccessToken(String clientId) throws Exception {
        ResponseEntity<String> response = buildTestRestTemplate(restTemplateBuilder, clientId, "secret")
        		.postForEntity("http://localhost:" + portOauth + "/oauth/token?client_id=" + clientId + "&grant_type=refresh_token&refresh_token=" + refreshToken, null, String.class);
        String responseText = response.getBody();
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        
        HashMap jwtMap = new ObjectMapper().readValue(responseText, HashMap.class);
        
        refreshToken = (String) jwtMap.get("refresh_token");

        return (String) jwtMap.get("access_token");
    }

    @Test
    public void givenUser_whenRevokeRefreshToken_thenRefreshTokenInvalidError() throws Exception {
        String clientId = "fooClientIdPassword";
        String accessToken2 = obtainAccessToken(clientId, "tom", "111");

        String accessToken3 = obtainRefreshedAccessToken(clientId);
        
        ResponseEntity<String> listTokensResponse = buildTestRestTemplate(restTemplateBuilder, clientId, "secret")
        		.exchange("http://localhost:" + portOauth + "/oauth/token/list?token=" + accessToken3, HttpMethod.GET, null, String.class);
        assertEquals(200, listTokensResponse.getStatusCodeValue());
        
        ResponseEntity<String> revokeAccessTokenResponse = buildTestRestTemplate(restTemplateBuilder, "fooClientIdPassword", "secret")
                .exchange("http://localhost:" + portOauth + "/oauth/token?token=" + accessToken3 + "&type=access_token", HttpMethod.DELETE, null, String.class);
        assertEquals(200, revokeAccessTokenResponse.getStatusCodeValue());
        
        try {
            buildTestRestTemplate(restTemplateBuilder, clientId, "secret")
                    .exchange("http://localhost:" + portOauth + "/oauth/token/list?token=" + accessToken3, HttpMethod.GET, null, String.class);
            fail("Expected fail");
        }
        catch (Exception ex) {
            assertTrue("Expected exception",ex instanceof RestClientResponseException);
            assertTrue("Expected 401", ((RestClientResponseException) ex).getRawStatusCode() == 401);
        }
        
        accessToken3 = obtainRefreshedAccessToken(clientId);
        
        ResponseEntity<String> listTokensResponse3 = buildTestRestTemplate(restTemplateBuilder, clientId, "secret")
                .exchange("http://localhost:" + portOauth + "/oauth/token/list?token=" + accessToken3, HttpMethod.GET, null, String.class);
        assertEquals(200, listTokensResponse3.getStatusCodeValue());
        
        ResponseEntity<String> revokeRefreshTokenResponse = buildTestRestTemplate(restTemplateBuilder, "fooClientIdPassword", "secret")
        		.exchange("http://localhost:" + portOauth + "/oauth/token?token=" + refreshToken + "&type=refresh_token", HttpMethod.DELETE, null, String.class);
        assertEquals(200, revokeRefreshTokenResponse.getStatusCodeValue());
        
        try {
            buildTestRestTemplate(restTemplateBuilder, clientId, "secret")
            .postForEntity("http://localhost:" + portOauth + "/oauth/token?client_id=fooClientIdPassword" + "&grant_type=refresh_token&refresh_token=" + refreshToken, null, String.class);
            fail("Expected fail");
        }
        catch (Exception ex) {
            assertTrue("Expected exception",ex instanceof RestClientResponseException);
            assertTrue("Expected 400", ((RestClientResponseException) ex).getRawStatusCode() == 400);
        }
        
        /*
        ResponseEntity<String> listTokensResponse2 = buildTestRestTemplate(restTemplateBuilder, accessToken4)
        		.getForEntity("http://localhost:" + portOauth + "/tokens", null, String.class);
        assertEquals(200, listTokensResponse2.getStatusCodeValue());
        assertEquals(401, listTokensResponse2.getStatusCode());*/
    }
}