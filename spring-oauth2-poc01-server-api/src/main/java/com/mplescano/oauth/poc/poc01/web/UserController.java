package com.mplescano.oauth.poc.poc01.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class UserController {

    @Autowired
    private AuthorizationServerTokenServices tokenServices;
	
    @PreAuthorize("#oauth2.hasScope('read')")
    @RequestMapping(method = RequestMethod.GET, value = "/users/extra")
    @ResponseBody
    public Map<String, Object> getExtraInfo(Authentication auth) {
    	Map<String, Object> mpResults = new HashMap<>();
    	if (auth instanceof OAuth2Authentication) {
        	Map<String, Object> details = tokenServices.getAccessToken((OAuth2Authentication) auth).getAdditionalInformation();
            //System.out.println("User organization is " + details.get("organization"));
            mpResults.put("organization", details != null? details.get("organization") : null);
    	}
        mpResults.put("authorities", auth.getAuthorities());
        return mpResults;
    }
}