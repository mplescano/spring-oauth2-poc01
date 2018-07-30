package com.mplescano.oauth.poc.poc01;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class TokenRevocationLiveTest extends Oauth2SupportTest {
    
    private String refreshToken;
 
    private int portApi = 8090;
    
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

    private void authorizeClient(String clientId) {
        ResponseEntity<String> response = buildTestRestTemplate(restTemplateBuilder, clientId, "secret")
        		.postForEntity("http://localhost:" + portOauth + "/oauth/authorize?client_id=" + clientId + "&response_type=code&scope=read,write", null, String.class);
        String responseText = response.getBody();
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void givenUser_whenRevokeRefreshToken_thenRefreshTokenInvalidError() throws Exception {
        //String accessToken1 = obtainAccessToken("fooClientIdPassword", "user1", "pass");
        String accessToken2 = obtainAccessToken("fooClientIdPassword", "tom", "111");
        //authorizeClient("fooClientIdPassword");

        String accessToken3 = obtainRefreshedAccessToken("fooClientIdPassword");
        //authorizeClient("fooClientIdPassword");
        ResponseEntity<String> listTokensResponse = buildTestRestTemplate(restTemplateBuilder, "tom", "111")
        		.exchange("http://localhost:" + portOauth + "/oauth/token/list", HttpMethod.GET, null, String.class);
        assertEquals(200, listTokensResponse.getStatusCodeValue());
        
        /*
        ResponseEntity<String> revokeRefreshTokenResponse = buildTestRestTemplate(restTemplateBuilder, "fooClientIdPassword", "secret")
        		.exchange("http://localhost:" + portOauth + "/oauth/token/revoke-refresh?token=" + refreshToken, HttpMethod.DELETE, null, String.class);
        assertEquals(200, revokeRefreshTokenResponse.getStatusCodeValue());
         
        String accessToken4 = obtainRefreshedAccessToken("fooClientIdPassword");
        //authorizeClient("fooClientIdPassword");
        
        ResponseEntity<String> listTokensResponse2 = buildTestRestTemplate(restTemplateBuilder, accessToken4)
        		.getForEntity("http://localhost:" + portOauth + "/tokens", null, String.class);
        assertEquals(200, listTokensResponse2.getStatusCodeValue());
        assertEquals(401, listTokensResponse2.getStatusCode());*/
    }
}