/**
 * SPDX-FileCopyrightText: 2018-2022 SAP SE or an SAP affiliate company and Cloud Security Client Java contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sap.cloud.security.xsuaa.token.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.nimbusds.jwt.JWT;
import com.sap.cloud.security.xsuaa.XsuaaCredentials;
import com.sap.cloud.security.xsuaa.XsuaaServiceConfiguration;
import com.sap.cloud.security.xsuaa.XsuaaServiceConfigurationCustom;
import com.sap.cloud.security.xsuaa.XsuaaServiceConfigurationDefault;
import com.sap.cloud.security.xsuaa.token.TokenClaims;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestOperations;

@RunWith(SpringRunner.class)
@TestPropertySource("/XsuaaJwtDecoderTest.properties")
@ContextConfiguration(classes = XsuaaServiceConfigurationDefault.class)
public class XsuaaJwtDecoderTest {

	@Autowired
	XsuaaServiceConfiguration configurationWithVerificationKey;

	XsuaaServiceConfiguration configuration = new XsuaaServiceConfigurationCustom(new XsuaaCredentials());
	private String rsaToken;
	private String ccToken;
	private String jwks;

	@Before
	public void setUp() throws IOException {
		rsaToken = IOUtils.resourceToString("/accessTokenRSA256WithVerificationKey.txt", StandardCharsets.UTF_8);
		ccToken = IOUtils.resourceToString("/token_cc.txt", StandardCharsets.UTF_8);
		jwks = IOUtils.resourceToString("/jwks.json", StandardCharsets.UTF_8);
	}

	@Test
	public void decode_withJwks_cache_disabled() {
		RestOperations restTemplate = Mockito.mock(RestOperations.class);
		Mockito.when(restTemplate.exchange(any(), eq(String.class))).thenReturn(ResponseEntity.ok().body(jwks));

		final JwtDecoder cut = new XsuaaJwtDecoderBuilder(configurationWithVerificationKey)
				.withDecoderCacheTime(0)
				.withRestOperations(restTemplate)
				.build();

		assertThat(cut.decode(rsaToken).getClaimAsString(TokenClaims.CLAIM_CLIENT_ID)).isEqualTo("sb-clientId!t0815");
		assertThat(cut.decode(rsaToken).getClaimAsString(TokenClaims.CLAIM_CLIENT_ID)).isEqualTo("sb-clientId!t0815");

		Mockito.verify(restTemplate, times(2)).exchange(any(), eq(String.class));
	}

	@Test
	public void decode_withJwks_cache_default() {
		RestOperations restTemplate = Mockito.mock(RestOperations.class);
		Mockito.when(restTemplate.exchange(any(), eq(String.class))).thenReturn(ResponseEntity.ok().body(jwks));

		final JwtDecoder cut = new XsuaaJwtDecoderBuilder(configurationWithVerificationKey)
				.withRestOperations(restTemplate)
				.build();

		assertThat(cut.decode(rsaToken).getClaimAsString(TokenClaims.CLAIM_CLIENT_ID)).isEqualTo("sb-clientId!t0815");
		assertThat(cut.decode(rsaToken).getClaimAsString(TokenClaims.CLAIM_CLIENT_ID)).isEqualTo("sb-clientId!t0815");

		Mockito.verify(restTemplate, times(1)).exchange(any(), eq(String.class));
	}

	@Test
	public void decode_withFallbackVerificationKey() {
		final JwtDecoder cut = new XsuaaJwtDecoderBuilder(configurationWithVerificationKey).build();

		assertThat(cut.decode(rsaToken).getClaimAsString(TokenClaims.CLAIM_CLIENT_ID)).isEqualTo("sb-clientId!t0815");
	}

	@Test
	public void decode_withInvalidFallbackVerificationKey_withoutUaaDomain() {
		XsuaaServiceConfiguration config = Mockito.mock(XsuaaServiceConfiguration.class);
		Mockito.when(config.getVerificationKey()).thenReturn(
				"xsuaa.verificationkey=-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAm1QaZzMjtEfHdimrHP3/2Yr+1z685eiOUlwybRVG9i8wsgOUh+PUGuQL8hgulLZWXU5MbwBLTECAEMQbcRTNVTolkq4i67EP6JesHJIFADbK1Ni0KuMcPuiyOLvDKiDEMnYG1XP3X3WCNfsCVT9YoU+lWIrZr/ZsIvQri8jczr4RkynbTBsPaAOygPUlipqDrpadMO1momNCbea/o6GPn38LxEw609ItfgDGhL6f/yVid5pFzZQWb+9l6mCuJww0hnhO6gt6Rv98OWDty9G0frWAPyEfuIW9B+mR/2vGhyU9IbbWpvFXiy9RVbbsM538TCjd5JF2dJvxy24addC4oQIDAQAB-----END PUBLIC KEY-----");

		final JwtDecoder cut = new XsuaaJwtDecoderBuilder(config).build();

		assertThatThrownBy(() -> cut.decode(rsaToken)).isInstanceOf(BadJwtException.class)
				.hasMessageContaining("Jwt validation with fallback verificationkey failed");
	}

	@Test
	public void decode_withFallbackVerificationKey_remoteKeyFetchFailed()  {
		XsuaaServiceConfiguration config = Mockito.mock(XsuaaServiceConfiguration.class);
		Mockito.when(config.getVerificationKey()).thenReturn(
				"-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAm1QaZzMjtEfHdimrHP3/2Yr+1z685eiOUlwybRVG9i8wsgOUh+PUGuQL8hgulLZWXU5MbwBLTECAEMQbcRTNVTolkq4i67EP6JesHJIFADbK1Ni0KuMcPuiyOLvDKiDEMnYG1XP3X3WCNfsCVT9YoU+lWIrZr/ZsIvQri8jczr4RkynbTBsPaAOygPUlipqDrpadMO1momNCbea/o6GPn38LxEw609ItfgDGhL6f/yVid5pFzZQWb+9l6mCuJww0hnhO6gt6Rv98OWDty9G0frWAPyEfuIW9B+mR/2vGhyU9IbbWpvFXiy9RVbbsM538TCjd5JF2dJvxy24addC4oQIDAQAB-----END PUBLIC KEY-----");
		Mockito.when(config.getUaaDomain()).thenReturn("localhost");
		Mockito.when(config.getClientId()).thenReturn("sb-clientId!t0815");

		final JwtDecoder cut = new XsuaaJwtDecoderBuilder(config).build();
		final Jwt jwt = cut.decode(rsaToken);

		assertThat(jwt.getAudience().get(0)).isEqualTo("sb-clientId!t0815");
	}

	@Test
	public void decode_withNonMatchingVerificationKey_throwsException()  {
		final JwtDecoder cut = new XsuaaJwtDecoderBuilder(configuration).build();

		assertThatThrownBy(() -> cut.decode(ccToken)).isInstanceOf(JwtException.class)
				.hasMessageContaining("Cannot verify with online token key, jku, kid, uaadomain is null");
	}

	@Test
	public void decode_whenJwksContainsInvalidJwksDomain_throwsException() throws IOException {
		String token = IOUtils.resourceToString("/token_user.txt", StandardCharsets.UTF_8);
		XsuaaJwtDecoder cut = (XsuaaJwtDecoder) new XsuaaJwtDecoderBuilder(configuration).build();

		cut.setTokenInfoExtractor(new TokenInfoExtractorImpl("https://subdomain.wrongoauth.ondemand.com/token_keys"));
		assertThatThrownBy(() -> cut.decode(token)).isInstanceOf(JwtException.class)
				.hasMessageContaining("JWT verification failed: Do not trust 'jku' token header");

		cut.setTokenInfoExtractor(
				new TokenInfoExtractorImpl("http://myauth.ondemand.com@malicious.ondemand.com/token_keys"));
		assertThatThrownBy(() -> cut.decode(token)).isInstanceOf(JwtException.class)
				.hasMessageContaining("JWT verification failed: Do not trust 'jku' token header");

		cut.setTokenInfoExtractor(new TokenInfoExtractorImpl(
				"http://malicious.ondemand.com/token_keys///myauth.ondemand.com/token_keys"));
		assertThatThrownBy(() -> cut.decode(token)).isInstanceOf(JwtException.class)
				.hasMessageContaining("JWT verification failed: Do not trust 'jku' token header");
	}

	@Test
	public void decode_whenJwksUrlIsNotValid_throwsException() {
		XsuaaJwtDecoder cut = (XsuaaJwtDecoder) new XsuaaJwtDecoderBuilder(configuration).build();

		cut.setTokenInfoExtractor(
				new TokenInfoExtractorImpl("http://myauth.ondemand.com\\@malicious.ondemand.com/token_keys"));
		assertThatThrownBy(() -> cut.decode(ccToken)).isInstanceOf(JwtException.class)
				.hasMessageContaining("JWT verification failed: JKU of token header is not valid");
	}

	@Test
	public void decode_whenJwksContainsInvalidPath_throwsException() {
		XsuaaJwtDecoder cut = (XsuaaJwtDecoder) new XsuaaJwtDecoderBuilder(configuration).build();
		cut.setTokenInfoExtractor(new TokenInfoExtractorImpl("https://subdomain.myauth.ondemand.com/wrong_endpoint"));

		assertThatThrownBy(() -> cut.decode(ccToken)).isInstanceOf(JwtException.class)
				.hasMessageContaining("Jwt token does not contain a valid 'jku' header parameter");
	}

	@Test
	public void decode_whenJwksContainQueryParameters_throwsException() {
		XsuaaJwtDecoder cut = (XsuaaJwtDecoder) new XsuaaJwtDecoderBuilder(configuration).build();
		cut.setTokenInfoExtractor(new TokenInfoExtractorImpl("https://subdomain.myauth.ondemand.com/token_keys?a=b"));

		assertThatThrownBy(() -> cut.decode(ccToken)).isInstanceOf(JwtException.class)
				.hasMessageContaining("Jwt token does not contain a valid 'jku' header parameter: ");

	}

	@Test
	public void decode_whenJwksContainsFragment_throwsException() {
		XsuaaJwtDecoder cut = (XsuaaJwtDecoder) new XsuaaJwtDecoderBuilder(configuration).build();
		cut.setTokenInfoExtractor(
				new TokenInfoExtractorImpl("https://subdomain.myauth.ondemand.com/token_keys#token_keys"));

		assertThatThrownBy(() -> cut.decode(ccToken)).isInstanceOf(JwtException.class)
				.hasMessageContaining("Jwt token does not contain a valid 'jku' header parameter:");
	}

	private static class TokenInfoExtractorImpl
			implements com.sap.cloud.security.xsuaa.token.authentication.TokenInfoExtractor {
		private final String jku;

		public TokenInfoExtractorImpl(String jku) {
			this.jku = jku;
		}

		@Override
		public String getJku(JWT jwt) {
			return jku;
		}

		@Override
		public String getKid(JWT jwt) {
			return "kid";
		}

		@Override
		public String getUaaDomain(JWT jwt) {
			return "myauth.ondemand.com";
		}
	}
}