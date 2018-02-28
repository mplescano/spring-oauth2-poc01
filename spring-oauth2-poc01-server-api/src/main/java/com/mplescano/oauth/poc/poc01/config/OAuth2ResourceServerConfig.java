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
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

@Configuration
@EnableResourceServer
public class OAuth2ResourceServerConfig extends ResourceServerConfigurerAdapter {

  @Autowired
  private DataSource dataSource;

  //

  @Override
  public void configure(final HttpSecurity http) throws Exception {
      // @formatter:off
      http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
          .and()
          .authorizeRequests().anyRequest().permitAll();
  // @formatter:on        
  }

  @Override
  public void configure(final ResourceServerSecurityConfigurer config) {
      config.tokenServices(tokenServices());
  }

  @Bean
  @Primary
  public DefaultTokenServices tokenServices() {
      final DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
      defaultTokenServices.setTokenStore(tokenStore());
      return defaultTokenServices;
  }

  // JDBC token store configuration
  @Bean
  public TokenStore tokenStore() {
      return new JdbcTokenStore(dataSource);
  }

}