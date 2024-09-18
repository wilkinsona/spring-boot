/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.security.oauth2.client.servlet;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationCodeGrantFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.CompositeFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OAuth2ClientAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
class OAuth2ClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OAuth2ClientAutoConfiguration.class, SecurityAutoConfiguration.class));

	private static final String REGISTRATION_PREFIX = "spring.security.oauth2.client.registration";

	@Test
	void autoConfigurationShouldBackOffForReactiveWebEnvironments() {
		new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(OAuth2ClientAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(OAuth2ClientAutoConfiguration.class));
	}

	@Test
	void clientRegistrationRepositoryBeanShouldNotBeCreatedWhenPropertiesAbsent() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ClientRegistrationRepository.class));
	}

	@Test
	void clientRegistrationRepositoryBeanShouldBeCreatedWhenPropertiesPresent() {
		this.contextRunner
			.withPropertyValues(REGISTRATION_PREFIX + ".foo.client-id=abcd",
					REGISTRATION_PREFIX + ".foo.client-secret=secret", REGISTRATION_PREFIX + ".foo.provider=github")
			.run((context) -> {
				ClientRegistrationRepository repository = context.getBean(ClientRegistrationRepository.class);
				ClientRegistration registration = repository.findByRegistrationId("foo");
				assertThat(registration).isNotNull();
				assertThat(registration.getClientSecret()).isEqualTo("secret");
			});
	}

	@Test
	void authorizedClientServiceAndRepositoryBeansAreConditionalOnClientRegistrationRepository() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(OAuth2AuthorizedClientService.class);
			assertThat(context).doesNotHaveBean(OAuth2AuthorizedClientRepository.class);
		});
	}

	@Test
	void configurationRegistersAuthorizedClientServiceAndRepositoryBeans() {
		this.contextRunner.withUserConfiguration(OAuth2AuthorizedClientRepositoryConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(InMemoryOAuth2AuthorizedClientService.class);
			assertThat(context).hasSingleBean(AuthenticatedPrincipalOAuth2AuthorizedClientRepository.class);
		});
	}

	@Test
	void authorizedClientServiceBeanIsConditionalOnMissingBean() {
		this.contextRunner.withUserConfiguration(OAuth2AuthorizedClientRepositoryConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(OAuth2AuthorizedClientService.class);
			assertThat(context).hasBean("testAuthorizedClientService");
		});
	}

	@Test
	void authorizedClientRepositoryBeanIsConditionalOnAuthorizedClientService() {
		this.contextRunner
			.run((context) -> assertThat(context).doesNotHaveBean(OAuth2AuthorizedClientRepository.class));
	}

	@Test
	void configurationRegistersAuthorizedClientRepositoryBean() {
		this.contextRunner.withUserConfiguration(OAuth2AuthorizedClientServiceConfiguration.class)
			.run((context) -> assertThat(context)
				.hasSingleBean(AuthenticatedPrincipalOAuth2AuthorizedClientRepository.class));
	}

	@Test
	void authorizedClientRepositoryBeanIsConditionalOnMissingBean() {
		this.contextRunner.withUserConfiguration(OAuth2AuthorizedClientRepositoryConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(OAuth2AuthorizedClientRepository.class);
			assertThat(context).hasBean("testAuthorizedClientRepository");
		});
	}

	@Test
	void securityWebFilterChainBeanConditionalOnWebApplication() {
		this.contextRunner.withUserConfiguration(OAuth2AuthorizedClientRepositoryConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(SecurityWebFilterChain.class));
	}

	@Test
	void configurationRegistersSecurityFilterChainBean() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(OAuth2ClientAutoConfiguration.class))
			.withUserConfiguration(OAuth2AuthorizedClientServiceConfiguration.class,
					EnableWebSecurityConfiguration.class)
			.run((context) -> {
				assertThat(hasFilter(context, OAuth2LoginAuthenticationFilter.class)).isTrue();
				assertThat(hasFilter(context, OAuth2AuthorizationCodeGrantFilter.class)).isTrue();
			});
	}

	@Test
	void securityConfigurerConfiguresOAuth2Login() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(OAuth2ClientAutoConfiguration.class))
			.withUserConfiguration(OAuth2AuthorizedClientServiceConfiguration.class,
					EnableWebSecurityConfiguration.class)
			.run((context) -> {
				ClientRegistrationRepository expected = context.getBean(ClientRegistrationRepository.class);
				ClientRegistrationRepository actual = (ClientRegistrationRepository) ReflectionTestUtils.getField(
						getSecurityFilters(context, OAuth2LoginAuthenticationFilter.class).get(0),
						"clientRegistrationRepository");
				assertThat(isEqual(expected.findByRegistrationId("first"), actual.findByRegistrationId("first")))
					.isTrue();
				assertThat(isEqual(expected.findByRegistrationId("second"), actual.findByRegistrationId("second")))
					.isTrue();
			});
	}

	@Test
	void securityConfigurerConfiguresAuthorizationCode() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(OAuth2ClientAutoConfiguration.class))
			.withUserConfiguration(OAuth2AuthorizedClientServiceConfiguration.class,
					EnableWebSecurityConfiguration.class)
			.run((context) -> {
				ClientRegistrationRepository expected = context.getBean(ClientRegistrationRepository.class);
				ClientRegistrationRepository actual = (ClientRegistrationRepository) ReflectionTestUtils.getField(
						getSecurityFilters(context, OAuth2AuthorizationCodeGrantFilter.class).get(0),
						"clientRegistrationRepository");
				assertThat(isEqual(expected.findByRegistrationId("first"), actual.findByRegistrationId("first")))
					.isTrue();
				assertThat(isEqual(expected.findByRegistrationId("second"), actual.findByRegistrationId("second")))
					.isTrue();
			});
	}

	@Test
	void autoConfigurationConditionalOnClassEnableWebSecurity() {
		assertWhenClassNotPresent(EnableWebSecurity.class);
	}

	@Test
	void autoConfigurationConditionalOnClassClientRegistration() {
		assertWhenClassNotPresent(ClientRegistration.class);
	}

	private void assertWhenClassNotPresent(Class<?> classToFilter) {
		FilteredClassLoader classLoader = new FilteredClassLoader(classToFilter);
		this.contextRunner.withClassLoader(classLoader)
			.withPropertyValues(REGISTRATION_PREFIX + ".foo.client-id=abcd",
					REGISTRATION_PREFIX + ".foo.client-secret=secret", REGISTRATION_PREFIX + ".foo.provider=github")
			.run((context) -> assertThat(context).doesNotHaveBean(ReactiveOAuth2ClientAutoConfiguration.class));
	}

	private boolean hasFilter(AssertableWebApplicationContext context, Class<? extends Filter> filter) {
		FilterChainProxy filterChainProxy = (FilterChainProxy) context.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
		return filterChainProxy.getFilterChains()
			.stream()
			.flatMap((chain) -> chain.getFilters().stream())
			.anyMatch(filter::isInstance);
	}

	private List<Filter> getSecurityFilters(AssertableWebApplicationContext context, Class<? extends Filter> filter) {
		return getSecurityFilterChain(context).getFilters().stream().filter(filter::isInstance).toList();
	}

	private SecurityFilterChain getSecurityFilterChain(AssertableWebApplicationContext context) {
		Filter springSecurityFilterChain = context.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN, Filter.class);
		FilterChainProxy filterChainProxy = getFilterChainProxy(springSecurityFilterChain);
		SecurityFilterChain securityFilterChain = filterChainProxy.getFilterChains().get(0);
		return securityFilterChain;
	}

	private FilterChainProxy getFilterChainProxy(Filter filter) {
		if (filter instanceof FilterChainProxy filterChainProxy) {
			return filterChainProxy;
		}
		if (filter instanceof CompositeFilter) {
			List<?> filters = (List<?>) ReflectionTestUtils.getField(filter, "filters");
			return (FilterChainProxy) filters.stream()
				.filter(FilterChainProxy.class::isInstance)
				.findFirst()
				.orElseThrow();
		}
		throw new IllegalStateException("No FilterChainProxy found");
	}

	private boolean isEqual(ClientRegistration reg1, ClientRegistration reg2) {
		boolean result = ObjectUtils.nullSafeEquals(reg1.getClientId(), reg2.getClientId());
		result = result && ObjectUtils.nullSafeEquals(reg1.getClientName(), reg2.getClientName());
		result = result && ObjectUtils.nullSafeEquals(reg1.getClientSecret(), reg2.getClientSecret());
		result = result && ObjectUtils.nullSafeEquals(reg1.getScopes(), reg2.getScopes());
		result = result && ObjectUtils.nullSafeEquals(reg1.getRedirectUri(), reg2.getRedirectUri());
		result = result && ObjectUtils.nullSafeEquals(reg1.getRegistrationId(), reg2.getRegistrationId());
		result = result
				&& ObjectUtils.nullSafeEquals(reg1.getAuthorizationGrantType(), reg2.getAuthorizationGrantType());
		result = result && ObjectUtils.nullSafeEquals(reg1.getProviderDetails().getAuthorizationUri(),
				reg2.getProviderDetails().getAuthorizationUri());
		result = result && ObjectUtils.nullSafeEquals(reg1.getProviderDetails().getUserInfoEndpoint(),
				reg2.getProviderDetails().getUserInfoEndpoint());
		result = result && ObjectUtils.nullSafeEquals(reg1.getProviderDetails().getTokenUri(),
				reg2.getProviderDetails().getTokenUri());
		return result;
	}

	@Configuration(proxyBeanMethods = false)
	static class ClientRepositoryConfiguration {

		@Bean
		ClientRegistrationRepository clientRegistrationRepository() {
			List<ClientRegistration> registrations = new ArrayList<>();
			registrations.add(getClientRegistration("first", "https://user-info-uri.com"));
			registrations.add(getClientRegistration("second", "https://other-user-info"));
			return new InMemoryClientRegistrationRepository(registrations);
		}

		private ClientRegistration getClientRegistration(String id, String userInfoUri) {
			ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(id);
			builder.clientName("foo")
				.clientId("foo")
				.clientAuthenticationMethod(
						org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.scope("read")
				.clientSecret("secret")
				.redirectUri("https://redirect-uri.com")
				.authorizationUri("https://authorization-uri.com")
				.tokenUri("https://token-uri.com")
				.userInfoUri(userInfoUri)
				.userNameAttributeName("login");
			return builder.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(ClientRepositoryConfiguration.class)
	static class OAuth2AuthorizedClientServiceConfiguration {

		@Bean
		OAuth2AuthorizedClientService testAuthorizedClientService(
				ClientRegistrationRepository clientRegistrationRepository) {
			return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(OAuth2AuthorizedClientServiceConfiguration.class)
	static class OAuth2AuthorizedClientRepositoryConfiguration {

		@Bean
		OAuth2AuthorizedClientRepository testAuthorizedClientRepository(
				OAuth2AuthorizedClientService authorizedClientService) {
			return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
		}

	}

	@EnableWebSecurity
	@Configuration(proxyBeanMethods = false)
	static class EnableWebSecurityConfiguration {

	}

}
