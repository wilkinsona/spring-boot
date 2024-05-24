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

package org.springframework.boot.docker.compose.service.connection.postgres;

import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.docker.compose.service.connection.jdbc.JdbcCredentials;
import org.springframework.boot.docker.compose.service.connection.jdbc.JdbcUrlBuilder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link JdbcConnectionDetails}
 * for a {@code postgres} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class PostgresJdbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<JdbcConnectionDetails> {

	private static final String[] POSTGRES_CONTAINER_NAMES = { "postgres", "bitnami/postgresql" };

	protected PostgresJdbcDockerComposeConnectionDetailsFactory() {
		super(POSTGRES_CONTAINER_NAMES);
	}

	@Override
	protected JdbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new PostgresJdbcDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link JdbcConnectionDetails} backed by a {@code postgres} {@link RunningService}.
	 */
	static class PostgresJdbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements JdbcConnectionDetails, EnvironmentAware {

		private static final JdbcUrlBuilder jdbcUrlBuilder = new JdbcUrlBuilder("postgresql", 5432);

		private final PostgresEnvironment postgresEnvironment;

		private final String jdbcUrl;

		private final JdbcCredentials credentials;

		PostgresJdbcDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.postgresEnvironment = new PostgresEnvironment(service.env());
			JdbcCredentials serviceCredentials = JdbcCredentials.from(service);
			this.credentials = serviceCredentials != null ? serviceCredentials : this.postgresEnvironment;
			this.jdbcUrl = jdbcUrlBuilder.build(service, this.postgresEnvironment.getDatabase());
		}

		@Override
		public void setEnvironment(Environment environment) {
			if (this.credentials instanceof EnvironmentAware environmentAwareCredentials) {
				environmentAwareCredentials.setEnvironment(environment);
			}
		}

		@Override
		public String getUsername() {
			return this.credentials.getUsername();
		}

		@Override
		public String getPassword() {
			return this.credentials.getPassword();
		}

		@Override
		public String getJdbcUrl() {
			return this.jdbcUrl;
		}

	}

}
