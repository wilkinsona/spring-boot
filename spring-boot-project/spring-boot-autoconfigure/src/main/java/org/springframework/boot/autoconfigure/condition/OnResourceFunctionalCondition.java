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

package org.springframework.boot.autoconfigure.condition;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;

/**
 * {@link FunctionalCondition} that checks for specific resources.
 *
 * @author Dave Syer
 * @see ConditionalOnResource
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class OnResourceFunctionalCondition extends SpringBootFunctionalCondition {

	private final List<String> resourceLocations;

	OnResourceFunctionalCondition(String location, List<String> resourceLocations) {
		super(location);
		this.resourceLocations = resourceLocations;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context) {
		ResourceLoader loader = context.getResourceLoader();
		List<String> missing = new ArrayList<>();
		for (String resourceLocation : this.resourceLocations) {
			String resource = context.getEnvironment().resolvePlaceholders(resourceLocation);
			if (!loader.getResource(resource).exists()) {
				missing.add(resourceLocation);
			}
		}
		if (!missing.isEmpty()) {
			return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnResource.class)
					.didNotFind("resource", "resources").items(Style.QUOTE, missing));
		}
		return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnResource.class)
				.found("location", "locations").items(this.resourceLocations));
	}

}
