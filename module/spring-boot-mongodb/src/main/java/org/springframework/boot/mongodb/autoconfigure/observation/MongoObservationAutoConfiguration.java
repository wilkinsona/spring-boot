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

import com.mongodb.observability.ObservabilitySettings;
import com.mongodb.observability.micrometer.MicrometerObservabilitySettings;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.mongodb.autoconfigure.metrics.MongoMetricsAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Mongo observation.
 *
 * @author Andy Wilkinson
 * @since 4.1.0
 */
@AutoConfiguration(after = MongoMetricsAutoConfiguration.class,
		afterName = "org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration")
@ConditionalOnBean(ObservationRegistry.class)
@ConditionalOnMissingBean(type = "io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener")
@ConditionalOnClass(MicrometerObservabilitySettings.class)
@ConditionalOnBooleanProperty(name = "management.observations.mongodb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(MongoObservationProperties.class)
public final class MongoObservationAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ObservabilitySettings.class)
	MicrometerObservabilitySettings micrometerObservabilitySettings(ObservationRegistry registry,
			MongoObservationProperties properties) {
		return MicrometerObservabilitySettings.builder()
			.observationRegistry(registry)
			.maxQueryTextLength(properties.getMaxQueryTextLength())
			.enableCommandPayloadTracing(properties.isEnableCommandPayloadTracing())
			.build();
	}

	@Bean
	MongoClientSettingsBuilderCustomizer mongoMetricsCommandListenerClientSettingsBuilderCustomizer(
			ObservabilitySettings observabilitySettings) {
		return (clientSettingsBuilder) -> clientSettingsBuilder.observabilitySettings(observabilitySettings);
	}

}
