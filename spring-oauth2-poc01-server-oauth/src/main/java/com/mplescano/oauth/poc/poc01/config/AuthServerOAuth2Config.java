package com.mplescano.oauth.poc.poc01.config;

import java.util.Arrays;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.FixedDefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.FixedJwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

@Configuration
@EnableAuthorizationServer
public class AuthServerOAuth2Config extends AuthorizationServerConfigurerAdapter {

    private final DataSource dataSource;
    
    private final AuthenticationManager authenticationManager;
    
    public AuthServerOAuth2Config(DataSource dataSource, 
                                  @Qualifier("authenticationManagerBean") AuthenticationManager authenticationManager) {
        this.dataSource = dataSource;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
        oauthServer
            .tokenKeyAccess("permitAll()")//Default is empty, which is interpreted as "denyAll()" (no access). /oauth/token_key (exposes public key for token verification if using JWT tokens).
            .checkTokenAccess("isAuthenticated()")//Default is empty, which is interpreted as "denyAll()" (no access). /oauth/check_token used by Resource Servers to decode access tokens
            ;
            /*.and().authorizeRequests()
            .antMatchers("/oauth/token/list").fullyAuthenticated()
            .and()
            .requestMatchers()
                .antMatchers("/oauth/token/list")*/;
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.jdbc(dataSource);
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
    	final TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
    	tokenEnhancerChain.setTokenEnhancers(Arrays.asList(tokenEnhancer(), accessTokenConverter()));
    	
        endpoints
        	.tokenServices(tokenServices())
        	.tokenStore(tokenStore())
            //.accessTokenConverter(accessTokenConverter())//if (accessTokenConverter() instanceof JwtAccessTokenConverter) then tokenStore = new JwtTokenStore(accessTokenConverter());
        	.authenticationManager(authenticationManager)
        	.tokenEnhancer(tokenEnhancerChain)//by default tokenEnhancer = accessTokenConverter
        	.pathMapping("/oauth/token", "/oauth/token/**")
        	;
    }

    /*
     * it's redundant since only is required to define accessTokenConverter() and set it up in endpoints.accessTokenConverter
     * if you want to implement a custom tokenstore then you have to define it as a bean*/  
    //@Bean
    //public TokenStore tokenStore() {
    //	return new JwtPersistedToken(new JwtTokenStore(accessTokenConverter()), dataSource);
    //}
    
    @Bean
    public TokenStore tokenStore() {
        return new JdbcTokenStore(dataSource);
    }
    
	@Bean
	public TokenEnhancer accessTokenConverter() {
		final FixedJwtAccessTokenConverter converter = new FixedJwtAccessTokenConverter();
		converter.setSigningKey("123");
		/*final KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(new ClassPathResource("mytest.jks"),
				"mypass".toCharArray());
		converter.setKeyPair(keyStoreKeyFactory.getKeyPair("mytest"));*/
		return converter;
	}
    
    /**/
    @Bean
    @Primary
    public FixedDefaultTokenServices tokenServices() {
    	final TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
    	tokenEnhancerChain.setTokenEnhancers(Arrays.asList(tokenEnhancer(), accessTokenConverter()));
        final FixedDefaultTokenServices defaultTokenServices = new FixedDefaultTokenServices();
        defaultTokenServices.setTokenStore(tokenStore());
        defaultTokenServices.setSupportRefreshToken(true);
        defaultTokenServices.setReuseRefreshToken(true);
        defaultTokenServices.setTokenEnhancer(tokenEnhancerChain);
        return defaultTokenServices;
    }
    
    @Bean
    public TokenEnhancer tokenEnhancer() {
        return new CustomTokenEnhancer();
    }
}