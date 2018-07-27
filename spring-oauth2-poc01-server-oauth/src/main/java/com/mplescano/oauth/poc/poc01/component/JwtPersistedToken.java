package com.mplescano.oauth.poc.poc01.component;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.util.Assert;

public class JwtPersistedToken implements TokenStore {

	private static final Log LOG = LogFactory.getLog(JwtPersistedToken.class);

	private static final String DEFAULT_ACCESS_TOKEN_AUTHENTICATION_SELECT_STATEMENT = "select token_id, authentication from oauth_access_token where token_id = ?";

	private static final String DEFAULT_ACCESS_TOKEN_SELECT_STATEMENT = "select token_id, token from oauth_access_token where token_id = ?";

	private static final String DEFAULT_ACCESS_TOKEN_DELETE_STATEMENT = "delete from oauth_access_token where token_id = ?";

	private static final String DEFAULT_ACCESS_TOKEN_INSERT_STATEMENT = "insert into oauth_access_token (token_id, authentication_id, user_name, client_id, refresh_token) values (?, ?, ?, ?, ?)";

	private static final String DEFAULT_REFRESH_TOKEN_INSERT_STATEMENT = "insert into oauth_refresh_token (token_id) values (?)";

	private static final String DEFAULT_REFRESH_TOKEN_SELECT_STATEMENT = "select token_id, token from oauth_refresh_token where token_id = ?";

	private static final String DEFAULT_REFRESH_TOKEN_AUTHENTICATION_SELECT_STATEMENT = "select token_id, authentication from oauth_refresh_token where token_id = ?";

	private static final String DEFAULT_REFRESH_TOKEN_DELETE_STATEMENT = "delete from oauth_refresh_token where token_id = ?";

	private static final String DEFAULT_ACCESS_TOKEN_DELETE_FROM_REFRESH_TOKEN_STATEMENT = "delete from oauth_access_token where refresh_token = ?";

	private static final String DEFAULT_ACCESS_TOKEN_FROM_AUTHENTICATION_SELECT_STATEMENT = "select token_id, token from oauth_access_token where authentication_id = ?";

	private String selectAccessTokenAuthenticationSql = DEFAULT_ACCESS_TOKEN_AUTHENTICATION_SELECT_STATEMENT;

	private String deleteAccessTokenSql = DEFAULT_ACCESS_TOKEN_DELETE_STATEMENT;

	private String insertAccessTokenSql = DEFAULT_ACCESS_TOKEN_INSERT_STATEMENT;

	private String selectAccessTokenSql = DEFAULT_ACCESS_TOKEN_SELECT_STATEMENT;

	private String insertRefreshTokenSql = DEFAULT_REFRESH_TOKEN_INSERT_STATEMENT;

	private String selectRefreshTokenSql = DEFAULT_REFRESH_TOKEN_SELECT_STATEMENT;

	private String selectRefreshTokenAuthenticationSql = DEFAULT_REFRESH_TOKEN_AUTHENTICATION_SELECT_STATEMENT;

	private String deleteRefreshTokenSql = DEFAULT_REFRESH_TOKEN_DELETE_STATEMENT;

	private String deleteAccessTokenFromRefreshTokenSql = DEFAULT_ACCESS_TOKEN_DELETE_FROM_REFRESH_TOKEN_STATEMENT;

	private String selectAccessTokenFromAuthenticationSql = DEFAULT_ACCESS_TOKEN_FROM_AUTHENTICATION_SELECT_STATEMENT;

	private final JwtTokenStore jwtTokenStore;

	private final JdbcTemplate jdbcTemplate;

	private AuthenticationKeyGenerator authenticationKeyGenerator = new DefaultAuthenticationKeyGenerator();

	public JwtPersistedToken(JwtTokenStore jwtTokenStore, DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource required");
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.jwtTokenStore = jwtTokenStore;
	}

	@Override
	public OAuth2Authentication readAuthentication(OAuth2AccessToken token) {
		// expected only one otherwise exception
		jdbcTemplate.queryForObject(selectAccessTokenAuthenticationSql,
				new RowMapper<Long>() {
					public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
						return rs.getLong(1);
					}
				}, extractTokenKey(token.getValue()));
		return jwtTokenStore.readAuthentication(token);
	}

	@Override
	public OAuth2Authentication readAuthentication(String token) {
		// expected only one otherwise exception
		jdbcTemplate.queryForObject(selectAccessTokenAuthenticationSql,
				new RowMapper<Long>() {
					public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
						return rs.getLong(1);
					}
				}, extractTokenKey(token));

		return jwtTokenStore.readAuthentication(token);
	}

	@Override
	public void storeAccessToken(OAuth2AccessToken token,
			OAuth2Authentication authentication) {
		String refreshToken = null;
		if (token.getRefreshToken() != null) {
			refreshToken = token.getRefreshToken().getValue();
		}

		if (readAccessToken(token.getValue()) != null) {
			removeAccessToken(token.getValue());
		}

		jdbcTemplate.update(insertAccessTokenSql,
				// token_id, token, authentication_id, user_name, client_id,
				// authentication, refresh_token
				new Object[] { extractTokenKey(token.getValue()),
						authenticationKeyGenerator.extractKey(authentication),
						authentication.isClientOnly() ? null : authentication.getName(),
						authentication.getOAuth2Request().getClientId(),
						extractTokenKey(refreshToken) },
				new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
						Types.VARCHAR });
	}

	@Override
	public OAuth2AccessToken readAccessToken(String tokenValue) {
		// expected only one otherwise exception
		jdbcTemplate.queryForObject(selectAccessTokenSql, new RowMapper<Long>() {
			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getLong(1);
			}
		}, extractTokenKey(tokenValue));
		return jwtTokenStore.readAccessToken(tokenValue);
	}

	public void removeAccessToken(OAuth2AccessToken token) {
		removeAccessToken(token.getValue());
	}

	private void removeAccessToken(String tokenValue) {
		jdbcTemplate.update(deleteAccessTokenSql, extractTokenKey(tokenValue));
	}

	@Override
	public void storeRefreshToken(OAuth2RefreshToken refreshToken,
			OAuth2Authentication authentication) {
		jdbcTemplate.update(insertRefreshTokenSql,
				new Object[] { extractTokenKey(refreshToken.getValue()), },
				new int[] { Types.VARCHAR });
	}

	@Override
	public OAuth2RefreshToken readRefreshToken(String tokenValue) {
		jdbcTemplate.queryForObject(selectRefreshTokenSql, new RowMapper<Long>() {
			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getLong(1);
			}
		}, extractTokenKey(tokenValue));
		return jwtTokenStore.readRefreshToken(tokenValue);
	}

	@Override
	public OAuth2Authentication readAuthenticationForRefreshToken(
			OAuth2RefreshToken token) {
		jdbcTemplate.queryForObject(selectRefreshTokenAuthenticationSql,
				new RowMapper<Long>() {
					public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
						return rs.getLong(1);
					}
				}, extractTokenKey(token.getValue()));
		return jwtTokenStore.readAuthenticationForRefreshToken(token);
	}

	@Override
	public void removeRefreshToken(OAuth2RefreshToken token) {
		jdbcTemplate.update(deleteRefreshTokenSql, extractTokenKey(token.getValue()));
		jwtTokenStore.removeRefreshToken(token);
	}

	@Override
	public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
		jdbcTemplate.update(deleteAccessTokenFromRefreshTokenSql,
				new Object[] { extractTokenKey(refreshToken.getValue()) },
				new int[] { Types.VARCHAR });
		jwtTokenStore.removeAccessTokenUsingRefreshToken(refreshToken);
	}

	@Override
	public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
		jdbcTemplate.queryForObject(selectAccessTokenFromAuthenticationSql,
				new RowMapper<Long>() {
					public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
						return rs.getLong(1);
					}
				}, authenticationKeyGenerator.extractKey(authentication));
		return jwtTokenStore.getAccessToken(authentication);
	}

	@Override
	public Collection<OAuth2AccessToken> findTokensByClientIdAndUserName(String clientId,
			String userName) {
		return jwtTokenStore.findTokensByClientIdAndUserName(clientId, userName);
	}

	@Override
	public Collection<OAuth2AccessToken> findTokensByClientId(String clientId) {
		return null;
	}

	protected String extractTokenKey(String value) {
		if (value == null) {
			return null;
		}
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(
					"SHA-256 algorithm not available.  Fatal (should be in the JDK).");
		}

		try {
			byte[] bytes = digest.digest(value.getBytes("UTF-8"));
			return String.format("%032x", new BigInteger(1, bytes));
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(
					"UTF-8 encoding not available.  Fatal (should be in the JDK).");
		}
	}

	public static class DefaultAuthenticationKeyGenerator
			implements AuthenticationKeyGenerator {

		private static final String CLIENT_ID = "client_id";

		private static final String SCOPE = "scope";

		private static final String USERNAME = "username";

		public String extractKey(OAuth2Authentication authentication) {
			Map<String, String> values = new LinkedHashMap<String, String>();
			OAuth2Request authorizationRequest = authentication.getOAuth2Request();
			if (!authentication.isClientOnly()) {
				values.put(USERNAME, authentication.getName());
			}
			values.put(CLIENT_ID, authorizationRequest.getClientId());
			if (authorizationRequest.getScope() != null) {
				values.put(SCOPE, OAuth2Utils.formatParameterList(
						new TreeSet<String>(authorizationRequest.getScope())));
			}
			return generateKey(values);
		}

		protected String generateKey(Map<String, String> values) {
			MessageDigest digest;
			try {
				digest = MessageDigest.getInstance("SHA-256");
				byte[] bytes = digest.digest(values.toString().getBytes("UTF-8"));
				return String.format("%032x", new BigInteger(1, bytes));
			}
			catch (NoSuchAlgorithmException nsae) {
				throw new IllegalStateException(
						"MD5 algorithm not available.  Fatal (should be in the JDK).",
						nsae);
			}
			catch (UnsupportedEncodingException uee) {
				throw new IllegalStateException(
						"UTF-8 encoding not available.  Fatal (should be in the JDK).",
						uee);
			}
		}
	}
}
