package com.mplescano.oauth.poc.poc01;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

public class CustomSimpleClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

	private int connectTimeout = -1;

	private int readTimeout = -1;
	
	private boolean followRedirect = true;
	
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
		super.setConnectTimeout(connectTimeout);
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
		super.setReadTimeout(readTimeout);
	}
	
	public void setFollowRedirect(boolean followRedirect) {
		this.followRedirect = followRedirect;
	}

	protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		if (this.connectTimeout >= 0) {
			connection.setConnectTimeout(this.connectTimeout);
		}
		if (this.readTimeout >= 0) {
			connection.setReadTimeout(this.readTimeout);
		}

		connection.setDoInput(true);

		if ("GET".equals(httpMethod)) {
			connection.setInstanceFollowRedirects(followRedirect);
		}
		else {
			connection.setInstanceFollowRedirects(false);
		}

		if ("POST".equals(httpMethod) || "PUT".equals(httpMethod) ||
				"PATCH".equals(httpMethod) || "DELETE".equals(httpMethod)) {
			connection.setDoOutput(true);
		}
		else {
			connection.setDoOutput(false);
		}

		connection.setRequestMethod(httpMethod);
	}

}
