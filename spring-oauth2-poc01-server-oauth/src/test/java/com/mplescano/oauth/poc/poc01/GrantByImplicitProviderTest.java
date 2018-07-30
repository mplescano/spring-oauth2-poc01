package com.mplescano.oauth.poc.poc01;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class GrantByImplicitProviderTest extends Oauth2SupportTest {

    private int portApi = 8090;
    
    private int portOauth = 8080;
    
    private static final String CLIENT_ID_01 = "my-normal-app";
    
    @Test
    public void getJwtTokenByImplicitGrant() throws Exception {
        String userName = "bob";//ROLE_USER
        String password = "abc123";
        String redirectUrl = "http://localhost:" + portApi + "/resources/user";
        HttpHeaders authHeaders = new HttpHeaders();
        ResponseEntity<String> response = buildTestRestTemplate(restTemplateBuilder, userName, password)
        		.postForEntity("http://localhost:" + portOauth + "/oauth/authorize?response_type=token&client_id=" + CLIENT_ID_01 + "&redirect_uri={redirectUrl}", 
        		        new HttpEntity<>(authHeaders), String.class,redirectUrl);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        List<String> setCookie = response.getHeaders().get("Set-Cookie");
        String jSessionIdCookie = setCookie.get(0);
        String cookieValue = jSessionIdCookie.split(";")[0];

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Cookie", cookieValue);
        //Map<String, String> body = new HashMap<>();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.set("scope.read", "true");
        body.set("scope.write", "true");
        body.set("user_oauth_approval", "true");
        body.set("authorize", "Authorize");
        /*body.put("scope.read", "true");
        body.put("scope.write", "true");
        body.put("user_oauth_approval", "true");
        body.put("authorize", "Authorize");*/
        response = buildTestRestTemplate(restTemplateBuilder, userName, password)
        		.postForEntity("http://localhost:" + portOauth + "/oauth/authorize?response_type=token&client_id=" + CLIENT_ID_01 + "&redirect_uri={redirectUrl}",
        		        new HttpEntity<MultiValueMap<String, String>>(body, headers)
        		        /*new HttpEntity<Map<String, String>>(body, headers)*/, String.class, redirectUrl);
        //&user_oauth_approval=true&authorize=Authorize
        Assert.assertEquals(HttpStatus.FOUND, response.getStatusCode());
        Assert.assertNull(response.getBody());
        String location = response.getHeaders().get("Location").get(0);

        //FIXME: Is this a bug with redirect URL?
        location = location.replace("#", "?");

        response = buildTestRestTemplate(restTemplateBuilder).getForEntity(location, String.class);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}