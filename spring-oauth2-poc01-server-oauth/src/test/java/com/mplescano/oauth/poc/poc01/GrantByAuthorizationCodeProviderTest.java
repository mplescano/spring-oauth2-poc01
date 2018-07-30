package com.mplescano.oauth.poc.poc01;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jose4j.jwt.consumer.JwtContext;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GrantByAuthorizationCodeProviderTest extends Oauth2SupportTest {
	
    private int portApi = 8090;
    
    private int portOauth = 8080;

    private static final String CLIENT_ID_01 = "my-normal-app";
    
    @Test
    public void getJwtTokenByAuthorizationCode() throws Exception {
        String userName = "bob";
        String password = "abc123";
        String location = null;
        String query = null;
        HttpHeaders headers = new HttpHeaders();

        String redirectUrl = "http://localhost:" + portApi + "/resources/user";
        ResponseEntity<String> response = buildTestRestTemplate(restTemplateBuilder, userName, password).postForEntity("http://localhost:" + portOauth + 
        		"/oauth/authorize?response_type=code&client_id=" + CLIENT_ID_01 + "&redirect_uri={redirectUrl}", null, String.class, redirectUrl);
        if (HttpStatus.OK == response.getStatusCode()) {
            Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
            List<String> setCookie = response.getHeaders().get("Set-Cookie");
            String jSessionIdCookie = setCookie.get(0);
            String cookieValue = jSessionIdCookie.split(";")[0];

            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.add("Cookie", cookieValue);
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("scope.read", "true");
            body.add("scope.write", "true");
            body.add("user_oauth_approval", "true");
            body.add("authorize", "Authorize");
            response = buildTestRestTemplate(restTemplateBuilder, userName, password).postForEntity("http://localhost:" + portOauth +
                    "/oauth/authorize?response_type=code&client_id=" + CLIENT_ID_01 + "&redirect_uri={redirectUrl}", 
                    new HttpEntity<MultiValueMap<String, String>>(body, headers), String.class, redirectUrl);
            Assert.assertEquals(HttpStatus.FOUND, response.getStatusCode());
            Assert.assertNull(response.getBody());
            location = response.getHeaders().get("Location").get(0);
            URI locationURI = new URI(location);
            query = locationURI.getQuery();
        }
        else if (HttpStatus.FOUND == response.getStatusCode()) {
            location = response.getHeaders().get("Location").get(0);
            URI locationURI = new URI(location);
            query = locationURI.getQuery();
        }
        else {
            Assert.fail("Status uknown:" + response.getStatusCode());
        }

        location = "http://localhost:" + portOauth + "/oauth/token?" + query + "&grant_type=authorization_code&client_id=" + CLIENT_ID_01 + 
        		"&redirect_uri={redirectUrl}";

        response = buildTestRestTemplate(restTemplateBuilder, CLIENT_ID_01, "").postForEntity(location, new HttpEntity<>(new HttpHeaders()), String.class, redirectUrl);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

        HashMap jwtMap = new ObjectMapper().readValue(response.getBody(), HashMap.class);
        String accessToken = (String) jwtMap.get("access_token");

        JwtContext jwtContext = jwtConsumer.process(accessToken);
        logJWTClaims(jwtContext);
        Assert.assertEquals(userName, jwtContext.getJwtClaims().getClaimValue("user_name"));

        headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        int statusCode = 0;
        try {
            response = buildTestRestTemplate(restTemplateBuilder).exchange("http://localhost:" + portApi + "/resources/client", HttpMethod.GET, new HttpEntity<>(null, headers), String.class);			
		}
		catch (RestClientResponseException ex) {
			statusCode = ex.getRawStatusCode();
		}
        Assert.assertEquals(HttpStatus.FORBIDDEN.value(), statusCode);

        response = buildTestRestTemplate(restTemplateBuilder).exchange("http://localhost:" + portApi + "/resources/user", HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

        response = buildTestRestTemplate(restTemplateBuilder).exchange("http://localhost:" + portApi + "/resources/principal", HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
        Assert.assertEquals(userName, response.getBody());

        response = buildTestRestTemplate(restTemplateBuilder).exchange("http://localhost:" + portApi + "/resources/roles", HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
        Assert.assertTrue(response.getBody().contains("{\"authority\":\"ROLE_USER\"}"));
    }
}