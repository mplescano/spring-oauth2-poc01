package com.mplescano.oauth.poc.poc01.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;

import com.mplescano.oauth.poc.poc01.model.entity.User;

public class ApiUserServiceImpl extends JdbcUserServiceImpl implements UserService {

    public static final String DEF_USERS_BY_ID_QUERY = "select id,username,password,enabled,roles "
            + "from users " + "where id = ?";
	
	public static final String DEF_ALL_USERS_SQL = "select username from users";
	
	private String usersByIdQuery;
	
	private String allUserListSql = DEF_ALL_USERS_SQL;

	public ApiUserServiceImpl() {
		this.usersByIdQuery = DEF_USERS_BY_ID_QUERY;
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