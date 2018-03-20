package com.mplescano.oauth.poc.poc01.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import com.mplescano.oauth.poc.poc01.service.JdbcUserServiceImpl;

@Configuration
@EnableResourceServer
public class OAuth2ResourceServerConfig extends ResourceServerConfigurerAdapter {

	@Autowired
	private DataSource dataSource;

	/**
	 * Appears in the response like this:
	 * WWW-Authenticate: Bearer realm="my_rest_api"
	 */
	private static final String RESOURCE_ID = "my_rest_api";

	@Override
	public void configure(final HttpSecurity http) throws Exception {
		// @formatter:off
      http
          .anonymous().disable()
          .sessionManagement()
          //.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS)//in order to avoid overwriting the cookie of oauth server
          .and()
          .authorizeRequests()
              //.antMatchers(HttpMethod.OPTIONS).permitAll()//Some browsers like Chrome like to send OPTIONS request to look for CORS before making AJAX call. Therefore, it is better to always allow OPTIONS requests.
              .anyRequest().authenticated()
          .and().exceptionHandling().accessDeniedHandler(new OAuth2AccessDeniedHandler());
  // @formatter:on        
	}

	@Override
	public void configure(final ResourceServerSecurityConfigurer config) {
		config.resourceId(RESOURCE_ID).tokenServices(tokenServices())
		// .stateless(false)//default true
		;
	}

	@Bean
	@Primary
	public DefaultTokenServices tokenServices() {
		final DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
		defaultTokenServices.setTokenStore(tokenStore());
		return defaultTokenServices;
	}

	// JWT token store configuration
	@Bean
	public TokenStore tokenStore() {
		return new JwtTokenStore(accessTokenConverter());
	}
	
    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setAccessTokenConverter(customAccessTokenConverter());
        converter.setSigningKey("123");
        /*
        final Resource resource = new ClassPathResource("public.txt");
        String publicKey = null;
        try {
        	publicKey = IOUtils.toString(resource.getInputStream());
        } catch (final IOException e) {
        	throw new RuntimeException(e);
        }
        converter.setVerifierKey(publicKey);*/
        return converter;
    }
	
	@Bean
	public JdbcUserServiceImpl userService() {
		JdbcUserServiceImpl userService = new JdbcUserServiceImpl();
		userService.setDataSource(dataSource);
		userService.setUsernameBasedPrimaryKey(false);
		return userService;
	}

	@Bean
	public CustomAccessTokenConverter customAccessTokenConverter() {
		return new CustomAccessTokenConverter();
	}
}