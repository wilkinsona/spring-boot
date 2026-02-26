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

package org.springframework.boot.micrometer.metrics.autoconfigure.jvm;

import java.lang.management.MemoryPoolMXBean;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.MeterConvention;
import io.micrometer.core.instrument.binder.SimpleMeterConvention;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmCompilationMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.jvm.convention.JvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmThreadMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmThreadMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmThreadMeterConventions;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.micrometer.observation.autoconfigure.condition.SemanticConventions;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JvmMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
class JvmMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(MeterRegistry.class, () -> new SimpleMeterRegistry())
		.withConfiguration(AutoConfigurations.of(JvmMetricsAutoConfiguration.class));

	@Test
	void autoConfiguresJvmMetrics() {
		this.contextRunner.run(assertMetricsBeans());
	}

	@Test
	void allowsCustomJvmGcMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmGcMetricsConfiguration.class)
			.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customJvmGcMetrics")));
	}

	@Test
	void allowsCustomJvmHeapPressureMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmHeapPressureMetricsConfiguration.class)
			.run(assertMetricsBeans()
				.andThen((context) -> assertThat(context).hasBean("customJvmHeapPressureMetrics")));
	}

	@Test
	void allowsCustomJvmMemoryMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmMemoryMetricsConfiguration.class)
			.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customJvmMemoryMetrics")));
	}

	@EnumSource
	@ParameterizedTest
	void allowCustomJvmMemoryMeterConventionsToBeUsed(SemanticConventions conventions) {
		JvmMemoryMeterConventions jvmMemoryMeterConventions = mock(JvmMemoryMeterConventions.class);
		this.contextRunner.withPropertyValues("management.observations.conventions=" + conventions.name())
			.withBean("customConventions", JvmMemoryMeterConventions.class, () -> jvmMemoryMeterConventions)
			.run((context) -> assertThat(context).hasSingleBean(JvmMemoryMetrics.class)
				.getBean(JvmMemoryMetrics.class)
				.hasFieldOrPropertyWithValue("conventions", jvmMemoryMeterConventions));
	}

	@Test
	void allowsCustomJvmThreadMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmThreadMetricsConfiguration.class)
			.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customJvmThreadMetrics")));
	}

	@EnumSource
	@ParameterizedTest
	void allowCustomJvmThreadMeterConventionsToBeUsed(SemanticConventions conventions) {
		JvmThreadMeterConventions jvmThreadMeterConventions = mock(JvmThreadMeterConventions.class);
		this.contextRunner.withPropertyValues("management.observations.conventions=" + conventions.name())
			.withBean("customConventions", JvmThreadMeterConventions.class, () -> jvmThreadMeterConventions)
			.run((context) -> assertThat(context).hasSingleBean(JvmThreadMetrics.class)
				.getBean(JvmThreadMetrics.class)
				.hasFieldOrPropertyWithValue("conventions", jvmThreadMeterConventions));
	}

	@Test
	void allowsCustomClassLoaderMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomClassLoaderMetricsConfiguration.class)
			.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customClassLoaderMetrics")));
	}

	@EnumSource
	@ParameterizedTest
	void allowCustomJvmClassLoadingMeterConventionsToBeUsed(SemanticConventions conventions) {
		JvmClassLoadingMeterConventions jvmClassLoadingMeterConventions = mock(JvmClassLoadingMeterConventions.class);
		this.contextRunner.withPropertyValues("management.observations.conventions=" + conventions.name())
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.withBean("customConventions", JvmClassLoadingMeterConventions.class, () -> jvmClassLoadingMeterConventions)
			.run((context) -> assertThat(context).hasSingleBean(ClassLoaderMetrics.class)
				.getBean(ClassLoaderMetrics.class)
				.hasFieldOrPropertyWithValue("conventions", jvmClassLoadingMeterConventions));
	}

	@Test
	void allowsCustomJvmInfoMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmInfoMetricsConfiguration.class)
			.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customJvmInfoMetrics")));
	}

	@Test
	void allowsCustomJvmCompilationMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmCompilationMetricsConfiguration.class)
			.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customJvmCompilationMetrics")));
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void autoConfiguresJvmMetricsWithVirtualThreadsMetrics() {
		this.contextRunner.run(assertMetricsBeans()
			.andThen((context) -> assertThat(context).hasSingleBean(getVirtualThreadMetricsClass())));
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void allowCustomVirtualThreadMetricsToBeUsed() {
		Class<MeterBinder> virtualThreadMetricsClass = getVirtualThreadMetricsClass();
		this.contextRunner
			.withBean("customVirtualThreadMetrics", virtualThreadMetricsClass,
					() -> BeanUtils.instantiateClass(virtualThreadMetricsClass))
			.run(assertMetricsBeans()
				.andThen((context) -> assertThat(context).hasSingleBean(getVirtualThreadMetricsClass())
					.hasBean("customVirtualThreadMetrics")));
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void shouldRegisterVirtualThreadMetricsRuntimeHints() {
		RuntimeHints hints = new RuntimeHints();
		new JvmMetricsAutoConfiguration.VirtualThreadMetricsRuntimeHintsRegistrar().registerHints(hints,
				getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(TypeReference.of(getVirtualThreadMetricsClass()))
			.withMemberCategories(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)).accepts(hints);
	}

	@Test
	void registersMicrometerConventionsByDefault() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(MicrometerJvmMemoryMeterConventions.class);
			assertThat(context).hasSingleBean(MicrometerJvmClassLoadingMeterConventions.class);
			assertThat(context).hasSingleBean(MicrometerJvmThreadMeterConventions.class);
			assertThat(context).doesNotHaveBean(OpenTelemetryJvmMemoryMeterConventions.class);
			assertThat(context).doesNotHaveBean(OpenTelemetryJvmClassLoadingMeterConventions.class);
			assertThat(context).doesNotHaveBean(OpenTelemetryJvmThreadMeterConventions.class);
		});
	}

	@Test
	void registersOpenTelemetryConventionsWhenConventionsSetToOpenTelemetry() {
		this.contextRunner.withPropertyValues("management.observations.conventions=opentelemetry").run((context) -> {
			assertThat(context).hasSingleBean(JvmMemoryMeterConventions.class)
				.hasSingleBean(OpenTelemetryJvmMemoryMeterConventions.class);
			assertThat(context).hasSingleBean(JvmClassLoadingMeterConventions.class)
				.hasSingleBean(OpenTelemetryJvmClassLoadingMeterConventions.class);
			assertThat(context).hasSingleBean(JvmThreadMeterConventions.class)
				.hasSingleBean(OpenTelemetryJvmThreadMeterConventions.class);
		});
	}

	private ContextConsumer<AssertableApplicationContext> assertMetricsBeans() {
		return (context) -> assertThat(context).hasSingleBean(JvmGcMetrics.class)
			.hasSingleBean(JvmHeapPressureMetrics.class)
			.hasSingleBean(JvmMemoryMetrics.class)
			.hasSingleBean(JvmThreadMetrics.class)
			.hasSingleBean(ClassLoaderMetrics.class)
			.hasSingleBean(JvmInfoMetrics.class)
			.hasSingleBean(JvmCompilationMetrics.class);
	}

	@SuppressWarnings("unchecked")
	private static Class<MeterBinder> getVirtualThreadMetricsClass() {
		return (Class<MeterBinder>) ClassUtils
			.resolveClassName("io.micrometer.java21.instrument.binder.jdk.VirtualThreadMetrics", null);
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmGcMetricsConfiguration {

		@Bean
		JvmGcMetrics customJvmGcMetrics() {
			return new JvmGcMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmHeapPressureMetricsConfiguration {

		@Bean
		JvmHeapPressureMetrics customJvmHeapPressureMetrics() {
			return new JvmHeapPressureMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmMemoryMetricsConfiguration {

		@Bean
		JvmMemoryMetrics customJvmMemoryMetrics() {
			return new JvmMemoryMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmThreadMetricsConfiguration {

		@Bean
		JvmThreadMetrics customJvmThreadMetrics() {
			return new JvmThreadMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomClassLoaderMetricsConfiguration {

		@Bean
		ClassLoaderMetrics customClassLoaderMetrics() {
			return new ClassLoaderMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmInfoMetricsConfiguration {

		@Bean
		JvmInfoMetrics customJvmInfoMetrics() {
			return new JvmInfoMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmCompilationMetricsConfiguration {

		@Bean
		JvmCompilationMetrics customJvmCompilationMetrics() {
			return new JvmCompilationMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmMemoryMeterConventionsConfiguration {

		@Bean
		JvmMemoryMeterConventions customJvmMemoryMeterConventions() {
			return new JvmMemoryMeterConventions() {
				@Override
				public MeterConvention<MemoryPoolMXBean> getMemoryUsedConvention() {
					return new SimpleMeterConvention<>("my.memory.used");
				}

				@Override
				public MeterConvention<MemoryPoolMXBean> getMemoryCommittedConvention() {
					return new SimpleMeterConvention<>("my.memory.committed");
				}

				@Override
				public MeterConvention<MemoryPoolMXBean> getMemoryMaxConvention() {
					return new SimpleMeterConvention<>("my.memory.max");
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmClassLoadingMeterConventionsConfiguration {

		@Bean
		JvmClassLoadingMeterConventions customJvmClassLoadingMeterConventions() {
			return new JvmClassLoadingMeterConventions() {
				@Override
				public MeterConvention<Object> loadedConvention() {
					return new SimpleMeterConvention<>("my.classes.loaded");
				}

				@Override
				public MeterConvention<Object> unloadedConvention() {
					return new SimpleMeterConvention<>("my.classes.unloaded");
				}

				@Override
				public MeterConvention<Object> currentClassCountConvention() {
					return new SimpleMeterConvention<>("my.classes.current");
				}
			};
		}

	}

}
