package com.mplescano.oauth.poc.poc01.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mplescano.oauth.poc.poc01.service.JdbcUserServiceImpl;

@Configuration
@Order(SecurityProperties.BASIC_AUTH_ORDER - 2)
public class ServerSecurityConfig extends WebSecurityConfigurerAdapter {
 
    @Autowired
    private DataSource dataSource;
    
	@Autowired
	public void globalUserDetails(final AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService()).passwordEncoder(passwordEncoder());
    }
 
    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() 
      throws Exception {
        return super.authenticationManagerBean();
    }
 
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
        .anonymous().disable()
        .authorizeRequests()
            .antMatchers("/login").permitAll()
            .anyRequest().authenticated()
            .and().httpBasic()//allow authentication without the login form 
            .and()
            .formLogin().permitAll();
    }
    
    @Bean
    public JdbcUserServiceImpl userService() {
        JdbcUserServiceImpl userService = new JdbcUserServiceImpl();
        userService.setDataSource(dataSource);
        userService.setUsernameBasedPrimaryKey(false);
        return userService;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}
