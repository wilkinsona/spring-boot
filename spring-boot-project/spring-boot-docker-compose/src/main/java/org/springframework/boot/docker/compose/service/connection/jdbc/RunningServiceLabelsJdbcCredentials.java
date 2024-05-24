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

package org.springframework.boot.docker.compose.service.connection.jdbc;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

/**
 * {@link JdbcCredentials} derived from the labels of a {@link RunningService}.
 *
 * @author Andy Wilkinson
 */
class RunningServiceLabelsJdbcCredentials implements JdbcCredentials, EnvironmentAware {

	private final PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper(
			SystemPropertyUtils.PLACEHOLDER_PREFIX, SystemPropertyUtils.PLACEHOLDER_SUFFIX,
			SystemPropertyUtils.VALUE_SEPARATOR, true);

	private final String username;

	private final String password;

	private volatile Environment environment;

	public RunningServiceLabelsJdbcCredentials(String username, String password) {
		this.username = username;
		this.password = password;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public String getUsername() {
		return this.propertyPlaceholderHelper.replacePlaceholders(this.username, this.environment::getProperty);
	}

	@Override
	public String getPassword() {
		return this.propertyPlaceholderHelper.replacePlaceholders(this.password, this.environment::getProperty);
	}

	static RunningServiceLabelsJdbcCredentials from(RunningService service) {
		String username = service.labels().get("org.springframework.boot.jdbc.username");
		if (username != null) {
			return new RunningServiceLabelsJdbcCredentials(username,
					service.labels().get("org.springframework.boot.jdbc.password"));
		}
		return null;
	}

}