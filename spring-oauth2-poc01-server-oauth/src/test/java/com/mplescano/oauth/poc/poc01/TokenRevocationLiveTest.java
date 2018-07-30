package com.mplescano.oauth.poc.poc01;

import java.util.HashMap;
import java.util.Map;


public class TokenRevocationLiveTest extends Oauth2SupportTest {
    
    private String refreshToken;
 
    private String obtainAccessToken(String clientId, String username, String password) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("grant_type", "password");
        params.put("client_id", clientId);
        params.put("username", username);
        params.put("password", password);
         
        Response response = RestAssured.given().auth().
          preemptive().basic(clientId,"secret").and().with().params(params).
          when().post("http://localhost:8081/spring-security-oauth-server/oauth/token");
        
        
        
        refreshToken = response.jsonPath().getString("refresh_token");
         
        return response.jsonPath().getString("access_token");
    }
     
    private String obtainRefreshToken(String clientId) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("grant_type", "refresh_token");
        params.put("client_id", clientId);
        params.put("refresh_token", refreshToken);
         
        Response response = RestAssured.given().auth()
          .preemptive().basic(clientId,"secret").and().with().params(params)
          .when().post("http://localhost:8081/spring-security-oauth-server/oauth/token");
         
        return response.jsonPath().getString("access_token");
    }
     
    private void authorizeClient(String clientId) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("response_type", "code");
        params.put("client_id", clientId);
        params.put("scope", "read,write");
         
        Response response = RestAssured.given().auth().preemptive()
          .basic(clientId,"secret").and().with().params(params).
          when().post("http://localhost:8081/spring-security-oauth-server/oauth/authorize");
    }
     
    @Test
    public void givenUser_whenRevokeRefreshToken_thenRefreshTokenInvalidError() {
        String accessToken1 = obtainAccessToken("fooClientIdPassword", "john", "123");
        String accessToken2 = obtainAccessToken("fooClientIdPassword", "tom", "111");
        authorizeClient("fooClientIdPassword");
         
        String accessToken3 = obtainRefreshToken("fooClientIdPassword");
        authorizeClient("fooClientIdPassword");
        Response refreshTokenResponse = RestAssured.given().
          header("Authorization", "Bearer " + accessToken3)
          .get("http://localhost:8082/spring-security-oauth-resource/tokens");
        assertEquals(200, refreshTokenResponse.getStatusCode());
         
        Response revokeRefreshTokenResponse = RestAssured.given()
          .header("Authorization", "Bearer " + accessToken1)
          .post("http://localhost:8082/spring-security-oauth-resource/tokens/revokeRefreshToken/"+refreshToken);
        assertEquals(200, revokeRefreshTokenResponse.getStatusCode());
         
        String accessToken4 = obtainRefreshToken("fooClientIdPassword");
        authorizeClient("fooClientIdPassword");
        Response refreshTokenResponse2 = RestAssured.given()
          .header("Authorization", "Bearer " + accessToken4)
          .get("http://localhost:8082/spring-security-oauth-resource/tokens");
        assertEquals(401, refreshTokenResponse2.getStatusCode());
    }
}