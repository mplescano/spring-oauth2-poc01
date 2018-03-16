package com.mplescano.oauth.poc.poc01.model.entity;

import org.springframework.security.core.userdetails.UserDetails;

public interface UserIdDetails extends UserDetails {

	Long getId();
}