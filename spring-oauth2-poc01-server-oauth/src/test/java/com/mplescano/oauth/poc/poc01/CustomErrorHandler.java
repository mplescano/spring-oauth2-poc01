package com.mplescano.oauth.poc.poc01;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.UnknownHttpStatusCodeException;

public class CustomErrorHandler implements ResponseErrorHandler {

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return hasError(getHttpStatusCode(response));
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		int statusCode = getHttpStatusCode(response);
		
		//String message, int statusCode, String statusText,
		//HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset
		if (statusCode >= 400) {
			throw new RestClientResponseException("client error", statusCode, response.getStatusText(),
					response.getHeaders(), getResponseBody(response), getCharset(response));
		}
		else if (statusCode >= 500) {
			throw new RestClientResponseException("server error", statusCode, response.getStatusText(),
					response.getHeaders(), getResponseBody(response), getCharset(response));
		}
		else {
			throw new RestClientException("Unknown status code [" + statusCode + "]");
		}
	}

	protected boolean hasError(int statusCode) {
		return (statusCode >= 400 ||
				statusCode >= 500);
	}
	
	/**
	 * Determine the HTTP status of the given response.
	 * @param response the response to inspect
	 * @return the associated HTTP status
	 * @throws IOException in case of I/O errors
	 * @throws UnknownHttpStatusCodeException in case of an unknown status code
	 * that cannot be represented with the {@link HttpStatus} enum
	 * @since 4.3.8
	 */
	protected int getHttpStatusCode(ClientHttpResponse response) throws IOException {
		try {
			return response.getRawStatusCode();
		}
		catch (IllegalArgumentException ex) {
			throw new UnknownHttpStatusCodeException(response.getRawStatusCode(), response.getStatusText(),
					response.getHeaders(), getResponseBody(response), getCharset(response));
		}
	}
	
	/**
	 * Determine the charset of the response (for inclusion in a status exception).
	 * @param response the response to inspect
	 * @return the associated charset, or {@code null} if none
	 * @since 4.3.8
	 */
	protected Charset getCharset(ClientHttpResponse response) {
		HttpHeaders headers = response.getHeaders();
		MediaType contentType = headers.getContentType();
		return (contentType != null ? contentType.getCharset() : null);
	}

	/**
	 * Read the body of the given response (for inclusion in a status exception).
	 * @param response the response to inspect
	 * @return the response body as a byte array,
	 * or an empty byte array if the body could not be read
	 * @since 4.3.8
	 */
	protected byte[] getResponseBody(ClientHttpResponse response) {
		try {
			return FileCopyUtils.copyToByteArray(response.getBody());
		}
		catch (IOException ex) {
			// ignore
		}
		return new byte[0];
	}
}
