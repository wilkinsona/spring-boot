/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webmvc.test.autoconfigure;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.test.http.client.BaseUrlUriBuilderFactory;
import org.springframework.boot.test.http.server.BaseUrl;
import org.springframework.boot.test.http.server.BaseUrlProviders;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.WebApplicationContext;

/**
 * Auto-configuration for {@link RestTestClient}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ RestClient.class, RestTestClient.class, ClientHttpMessageConvertersCustomizer.class })
public final class RestTestClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	RestTestClient restTestClient(WebApplicationContext applicationContext, ObjectProvider<MockMvc> mockMvc,
			List<RestTestClientBuilderCustomizer> customizers) {
		RestTestClient.Builder<?> builder = prepareBuilder(applicationContext, mockMvc.getIfAvailable());
		for (RestTestClientBuilderCustomizer customizer : customizers) {
			customizer.customize(builder);
		}
		return builder.build();
	}

	private RestTestClient.Builder<?> prepareBuilder(WebApplicationContext applicationContext,
			@Nullable MockMvc mockMvc) {
		BaseUrl baseUrl = new BaseUrlProviders(applicationContext).getBaseUrlOrDefault();
		if (baseUrl == BaseUrl.DEFAULT) {
			return (mockMvc != null) ? RestTestClient.bindTo(mockMvc)
					: RestTestClient.bindToApplicationContext(applicationContext);
		}
		return RestTestClient.bindToServer().uriBuilderFactory(BaseUrlUriBuilderFactory.get(baseUrl));
	}

	@Bean
	SpringBootRestTestClientBuilderCustomizer springBootRestTestClientBuilderCustomizer(
			ObjectProvider<ClientHttpMessageConvertersCustomizer> httpMessageConverterCustomizers) {
		return new SpringBootRestTestClientBuilderCustomizer(httpMessageConverterCustomizers.orderedStream().toList());
	}

}
