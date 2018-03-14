drop table if exists oauth_client_details;
create table oauth_client_details (
  client_id VARCHAR(255) PRIMARY KEY,
  resource_ids VARCHAR(255),
  client_secret VARCHAR(255),
  scope VARCHAR(255),
  authorized_grant_types VARCHAR(255),
  web_server_redirect_uri VARCHAR(255),
  authorities VARCHAR(255),
  access_token_validity INTEGER,
  refresh_token_validity INTEGER,
  additional_information VARCHAR(4096),
  autoapprove VARCHAR(255)
);
 
drop table if exists oauth_client_token;
create table oauth_client_token (
  authentication_id VARCHAR(255) PRIMARY KEY,
  token_id VARCHAR(255),
  token BLOB,
  user_name VARCHAR(255),
  client_id VARCHAR(255)
);
 
drop table if exists oauth_access_token;
create table oauth_access_token (
  authentication_id VARCHAR(255) PRIMARY KEY,
  token_id VARCHAR(255),
  token BLOB,
  user_name VARCHAR(255),
  client_id VARCHAR(255),
  authentication BLOB,
  refresh_token VARCHAR(255)
);
 
drop table if exists oauth_refresh_token;
create table oauth_refresh_token (
  token_id VARCHAR(255),
  token BLOB,
  authentication BLOB
);
 
drop table if exists oauth_code;
create table oauth_code (
  code VARCHAR(255),
  authentication BLOB
);
 
drop table if exists oauth_approvals;
create table oauth_approvals (
    user_id VARCHAR(255),
    client_id VARCHAR(255),
    scope VARCHAR(255),
    status VARCHAR(10),
    expires_at TIMESTAMP,
    last_modified_at TIMESTAMP
);
 
drop table if exists client_details;
create table client_details (
  app_id VARCHAR(255) PRIMARY KEY,
  resource_ids VARCHAR(255),
  app_secret VARCHAR(255),
  scope VARCHAR(255),
  grant_types VARCHAR(255),
  redirect_url VARCHAR(255),
  authorities VARCHAR(255),
  access_token_validity INTEGER,
  refresh_token_validity INTEGER,
  additional_information VARCHAR(4096),
  auto_approve_scopes VARCHAR(255)
);

DROP TABLE users IF EXISTS;
CREATE TABLE users (
  id         INTEGER IDENTITY PRIMARY KEY,
  first_name VARCHAR(30) NOT NULL,
  last_name  VARCHAR(30) NOT NULL,
  roles    VARCHAR(255) NOT NULL,
  username       VARCHAR(30) NOT NULL,
  password  VARCHAR(255) NOT NULL,
  enabled boolean not null,
  created_at	TIMESTAMP not null,
  UNIQUE (username),
  UNIQUE (first_name, last_name)
);

DROP TABLE authorities IF EXISTS;
create table authorities (
	id         INTEGER IDENTITY PRIMARY KEY,
	role varchar(50) not null,
	authority varchar(50) not null,
	UNIQUE (role, authority)
);