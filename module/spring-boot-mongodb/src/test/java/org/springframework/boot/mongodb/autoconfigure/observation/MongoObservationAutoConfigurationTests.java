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

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.internal.MongoClientImpl;
import com.mongodb.observability.ObservabilitySettings;
import com.mongodb.observability.micrometer.MicrometerObservabilitySettings;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoObservationAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class MongoObservationAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(MongoAutoConfiguration.class, MongoObservationAutoConfiguration.class));

	@Test
	void whenThereIsAnObservationRegistryThenObservabilitySettingsAreAdded() {
		this.contextRunner.withBean(ObservationRegistry.class, () -> ObservationRegistry.NOOP).run((context) -> {
			assertThat(context).hasSingleBean(MicrometerObservabilitySettings.class);
			assertThat(getMongoClientSettings(context)).extracting(MongoClientSettings::getObservabilitySettings)
				.isSameAs(context.getBean(MicrometerObservabilitySettings.class));
		});
	}

	@Test
	void whenObservabilityIsDisabledThenObservabilitySettingsAreNotAdded() {
		this.contextRunner.withBean(ObservationRegistry.class, () -> ObservationRegistry.NOOP)
			.withPropertyValues("management.observations.mongodb.enabled=false")
			.run((context) -> {
				assertThat(context).doesNotHaveBean(ObservabilitySettings.class);
				assertThat(getMongoClientSettings(context)).extracting(MongoClientSettings::getObservabilitySettings)
					.isNull();
			});
	}

	@Test
	void whenMongoMetricsCommandListenerIsDefinedThenObservabilitySettingsAreNotAdded() {
		this.contextRunner.withBean(ObservationRegistry.class, () -> ObservationRegistry.NOOP)
			.withBean(MongoMetricsCommandListener.class,
					() -> new MongoMetricsCommandListener(new SimpleMeterRegistry()))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(ObservabilitySettings.class);
				assertThat(getMongoClientSettings(context)).extracting(MongoClientSettings::getObservabilitySettings)
					.isNull();
			});
	}

	@Test
	void whenObservabilitySettingsAreDefinedThenMongoClientSettingsUsesThem() {
		MicrometerObservabilitySettings observabilitySettings = MicrometerObservabilitySettings.builder()
			.observationRegistry(ObservationRegistry.NOOP)
			.build();
		this.contextRunner.withBean(ObservationRegistry.class, () -> ObservationRegistry.NOOP)
			.withBean(ObservabilitySettings.class, () -> observabilitySettings)
			.run((context) -> {
				assertThat(context).hasSingleBean(ObservabilitySettings.class);
				assertThat(getMongoClientSettings(context)).extracting(MongoClientSettings::getObservabilitySettings)
					.isSameAs(observabilitySettings);
			});
	}

	@Test
	void whenCommandPayloadTracingIsEnabledItIsReflectedInObservabilitySettings() {
		this.contextRunner.withBean(ObservationRegistry.class, () -> ObservationRegistry.NOOP)
			.withPropertyValues("management.observations.mongodb.enable-command-payload-tracing=true")
			.run((context) -> {
				assertThat(context).hasSingleBean(MicrometerObservabilitySettings.class);
				ObservabilitySettings observabilitySettings = getMongoClientSettings(context)
					.getObservabilitySettings();
				assertThat(observabilitySettings).isNotNull();
				assertThat(observabilitySettings).isInstanceOf(MicrometerObservabilitySettings.class);
				assertThat(((MicrometerObservabilitySettings) observabilitySettings).isEnableCommandPayloadTracing())
					.isTrue();
			});
	}

	@Test
	void whenMaxQueryLengthIsCustomizedItIsReflectedInObservabilitySettings() {
		this.contextRunner.withBean(ObservationRegistry.class, () -> ObservationRegistry.NOOP)
			.withPropertyValues("management.observations.mongodb.max-query-text-length=1000")
			.run((context) -> {
				assertThat(context).hasSingleBean(MicrometerObservabilitySettings.class);
				ObservabilitySettings observabilitySettings = getMongoClientSettings(context)
					.getObservabilitySettings();
				assertThat(observabilitySettings).isNotNull();
				assertThat(observabilitySettings).isInstanceOf(MicrometerObservabilitySettings.class);
				assertThat(((MicrometerObservabilitySettings) observabilitySettings).getMaxQueryTextLength())
					.isEqualTo(1000);
			});
	}

	private MongoClientSettings getMongoClientSettings(AssertableApplicationContext context) {
		return ((MongoClientImpl) context.getBean(MongoClient.class)).getSettings();
	}

}
