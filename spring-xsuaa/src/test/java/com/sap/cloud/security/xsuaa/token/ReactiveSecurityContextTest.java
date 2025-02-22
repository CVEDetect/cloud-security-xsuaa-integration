/**
 * SPDX-FileCopyrightText: 2018-2022 SAP SE or an SAP affiliate company and Cloud Security Client Java contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sap.cloud.security.xsuaa.token;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;

import com.sap.cloud.security.xsuaa.test.JwtGenerator;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class ReactiveSecurityContextTest {

	@Test
	public void unauthenticated() {
		SecurityContext expectedContext = new SecurityContextImpl();
		ReactiveSecurityContextHolder.withSecurityContext(Mono.just(expectedContext));
		Mono<XsuaaToken> tokenMono = ReactiveSecurityContext.getToken();

		StepVerifier.create(tokenMono)
				.expectError(AccessDeniedException.class)
				.verify();
	}

	@Test
	@Ignore
	public void authenticated() {
		XsuaaToken jwt = new XsuaaToken(new JwtGenerator().setUserName("user").getToken());
		SecurityContext expectedContext = new SecurityContextImpl(
				new TestingAuthenticationToken("user", jwt, "ROLE_USER"));
		ReactiveSecurityContextHolder.withSecurityContext(Mono.just(expectedContext));
		Mono<XsuaaToken> tokenMono = ReactiveSecurityContext.getToken();

		StepVerifier.create(tokenMono)
				.expectNext(jwt)
				.verifyComplete();
	}

}
