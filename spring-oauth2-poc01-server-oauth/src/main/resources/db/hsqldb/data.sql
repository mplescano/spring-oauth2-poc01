INSERT INTO oauth_client_details
	(client_id, client_secret, scope, resource_ids,
	authorized_grant_types,	web_server_redirect_uri, authorities, 
	access_token_validity, refresh_token_validity, additional_information, autoapprove)
VALUES
	('fooClientIdPassword', 'secret', 'foo,read,write', null,
	'password,authorization_code,refresh_token', null, null, 
	36000, 36000, null, true);

INSERT INTO oauth_client_details
(client_id, client_secret, scope, 
authorized_grant_types, web_server_redirect_uri, authorities, 
access_token_validity, refresh_token_validity, additional_information, autoapprove)
VALUES
('sampleClientId', 'secret', 'read,write,foo,bar',
'implicit', 'http://TCDIGITAL35ZZZ:8070/implicit', 'ROLE_CLIENT', 
36000, 36000, null, false);

INSERT INTO oauth_client_details
	(client_id, client_secret, scope, resource_ids, 
	authorized_grant_types,	web_server_redirect_uri, authorities, 
	access_token_validity, refresh_token_validity, additional_information, autoapprove)
VALUES
	('my-trusted-client', 'secret', 'read,write,trust', 'spring-oauth2-poc01',
	'password,authorization_code,refresh_token', null, 'ROLE_CLIENT,ROLE_TRUSTED_CLIENT', 
	120, 600, null, true);

INSERT INTO oauth_client_details
	(client_id, client_secret, scope, resource_ids, 
	authorized_grant_types,	web_server_redirect_uri, authorities, 
	access_token_validity, refresh_token_validity, additional_information, autoapprove)
VALUES
	('my-trusted-app', 'secret', 'read,write,trust', 'spring-oauth2-poc01',
	'client_credentials,password', null, 'ROLE_CLIENT,ROLE_TRUSTED_CLIENT', 
	120, 600, null, true);

INSERT INTO oauth_client_details
	(client_id, client_secret, scope, resource_ids, 
	authorized_grant_types,	web_server_redirect_uri, authorities, 
	access_token_validity, refresh_token_validity, additional_information, autoapprove)
VALUES
	('my-normal-app', null, 'read,write', 'spring-oauth2-poc01',
	'authorization_code,implicit', 'http://localhost:8090/resources/user', 'ROLE_CLIENT', 
	120, 600, null, true);
	
INSERT INTO authorities VALUES (1, 'ROLE_ADMIN', 'PERM_VIEW_USER');
INSERT INTO authorities VALUES (2, 'ROLE_ADMIN', 'PERM_MODIFY_USER');
INSERT INTO authorities VALUES (3, 'ROLE_ADMIN', 'PERM_ADD_USER');
INSERT INTO authorities VALUES (4, 'ROLE_USER', 'PERM_VIEW_USER');
INSERT INTO authorities VALUES (5, 'ROLE_USER', 'PERM_ADD_USER');

INSERT INTO users(id, first_name, last_name, roles, username, password, enabled, created_at) VALUES (1, 'Joe', 'Black', 'ROLE_USER', 'john', '123', FALSE, '2016-12-26 19:36:34');
INSERT INTO users(id, first_name, last_name, roles, username, password, enabled, created_at) VALUES (2, 'Tom', 'Duncan', 'ROLE_ADMIN', 'tom', '111', TRUE, '2016-12-23 17:30:51');
INSERT INTO users(id, first_name, last_name, roles, username, password, enabled, created_at) VALUES (3, 'Keith', 'Andrews', 'ROLE_USER', 'user1', 'pass', TRUE, '2016-06-04 23:53:59');
INSERT INTO users(id, first_name, last_name, roles, username, password, enabled, created_at) VALUES (4, 'Karen', 'Moreno', 'ROLE_ADMIN', 'admin', 'nimda', TRUE, '2016-02-08 02:37:11');
INSERT INTO users(id, first_name, last_name, roles, username, password, enabled, created_at) VALUES (5, 'Bill', 'Bill', 'ROLE_ADMIN', 'bill', 'abc123', TRUE, '2016-02-08 02:37:11');
INSERT INTO users(id, first_name, last_name, roles, username, password, enabled, created_at) VALUES (6, 'Bob', 'Bob', 'ROLE_USER', 'bob', 'abc123', TRUE, '2016-02-08 02:37:11');

