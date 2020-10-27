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

package org.springframework.boot.autoconfigure.elasticsearch;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Elasticsearch REST clients.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RestHighLevelClient.class)
@ConditionalOnMissingBean(RestClient.class)
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchRestClientAutoConfiguration {

	@SuppressWarnings("deprecation")
	private static List<String> determineUris(ElasticsearchProperties properties) {
		if (!Collections.singletonList("http://localhost:9200").equals(properties.getRest().getUris())) {
			return properties.getRest().getUris();
		}
		return properties.getUris();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(RestClientBuilder.class)
	static class RestClientBuilderConfiguration {

		@Bean
		RestClientBuilderCustomizer defaultRestClientBuilderCustomizer(ElasticsearchProperties properties) {
			return new DefaultRestClientBuilderCustomizer(properties);
		}

		@Bean
		RestClientBuilder elasticsearchRestClientBuilder(ElasticsearchProperties properties,
				ObjectProvider<RestClientBuilderCustomizer> builderCustomizers) {
			HttpHost[] hosts = determineUris(properties).stream().map(this::createHttpHost).toArray(HttpHost[]::new);
			RestClientBuilder builder = RestClient.builder(hosts);
			builder.setHttpClientConfigCallback((httpClientBuilder) -> {
				builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(httpClientBuilder));
				return httpClientBuilder;
			});
			builder.setRequestConfigCallback((requestConfigBuilder) -> {
				builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(requestConfigBuilder));
				return requestConfigBuilder;
			});
			builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder;
		}

		private HttpHost createHttpHost(String uri) {
			try {
				return createHttpHost(URI.create(uri));
			}
			catch (IllegalArgumentException ex) {
				return HttpHost.create(uri);
			}
		}

		private HttpHost createHttpHost(URI uri) {
			if (!StringUtils.hasLength(uri.getUserInfo())) {
				return HttpHost.create(uri.toString());
			}
			try {
				return HttpHost.create(new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(),
						uri.getQuery(), uri.getFragment()).toString());
			}
			catch (URISyntaxException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(RestHighLevelClient.class)
	static class RestHighLevelClientConfiguration {

		@Bean
		RestHighLevelClient elasticsearchRestHighLevelClient(RestClientBuilder restClientBuilder) {
			return new RestHighLevelClient(restClientBuilder);
		}

	}

	static class DefaultRestClientBuilderCustomizer implements RestClientBuilderCustomizer {

		private static final PropertyMapper map = PropertyMapper.get();

		private final ElasticsearchProperties properties;

		DefaultRestClientBuilderCustomizer(ElasticsearchProperties properties) {
			this.properties = properties;
		}

		@Override
		public void customize(RestClientBuilder builder) {
		}

		@Override
		public void customize(HttpAsyncClientBuilder builder) {
			builder.setDefaultCredentialsProvider(new PropertiesCredentialsProvider(this.properties));
		}

		@Override
		public void customize(RequestConfig.Builder builder) {
			map.from(this.properties.getRest()::getConnectionTimeout).whenNonNull().asInt(Duration::toMillis)
					.to(builder::setConnectTimeout);
			map.from(this.properties.getRest()::getReadTimeout).whenNonNull().asInt(Duration::toMillis)
					.to(builder::setSocketTimeout);
		}

	}

	private static class PropertiesCredentialsProvider extends BasicCredentialsProvider {

		PropertiesCredentialsProvider(ElasticsearchProperties properties) {
			Credentials credentials = determineCredentials(properties);
			if (credentials != null) {
				setCredentials(AuthScope.ANY, credentials);
			}
			determineUris(properties).stream().map(this::toUri).filter(this::hasUserInfo)
					.forEach(this::addUserInfoCredentials);
		}

		@SuppressWarnings("deprecation")
		private Credentials determineCredentials(ElasticsearchProperties properties) {
			if (StringUtils.hasText(properties.getUsername())) {
				return new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword());
			}
			if (StringUtils.hasText(properties.getRest().getUsername())) {
				return new UsernamePasswordCredentials(properties.getRest().getUsername(),
						properties.getRest().getPassword());
			}
			return null;
		}

		private URI toUri(String uri) {
			try {
				return URI.create(uri);
			}
			catch (IllegalArgumentException ex) {
				return null;
			}
		}

		private boolean hasUserInfo(URI uri) {
			return uri != null && StringUtils.hasLength(uri.getUserInfo());
		}

		private void addUserInfoCredentials(URI uri) {
			AuthScope authScope = new AuthScope(uri.getHost(), uri.getPort());
			Credentials credentials = createUserInfoCredentials(uri.getUserInfo());
			setCredentials(authScope, credentials);
		}

		private Credentials createUserInfoCredentials(String userInfo) {
			int delimiter = userInfo.indexOf(":");
			if (delimiter == -1) {
				return new UsernamePasswordCredentials(userInfo, null);
			}
			String username = userInfo.substring(0, delimiter);
			String password = userInfo.substring(delimiter + 1);
			return new UsernamePasswordCredentials(username, password);
		}

	}

}
