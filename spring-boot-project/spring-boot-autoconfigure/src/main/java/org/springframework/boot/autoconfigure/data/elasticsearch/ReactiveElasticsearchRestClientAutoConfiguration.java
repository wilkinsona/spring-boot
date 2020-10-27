/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.elasticsearch;

import java.util.Collections;
import java.util.List;

import reactor.netty.http.client.HttpClient;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Elasticsearch Reactive REST
 * clients.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ ReactiveRestClients.class, WebClient.class, HttpClient.class })
@EnableConfigurationProperties({ ReactiveElasticsearchRestClientProperties.class, ElasticsearchProperties.class })
public class ReactiveElasticsearchRestClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ClientConfiguration clientConfiguration(ElasticsearchProperties properties,
			ReactiveElasticsearchRestClientProperties clientProperties) {
		String[] uris = determineUris(properties, clientProperties).stream().map((uri) -> {
			if (uri.startsWith("http://")) {
				return uri.substring(7);
			}
			if (uri.startsWith("https://")) {
				return uri.substring(8);
			}
			return uri;
		}).toArray(String[]::new);
		ClientConfiguration.MaybeSecureClientConfigurationBuilder builder = ClientConfiguration.builder()
				.connectedTo(uris);
		if (clientProperties.isUseSsl()) {
			builder.usingSsl();
		}
		configureTimeouts(builder, clientProperties);
		configureAuthentication(builder, properties, clientProperties);
		configureExchangeStrategies(builder, clientProperties);
		return builder.build();
	}

	private void configureTimeouts(ClientConfiguration.TerminalClientConfigurationBuilder builder,
			ReactiveElasticsearchRestClientProperties properties) {
		PropertyMapper map = PropertyMapper.get();
		map.from(properties.getConnectionTimeout()).whenNonNull().to(builder::withConnectTimeout);
		map.from(properties.getSocketTimeout()).whenNonNull().to(builder::withSocketTimeout);

	}

	@SuppressWarnings("deprecation")
	private void configureAuthentication(ClientConfiguration.TerminalClientConfigurationBuilder builder,
			ElasticsearchProperties properties, ReactiveElasticsearchRestClientProperties clientProperties) {
		PropertyMapper map = PropertyMapper.get();
		String username;
		String password;
		if (StringUtils.hasText(properties.getUsername())) {
			username = properties.getUsername();
			password = properties.getPassword();
		}
		else {
			username = clientProperties.getUsername();
			password = clientProperties.getPassword();
		}
		map.from(username).whenHasText().to((user) -> {
			HttpHeaders headers = new HttpHeaders();
			headers.setBasicAuth(user, password);
			builder.withDefaultHeaders(headers);
		});
	}

	private void configureExchangeStrategies(ClientConfiguration.TerminalClientConfigurationBuilder builder,
			ReactiveElasticsearchRestClientProperties properties) {
		PropertyMapper map = PropertyMapper.get();
		builder.withWebClientConfigurer((webClient) -> {
			ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
					.codecs((configurer) -> map.from(properties.getMaxInMemorySize()).whenNonNull()
							.asInt(DataSize::toBytes)
							.to((maxInMemorySize) -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize)))
					.build();
			return webClient.mutate().exchangeStrategies(exchangeStrategies).build();
		});
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactiveElasticsearchClient reactiveElasticsearchClient(ClientConfiguration clientConfiguration) {
		return ReactiveRestClients.create(clientConfiguration);
	}

	@SuppressWarnings("deprecation")
	private static List<String> determineUris(ElasticsearchProperties properties,
			ReactiveElasticsearchRestClientProperties clientProperties) {
		if (!Collections.singletonList("localhost:9200").equals(clientProperties.getEndpoints())) {
			return clientProperties.getEndpoints();
		}
		return properties.getUris();
	}

}
