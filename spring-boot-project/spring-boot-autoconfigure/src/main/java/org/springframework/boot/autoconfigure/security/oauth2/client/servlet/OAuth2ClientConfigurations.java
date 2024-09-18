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

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.ConditionalOnDefaultWebSecurity;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * OAuth2 Client configurations.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
class OAuth2ClientConfigurations {

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(OAuth2ClientProperties.class)
	@Conditional(ClientsConfiguredCondition.class)
	@ConditionalOnMissingBean(ClientRegistrationRepository.class)
	static class ClientRegistrationRepositoryConfiguration {

		@Bean
		InMemoryClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
			List<ClientRegistration> registrations = new ArrayList<>(
					new OAuth2ClientPropertiesMapper(properties).asClientRegistrations().values());
			return new InMemoryClientRegistrationRepository(registrations);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(ClientRegistrationRepository.class)
	static class OAuth2ClientConfiguration {

		@Bean
		@ConditionalOnMissingBean
		OAuth2AuthorizedClientService authorizedClientService(
				ClientRegistrationRepository clientRegistrationRepository) {
			return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
		}

		@Bean
		@ConditionalOnMissingBean
		OAuth2AuthorizedClientRepository authorizedClientRepository(
				OAuth2AuthorizedClientService authorizedClientService) {
			return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnDefaultWebSecurity
		@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
		static class OAuth2SecurityFilterChainConfiguration {

			@Bean
			SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
				http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated());
				http.oauth2Login(withDefaults());
				http.oauth2Client(withDefaults());
				return http.build();
			}

		}

	}

}
