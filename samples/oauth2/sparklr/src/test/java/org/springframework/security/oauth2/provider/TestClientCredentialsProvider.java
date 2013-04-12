package org.springframework.security.oauth2.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.test.OAuth2ContextConfiguration;
import org.springframework.security.oauth2.client.test.OAuth2ContextSetup;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;

/**
 * @author Ryan Heaton
 * @author Dave Syer
 */
public class TestClientCredentialsProvider {

	@Rule
	public ServerRunning serverRunning = ServerRunning.isRunning();

	@Rule
	public OAuth2ContextSetup context = OAuth2ContextSetup.standard(serverRunning);
	
	private ClientCredentialsResourceDetails resource;

	private HttpHeaders responseHeaders;

	private HttpStatus responseStatus;

	/**
	 * tests the basic provider
	 */
	@Test
	@OAuth2ContextConfiguration(ClientCredentials.class)
	public void testPostForToken() throws Exception {
		OAuth2AccessToken token = context.getAccessToken();
		assertNull(token.getRefreshToken());
	}

	/**
	 * tests the basic provider
	 */
	@Test
	@OAuth2ContextConfiguration(FormClientCredentials.class)
	public void testPostForTokenWithForm() throws Exception {
		OAuth2AccessToken token = context.getAccessToken();
		assertNull(token.getRefreshToken());
	}

	/**
	 * tests that the registered scopes are used as defaults
	 */
	@Test
	@OAuth2ContextConfiguration(NoScopeClientCredentials.class)
	public void testPostForTokenWithNoScopes() throws Exception {
		OAuth2AccessToken token = context.getAccessToken();
		assertFalse(token.getScope().isEmpty());
	}

	@Test
	@OAuth2ContextConfiguration(resource = InvalidClientCredentials.class, initialize = false)
	public void testInvalidCredentials() throws Exception {
		context.setAccessTokenProvider(new ClientCredentialsAccessTokenProvider() {
			@Override
			protected ResponseErrorHandler getResponseErrorHandler() {
				return new DefaultResponseErrorHandler() {
					public void handleError(ClientHttpResponse response) throws IOException {
						responseHeaders = response.getHeaders();
						responseStatus = response.getStatusCode();
					}
				};
			}
		});
		try {
			context.getAccessToken();
			fail("Expected ResourceAccessException");
		}
		catch (Exception e) {
			// ignore
		}
		// System.err.println(responseHeaders);
		String header = responseHeaders.getFirst("WWW-Authenticate");
		assertTrue("Wrong header: " + header, header.contains("Basic realm"));
		assertEquals(HttpStatus.UNAUTHORIZED, responseStatus);
	}

	@Test
	@OAuth2ContextConfiguration(resource = InvalidClientCredentials.class, initialize = false)
	public void testInvalidCredentialsWithFormAuthentication() throws Exception {
		resource.setClientAuthenticationScheme(AuthenticationScheme.form);
		context.setAccessTokenProvider(new ClientCredentialsAccessTokenProvider() {
			@Override
			protected ResponseErrorHandler getResponseErrorHandler() {
				return new DefaultResponseErrorHandler() {
					public void handleError(ClientHttpResponse response) throws IOException {
						responseHeaders = response.getHeaders();
						responseStatus = response.getStatusCode();
					}
				};
			}
		});
		try {
			context.getAccessToken();
			fail("Expected ResourceAccessException");
		}
		catch (Exception e) {
			// ignore
		}
		// System.err.println(responseHeaders);
		String header = responseHeaders.getFirst("WWW-Authenticate");
		assertTrue("Wrong header: " + header, header.contains("Form realm"));
		assertEquals(HttpStatus.UNAUTHORIZED, responseStatus);
	}

	static class ClientCredentials extends ClientCredentialsResourceDetails {
		public ClientCredentials(Object target) {
			setClientId("my-client-with-registered-redirect");
			setScope(Arrays.asList("read"));
			setId(getClientId());
			TestClientCredentialsProvider test = (TestClientCredentialsProvider) target;
			setAccessTokenUri(test.serverRunning.getUrl("/sparklr2/oauth/token"));
			test.resource = this;
		}
	}

	static class FormClientCredentials extends ClientCredentials {
		public FormClientCredentials(Object target) {
			super(target);
			setClientAuthenticationScheme(AuthenticationScheme.form);
		}
	}

	static class NoScopeClientCredentials extends ClientCredentialsResourceDetails {
		public NoScopeClientCredentials(Object target) {
			setClientId("my-client-with-registered-redirect");
			setId(getClientId());
			TestClientCredentialsProvider test = (TestClientCredentialsProvider) target;
			setAccessTokenUri(test.serverRunning.getUrl("/sparklr2/oauth/token"));
		}
	}

	static class InvalidClientCredentials extends ClientCredentials {
		public InvalidClientCredentials(Object target) {
			super(target);
			setClientId("my-client-with-secret");
			setClientSecret("wrong");
		}
	}

}
