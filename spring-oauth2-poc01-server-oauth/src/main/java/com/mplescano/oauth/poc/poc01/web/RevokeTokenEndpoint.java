package com.mplescano.oauth.poc.poc01.web;

import java.util.Collections;
import java.util.Enumeration;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.security.oauth2.provider.endpoint.FrameworkEndpoint;
import org.springframework.security.oauth2.provider.token.ConsumerTokenServices;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@FrameworkEndpoint
public class RevokeTokenEndpoint {

    @Resource(name = "tokenServices")
    ConsumerTokenServices tokenServices;

    @RequestMapping(method = RequestMethod.DELETE, value = "/oauth/token")
    @ResponseBody
    public void revokeToken(HttpServletRequest request) {
        Enumeration<String> authorizations = request.getHeaders("Authorization");
        for (String rawAuthorization : Collections.list(authorizations)) {
        	String bearerAuthorization = null;
            if (rawAuthorization != null && rawAuthorization.contains(",") && rawAuthorization.contains("Bearer")) {
            	String[] arrAuthorization = rawAuthorization.split("\\,");
            	for (String splittedAuth : arrAuthorization) {
            		if (splittedAuth.startsWith("Bearer")) {
                		bearerAuthorization = splittedAuth;
            		}
				}
            }
            else if (rawAuthorization != null && !rawAuthorization.contains(",") && rawAuthorization.contains("Bearer")) {
            	bearerAuthorization = rawAuthorization;
            }
            if (bearerAuthorization != null) {
                String tokenId = bearerAuthorization.substring("Bearer".length() + 1);
                tokenServices.revokeToken(tokenId);
            }
		}
    }
}