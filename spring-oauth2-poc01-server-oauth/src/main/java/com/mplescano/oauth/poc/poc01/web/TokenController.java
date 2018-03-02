package com.mplescano.oauth.poc.poc01.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TokenController {

    @Resource(name = "tokenStore")
    TokenStore tokenStore;
    
    @RequestMapping(method = RequestMethod.GET, value = "/tokens")
    @ResponseBody
    public List<String> getTokens() {
        List<String> tokenValues = new ArrayList<String>();
        Collection<OAuth2AccessToken> tokens = tokenStore.findTokensByClientId("sampleClientId");
        if (tokens != null) {
            for (OAuth2AccessToken token : tokens) {
                tokenValues.add(token.getValue());
            }
        }
        return tokenValues;
    }
}
