/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.hateoas;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.hateoas.HypermediaReactiveAutoConfiguration.HypermediaConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.MockReactiveWebServerFactory;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.LinkDiscoverer;
import org.springframework.hateoas.client.LinkDiscoverers;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.mediatype.hal.HalLinkDiscoverer;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.support.WebStack;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HypermediaReactiveAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class HypermediaReactiveAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(WebFluxAutoConfiguration.class, HypermediaReactiveAutoConfiguration.class))
			.withBean(MockReactiveWebServerFactory.class, MockReactiveWebServerFactory::new);

	@Test
	void whenAutoConfigurationIsActiveThenLinkDiscoverersIsCreated() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(LinkDiscoverers.class);
			Optional<LinkDiscoverer> optionalDiscoverer = context.getBean(LinkDiscoverers.class)
					.getLinkDiscovererFor(MediaTypes.HAL_JSON);
			assertThat(optionalDiscoverer).containsInstanceOf(HalLinkDiscoverer.class);
		});
	}

	@Test
	void whenAutoConfigurationIsActiveThenEntityLinksIsCreated() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(EntityLinks.class));
	}

	@Test
	void whenEnableHypermediaSupportIsDeclaredManuallyThenAutoConfigurationBacksOff() {
		this.contextRunner.withUserConfiguration(EnableHypermediaSupportConfig.class)
				.run((context) -> assertThat(context).doesNotHaveBean(HypermediaConfiguration.class));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableHypermediaSupport(type = HypermediaType.HAL, stacks = WebStack.WEBFLUX)
	static class EnableHypermediaSupportConfig {

	}

}
