package com.mplescano.oauth.poc.poc01;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jose4j.json.JsonUtil;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@RunWith(SpringRunner.class)
@Import(Oauth2SupportTest.AdditionalConfiguration.class)
public class Oauth2SupportTest {

    @Autowired
    protected TestRestTemplate restTemplate;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected RestTemplateBuilder restTemplateBuilder;
    
    protected JwtConsumer jwtConsumer;

    protected static final Logger logger = LoggerFactory.getLogger(Oauth2SupportTest.class);

    @Before
    public void setup() {
       jwtConsumer = new JwtConsumerBuilder().setSkipAllValidators().setDisableRequireSignature().setSkipSignatureVerification()
          .build();
    }

    protected void logJWTClaims(JwtContext jwtContext) throws Exception {
       logger.info(prettyPrintJson(JsonUtil.toJson(jwtContext.getJwtClaims().getClaimsMap())));
    }
    
    protected void logJson(String json) throws Exception {
        logger.info(prettyPrintJson(json));
     }
    
    protected String prettyPrintJson(String flatJson) throws Exception {
		return (new JSONObject(flatJson).toString(3));
     }
    
    TestRestTemplate buildTestRestTemplate(RestTemplateBuilder builder, String username, String password) {
    	TestRestTemplate result = new TestRestTemplate(builder);
		List<ClientHttpRequestInterceptor> interceptors = result.getRestTemplate().getInterceptors();
		if (interceptors == null) {
			interceptors = Collections.emptyList();
		}
		interceptors = new ArrayList<ClientHttpRequestInterceptor>(interceptors);
		Iterator<ClientHttpRequestInterceptor> iterator = interceptors.iterator();
		while (iterator.hasNext()) {
			if (iterator.next() instanceof BasicAuthorizationInterceptor) {
				iterator.remove();
			}
		}
		interceptors.add(0, new BasicAuthorizationInterceptor(username, password));
		result.getRestTemplate().setInterceptors(interceptors);
    	result.getRestTemplate().setErrorHandler(new CustomErrorHandler());
    	return result;
    }
    
    TestRestTemplate buildTestRestTemplate(RestTemplateBuilder builder) {
    	TestRestTemplate result = new TestRestTemplate(builder);
    	result.getRestTemplate().setErrorHandler(new CustomErrorHandler());
    	return result;
    }
    
    @TestConfiguration
    public static class AdditionalConfiguration {
    	
		@Bean
    	ObjectMapper objectMapper() {
    		ObjectMapper mapper = new ObjectMapper();
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    		return mapper;
    	}
    	
        @Bean
        @Primary
        RestTemplateBuilder restTemplateBuilder(ObjectMapper objectMapper) {
        	Logger log = LoggerFactory.getLogger(this.getClass());
        	log.debug("========== resteTemplateBuilder");
        	List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
        		StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
        		stringConverter.setWriteAcceptCharset(false);

        		messageConverters.add(new ByteArrayHttpMessageConverter());
        		messageConverters.add(stringConverter);
        		messageConverters.add(new ResourceHttpMessageConverter());
        		messageConverters.add(new SourceHttpMessageConverter<>());
        		messageConverters.add(new AllEncompassingFormHttpMessageConverter());
        		messageConverters.add(new MappingJackson2HttpMessageConverter(objectMapper));
        		messageConverters.add(new FormHttpMessageConverter());
        				
        	RestTemplateBuilder builder = new RestTemplateBuilder();
        	builder = builder.requestFactory(() -> new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory() {{setOutputStreaming(false);}}))
        			.interceptors(new LoggingRequestInterceptor()).messageConverters(messageConverters).errorHandler(new CustomErrorHandler());
        	return builder;
        }
        
        @Bean
        @Primary
        TestRestTemplate restTemplate(RestTemplateBuilder builder) {
        	TestRestTemplate result = new TestRestTemplate(builder);
        	result.getRestTemplate().setErrorHandler(new CustomErrorHandler());
        	return result;
        }
    }
}
