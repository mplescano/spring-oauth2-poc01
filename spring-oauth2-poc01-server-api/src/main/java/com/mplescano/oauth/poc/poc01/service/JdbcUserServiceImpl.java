package com.mplescano.oauth.poc.poc01.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.mplescano.oauth.poc.poc01.model.entity.User;
import com.mplescano.oauth.poc.poc01.model.entity.UserIdDetails;

public class JdbcUserServiceImpl extends JdbcDaoSupport implements UserDetailsManager, MessageSourceAware, UserService {

	public static final String DEF_USERS_BY_USERNAME_QUERY = "select id,username,password,enabled,roles "
			+ "from users " + "where username = ?";
	
    public static final String DEF_USERS_BY_ID_QUERY = "select id,username,password,enabled,roles "
            + "from users " + "where id = ?";
	
	public static final String DEF_AUTHORITIES_BY_ROLENAME_QUERY = "select authority from authorities where role = ?";
	
	public static final String DEF_CREATE_USER_SQL = "insert into users (username, password, enabled, roles) values (?,?,?,?)";
	
	public static final String DEF_UPDATE_USER_SQL = "update users set password = ?, enabled = ?, roles = ? where username = ?";
	
	public static final String DEF_DELETE_USER_SQL = "delete from users where username = ?";
	
	public static final String DEF_CHANGE_PASSWORD_SQL = "update users set password = ? where username = ?";
	
	public static final String DEF_USER_EXISTS_SQL = "select username from users where username = ?";
	
	public static final String DEF_ALL_USERS_SQL = "select username from users";
	
	protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

	private boolean enableAuthorities = true;
	
	private String usersByUsernameQuery;
	
	private String usersByIdQuery;
	
	private String authoritiesByRolenameQuery;
	
	private String createUserSql = DEF_CREATE_USER_SQL;
	
	private String updateUserSql = DEF_UPDATE_USER_SQL;
	
	private String deleteUserSql = DEF_DELETE_USER_SQL;
	
	private String changePasswordSql = DEF_CHANGE_PASSWORD_SQL;
	
	private String userExistsSql = DEF_USER_EXISTS_SQL;
	
	private String allUserListSql = DEF_ALL_USERS_SQL;
	
	private boolean usernameBasedPrimaryKey = true;
	
	private AuthenticationManager authenticationManager;
	
	public JdbcUserServiceImpl() {
		this.usersByUsernameQuery = DEF_USERS_BY_USERNAME_QUERY;
		this.usersByIdQuery = DEF_USERS_BY_ID_QUERY;
		this.authoritiesByRolenameQuery = DEF_AUTHORITIES_BY_ROLENAME_QUERY;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		Assert.notNull(messageSource, "messageSource cannot be null");
		this.messages = new MessageSourceAccessor(messageSource);
	}

	@Override
	public UserIdDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		List<UserIdDetails> users = loadUsersByUsername(username);

		if (users.size() == 0) {
			this.logger.debug("Query returned no results for user '" + username + "'");

			throw new UsernameNotFoundException(
					this.messages.getMessage("JdbcDaoImpl.notFound",
							new Object[] { username }, "Username {0} not found"));
		}

		UserIdDetails user = users.get(0); // contains no GrantedAuthority[]

		Set<GrantedAuthority> dbAuthsSet = new HashSet<GrantedAuthority>();

		if (this.enableAuthorities) {
			dbAuthsSet.addAll(user.getAuthorities());
			for (GrantedAuthority grantedRoleName : user.getAuthorities()) {
				dbAuthsSet.addAll(loadUserAuthorities(grantedRoleName.getAuthority()));
			}
			
		}

		List<GrantedAuthority> dbAuths = new ArrayList<GrantedAuthority>(dbAuthsSet);

		addCustomAuthorities(user.getUsername(), dbAuths);

		if (dbAuths.size() == 0) {
			this.logger.debug("User '" + username
					+ "' has no authorities and will be treated as 'not found'");

			throw new UsernameNotFoundException(this.messages.getMessage(
					"JdbcDaoImpl.noAuthority", new Object[] { username },
					"User {0} has no GrantedAuthority"));
		}

		return createUserDetails(username, user, dbAuths);
	}

	/**
	 * Executes the SQL <tt>usersByUsernameQuery</tt> and returns a list of UserDetails
	 * objects. There should normally only be one matching user.
	 */
	protected List<UserIdDetails> loadUsersByUsername(String username) {
		return getJdbcTemplate().query(this.usersByUsernameQuery,
				new String[] { username }, rowMapperUser());
	}

    private RowMapper<UserIdDetails> rowMapperUser() {
        return new RowMapper<UserIdDetails>() {
        	@Override
        	public UserIdDetails mapRow(ResultSet rs, int rowNum)
        			throws SQLException {
        		Long id = rs.getLong(1);
        		String username = rs.getString(2);
        		String password = rs.getString(3);
        		boolean enabled = rs.getBoolean(4);
        		String roles = rs.getString(5);
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (roles != null) {
                    String[] arrRoles = roles.split("\\,");
                    for (String roleName : arrRoles) {
                        authorities.add(new SimpleGrantedAuthority(roleName));
                    }
                }
        		return new User(id, username, password, enabled, true, true, true, authorities);
        	}
        };
    }
	
	/**
	 * Loads authorities by executing the SQL from <tt>authoritiesByUsernameQuery</tt>.
	 *
	 * @return a list of GrantedAuthority objects for the user
	 */
	protected List<GrantedAuthority> loadUserAuthorities(String roleName) {
		return getJdbcTemplate().query(this.authoritiesByRolenameQuery,
				new String[] { roleName }, new RowMapper<GrantedAuthority>() {
					@Override
					public GrantedAuthority mapRow(ResultSet rs, int rowNum) throws SQLException {
						String authority = rs.getString(1);
						return new SimpleGrantedAuthority(authority);
					}
				});
	}
	
	/**
	 * Can be overridden to customize the creation of the final UserDetailsObject which is
	 * returned by the <tt>loadUserByUsername</tt> method.
	 *
	 * @param username the name originally passed to loadUserByUsername
	 * @param userFromUserQuery the object returned from the execution of the
	 * @param combinedAuthorities the combined array of authorities from all the authority
	 * loading queries.
	 * @return the final UserDetails which should be used in the system.
	 */
	protected UserIdDetails createUserDetails(String username, UserIdDetails userFromUserQuery, List<GrantedAuthority> combinedAuthorities) {
		String returnUsername = userFromUserQuery.getUsername();

		if (!this.usernameBasedPrimaryKey) {
			returnUsername = username;
		}

		return new User(userFromUserQuery.getId(), returnUsername, userFromUserQuery.getPassword(),
				userFromUserQuery.isEnabled(), true, true, true, combinedAuthorities);
	}
	
	/**
	 * Allows subclasses to add their own granted authorities to the list to be returned
	 * in the <tt>UserDetails</tt>.
	 *
	 * @param username the username, for use by finder methods
	 * @param authorities the current granted authorities, as populated from the
	 * <code>authoritiesByUsername</code> mapping
	 */
	protected void addCustomAuthorities(String username, List<GrantedAuthority> authorities) {
	}
	
	@Override
	public void createUser(final UserDetails user) {
		validateUserDetails(user);
		StringBuilder stbRoles = new StringBuilder();
		for (GrantedAuthority authority : user.getAuthorities()) {
			if (authority.getAuthority().startsWith("ROLE_")) {
				stbRoles.append(authority.getAuthority()).append(",");
			}
		}
		final String roles = StringUtils.trimTrailingCharacter(stbRoles.toString(), ',');
		getJdbcTemplate().update(createUserSql, new PreparedStatementSetter() {
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setString(1, user.getUsername());
				ps.setString(2, user.getPassword());
				ps.setBoolean(3, user.isEnabled());
				ps.setString(4, roles);
			}
		});

		if (isEnableAuthorities()) {
			insertUserAuthorities(user);
		}
	}

	private void insertUserAuthorities(UserDetails user) {
		for (GrantedAuthority auth : user.getAuthorities()) {
			if (!auth.getAuthority().startsWith("ROLE_")) {
				//TODO ensure the authority belongs to that role
			}
		}
	}
	
	@Override
	public void updateUser(final UserDetails user) {
		validateUserDetails(user);
		StringBuilder stbRoles = new StringBuilder();
		for (GrantedAuthority authority : user.getAuthorities()) {
			if (authority.getAuthority().startsWith("ROLE_")) {
				stbRoles.append(authority.getAuthority()).append(",");
			}
		}
		final String roles = StringUtils.trimTrailingCharacter(stbRoles.toString(), ',');
		getJdbcTemplate().update(updateUserSql, new PreparedStatementSetter() {
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setString(1, user.getPassword());
				ps.setBoolean(2, user.isEnabled());
				ps.setString(3, roles);
				ps.setString(4, user.getUsername());
			}
		});

		if (isEnableAuthorities()) {
			insertUserAuthorities(user);
		}
	}

	@Override
	public void deleteUser(String username) {
		getJdbcTemplate().update(deleteUserSql, username);
	}

	@Override
	public void changePassword(String oldPassword, String newPassword) {
		Authentication currentUser = SecurityContextHolder.getContext()
				.getAuthentication();

		if (currentUser == null) {
			// This would indicate bad coding somewhere
			throw new AccessDeniedException(
					"Can't change password as no Authentication object found in context "
							+ "for current user.");
		}

		String username = currentUser.getName();

		// If an authentication manager has been set, re-authenticate the user with the
		// supplied password.
		if (authenticationManager != null) {
			logger.debug("Reauthenticating user '" + username
					+ "' for password change request.");

			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
					username, oldPassword));
		}
		else {
			logger.debug("No authentication manager set. Password won't be re-checked.");
		}

		logger.debug("Changing password for user '" + username + "'");

		getJdbcTemplate().update(changePasswordSql, newPassword, username);

		SecurityContextHolder.getContext().setAuthentication(
				createNewAuthentication(currentUser, newPassword));
	}

	@Override
	public boolean userExists(String username) {
		List<String> users = getJdbcTemplate().queryForList(userExistsSql,
				new String[] { username }, String.class);

		if (users.size() > 1) {
			throw new IncorrectResultSizeDataAccessException(
					"More than one user found with name '" + username + "'", 1);
		}

		return users.size() == 1;
	}

	public void setEnableAuthorities(boolean enableAuthorities) {
		this.enableAuthorities = enableAuthorities;
	}
	
	public boolean isEnableAuthorities() {
		return enableAuthorities;
	}

	/**
	 * If <code>true</code> (the default), indicates the
	 * {@link #getUsersByUsernameQuery()} returns a username in response to a query. If
	 * <code>false</code>, indicates that a primary key is used instead. If set to
	 * <code>true</code>, the class will use the database-derived username in the returned
	 * <code>UserDetails</code>. If <code>false</code>, the class will use the
	 * {@link #loadUserByUsername(String)} derived username in the returned
	 * <code>UserDetails</code>.
	 *
	 * @param usernameBasedPrimaryKey <code>true</code> if the mapping queries return the
	 * username <code>String</code>, or <code>false</code> if the mapping returns a
	 * database primary key.
	 */
	public void setUsernameBasedPrimaryKey(boolean usernameBasedPrimaryKey) {
		this.usernameBasedPrimaryKey = usernameBasedPrimaryKey;
	}

	protected boolean isUsernameBasedPrimaryKey() {
		return this.usernameBasedPrimaryKey;
	}
	
	private void validateUserDetails(UserDetails user) {
		Assert.hasText(user.getUsername(), "Username may not be empty or null");
		validateAuthorities(user.getAuthorities());
	}
	
	private void validateAuthorities(Collection<? extends GrantedAuthority> authorities) {
		Assert.notNull(authorities, "Authorities list must not be null");

		for (GrantedAuthority authority : authorities) {
			Assert.notNull(authority, "Authorities list contains a null entry");
			Assert.hasText(authority.getAuthority(),
					"getAuthority() method must return a non-empty string");
		}
	}

	protected Authentication createNewAuthentication(Authentication currentAuth,
			String newPassword) {
		UserDetails user = loadUserByUsername(currentAuth.getName());

		UsernamePasswordAuthenticationToken newAuthentication = new UsernamePasswordAuthenticationToken(
				user, null, user.getAuthorities());
		newAuthentication.setDetails(currentAuth.getDetails());

		return newAuthentication;
	}
	
	public void setAuthenticationManager(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

    @Override
    public User findById(long id) {
        return (User) getJdbcTemplate().queryForObject(this.usersByIdQuery,
                new Long[] { id }, rowMapperUser());
    }

    @Override
    public User findByName(String name) {
        return (User) loadUserByUsername(name);
    }

    @Override
    public void saveUser(User user) {
        createUser(user);
    }

    @Override
    public void updateUser(User user) {
        updateUser((UserDetails) user);
    }

    @Override
    public void deleteUserById(long id) {
        
    }

    @Override
    public List<User> findAllUsers() {
        List<User> userList = new ArrayList<>();
        List<String> usernameList = getJdbcTemplate().queryForList(allUserListSql, String.class);
        for (String username : usernameList) {
            userList.add((User) loadUserByUsername(username));
        }

        return userList;
    }

    @Override
    public void deleteAllUsers() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isUserExist(User user) {
         return userExists(user.getUsername());
    }
}