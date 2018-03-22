package com.mplescano.oauth.poc.poc01;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class GrantByImplicitProviderTest extends Oauth2SupportTest {

    private int portApi = 8090;
    
    private int portOauth = 8080;
    
    private static final String CLIENT_ID_01 = "my-normal-app";
    
    @Test
    public void getJwtTokenByImplicitGrant() throws Exception {
        String userName = "bob";//ROLE_USER
        String password = "abc123";
        String redirectUrl = "http://localhost:" + portApi + "/resources/user";
        ResponseEntity<String> response = new TestRestTemplate(userName, password)
        		.postForEntity("http://localhost:" + portOauth + "/oauth/authorize?response_type=token&client_id=" + CLIENT_ID_01 + "&redirect_uri={redirectUrl}", 
        				null, String.class,redirectUrl);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        List<String> setCookie = response.getHeaders().get("Set-Cookie");
        String jSessionIdCookie = setCookie.get(0);
        String cookieValue = jSessionIdCookie.split(";")[0];

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", cookieValue);
        response = new TestRestTemplate(userName, password)
        		.postForEntity("http://localhost:" + portOauth + "/oauth/authorize?response_type=token&client_id=" + CLIENT_ID_01 + "&redirect_uri={redirectUrl}&user_oauth_approval=true&authorize=Authorize",
                new HttpEntity<>(headers), String.class, redirectUrl);
        Assert.assertEquals(HttpStatus.FOUND, response.getStatusCode());
        Assert.assertNull(response.getBody());
        String location = response.getHeaders().get("Location").get(0);

        //FIXME: Is this a bug with redirect URL?
        location = location.replace("#", "?");

        response = new TestRestTemplate().getForEntity(location, String.class);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}