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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

/**
 * Configuration properties for Elasticsearch
 *
 * @author Andy Wilkinson
 * @since 2.4.0
 */
@ConfigurationProperties("spring.elasticsearch")
public class ElasticsearchProperties {

	/**
	 * Comma-separated list of the Elasticsearch instances to use.
	 */
	private List<String> uris = new ArrayList<>(Collections.singletonList("http://localhost:9200"));

	private String username;

	private String password;

	private final Rest rest = new Rest();

	public List<String> getUris() {
		return this.uris;
	}

	public void setUris(List<String> uris) {
		this.uris = uris;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Rest getRest() {
		return this.rest;
	}

	public static class Rest {

		/**
		 * Comma-separated list of the Elasticsearch instances to use.
		 */
		private List<String> uris = new ArrayList<>(Collections.singletonList("http://localhost:9200"));

		/**
		 * Credentials username.
		 */
		private String username;

		/**
		 * Credentials password.
		 */
		private String password;

		/**
		 * Connection timeout.
		 */
		private Duration connectionTimeout = Duration.ofSeconds(1);

		/**
		 * Read timeout.
		 */
		private Duration readTimeout = Duration.ofSeconds(30);

		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "spring.elasticsearch.uris")
		public List<String> getUris() {
			return this.uris;
		}

		@Deprecated
		public void setUris(List<String> uris) {
			this.uris = uris;
		}

		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "spring.elasticsearch.username")
		public String getUsername() {
			return this.username;
		}

		@Deprecated
		public void setUsername(String username) {
			this.username = username;
		}

		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "spring.elasticsearch.password")
		public String getPassword() {
			return this.password;
		}

		@Deprecated
		public void setPassword(String password) {
			this.password = password;
		}

		public Duration getConnectionTimeout() {
			return this.connectionTimeout;
		}

		public void setConnectionTimeout(Duration connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
		}

		public Duration getReadTimeout() {
			return this.readTimeout;
		}

		public void setReadTimeout(Duration readTimeout) {
			this.readTimeout = readTimeout;
		}

	}

}
