package com.mplescano.oauth.poc.poc01.web;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/*import java.util.Collections;
import java.util.Enumeration;
*/
import javax.annotation.Resource;
//import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.endpoint.FrameworkEndpoint;
import org.springframework.security.oauth2.provider.token.ConsumerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@FrameworkEndpoint
public class RevokeTokenEndpoint {

    @Resource(name = "tokenServices")
    ConsumerTokenServices tokenServices;
    
    @Resource(name = "tokenStore")
    TokenStore tokenStore;

    @RequestMapping(method = RequestMethod.DELETE, value = "/oauth/token")
    @ResponseBody
    public String revokeToken(@RequestParam("token") String token, @RequestParam("type") String tokenType) {
        if ("access_token".equals(tokenType)) {
            //TODO only the access token
            tokenServices.revokeToken(token);
        }
        else if ("refresh_token".equals(tokenType)) {
            OAuth2RefreshToken oauth2RefreshToken = tokenStore.readRefreshToken(token);
            tokenStore.removeRefreshToken(oauth2RefreshToken);
        }
        else if ("all".equals(tokenType)) {
            //TODO
        }

    	return token;
    }
    
    /*@RequestMapping(method = RequestMethod.DELETE, value = "/oauth/token/revoke-refresh")
    @ResponseBody
    public String revokeRefreshToken(@RequestParam("token") String refreshToken) {
    	OAuth2RefreshToken oauth2RefreshToken = tokenStore.readRefreshToken(refreshToken);
    	tokenStore.removeRefreshToken(oauth2RefreshToken);
    	return refreshToken;
    }*/
    
    @RequestMapping(method = RequestMethod.GET, value = "/oauth/token/list")
    @ResponseBody
    public List<String> getTokens(Principal principal) {
    	String clientId = getClientId(principal);
        List<String> tokenValues = new ArrayList<>();
        Collection<OAuth2AccessToken> tokens = tokenStore.findTokensByClientId(clientId);//"sampleClientId"
        if (tokens != null) {
            for (OAuth2AccessToken token : tokens) {
                tokenValues.add(token.getValue());
            }
        }
        return tokenValues;
    }
    
	protected String getClientId(Principal principal) {
		Authentication client = (Authentication) principal;
		if (!client.isAuthenticated()) {
			throw new InsufficientAuthenticationException("The client is not authenticated.");
		}
		String clientId = client.getName();
		if (client instanceof OAuth2Authentication) {
			// Might be a client and user combined authentication
			clientId = ((OAuth2Authentication) client).getOAuth2Request().getClientId();
		}
		return clientId;
	}
}