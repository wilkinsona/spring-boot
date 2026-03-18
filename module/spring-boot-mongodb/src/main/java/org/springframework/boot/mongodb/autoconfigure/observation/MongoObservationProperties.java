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

package org.springframework.boot.mongodb.autoconfigure.observation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for MongoDB observability.
 *
 * @author Andy Wilkinson
 * @since 4.1.0
 */
@ConfigurationProperties("management.observations.mongodb")
public class MongoObservationProperties {

	/**
	 * Maximum length of command payloads captured in tracing spans.
	 */
	private int maxQueryTextLength = Integer.MAX_VALUE;

	/**
	 * Whether to enable command payload tracing.
	 */
	private boolean enableCommandPayloadTracing;

	public int getMaxQueryTextLength() {
		return this.maxQueryTextLength;
	}

	public void setMaxQueryTextLength(int maxQueryTextLength) {
		this.maxQueryTextLength = maxQueryTextLength;
	}

	public boolean isEnableCommandPayloadTracing() {
		return this.enableCommandPayloadTracing;
	}

	public void setEnableCommandPayloadTracing(boolean enableCommandPayloadTracing) {
		this.enableCommandPayloadTracing = enableCommandPayloadTracing;
	}

}
