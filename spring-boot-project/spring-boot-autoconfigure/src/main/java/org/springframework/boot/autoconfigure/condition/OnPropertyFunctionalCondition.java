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
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.StringUtils;

/**
 * {@link FunctionalCondition} that checks if properties are defined in environment.
 *
 * @author Maciej Walkowiak
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
class OnPropertyFunctionalCondition extends SpringBootFunctionalCondition {

	private final List<PropertySpec> propertySpecs;

	public OnPropertyFunctionalCondition(String location, List<PropertySpec> propertySpecs) {
		super(location);
		this.propertySpecs = propertySpecs;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context) {
		List<ConditionMessage> noMatch = new ArrayList<>();
		List<ConditionMessage> match = new ArrayList<>();
		for (PropertySpec spec : this.propertySpecs) {
			ConditionOutcome outcome = determineOutcome(spec, context.getEnvironment());
			(outcome.isMatch() ? match : noMatch).add(outcome.getConditionMessage());
		}
		if (!noMatch.isEmpty()) {
			return ConditionOutcome.noMatch(ConditionMessage.of(noMatch));
		}
		return ConditionOutcome.match(ConditionMessage.of(match));
	}

	private ConditionOutcome determineOutcome(PropertySpec spec, PropertyResolver resolver) {
		List<String> missingProperties = new ArrayList<>();
		List<String> nonMatchingProperties = new ArrayList<>();
		spec.collectProperties(resolver, missingProperties, nonMatchingProperties);
		if (!missingProperties.isEmpty()) {
			return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnProperty.class, spec)
					.didNotFind("property", "properties").items(Style.QUOTE, missingProperties));
		}
		if (!nonMatchingProperties.isEmpty()) {
			return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnProperty.class, spec)
					.found("different value in property", "different value in properties")
					.items(Style.QUOTE, nonMatchingProperties));
		}
		return ConditionOutcome
				.match(ConditionMessage.forCondition(ConditionalOnProperty.class, spec).because("matched"));
	}

	public static class PropertySpec {

		private String prefix;

		private String havingValue;

		private final List<String> names = new ArrayList<>();

		private boolean matchIfMissing;

		PropertySpec() {

		}

		PropertySpec(String prefix, String havingValue, String[] names, boolean matchIfMissing) {
			this.prefix = (StringUtils.hasText(prefix) && !prefix.endsWith(".")) ? prefix + "." : prefix;
			this.havingValue = havingValue;
			this.names.addAll(Arrays.asList(names));
			this.matchIfMissing = matchIfMissing;
		}

		public PropertySpec prefix(String prefix) {
			this.prefix = prefix;
			return this;
		}

		public PropertySpec name(String name) {
			this.names.add(name);
			return this;
		}

		public PropertySpec havingValue(String value) {
			this.havingValue = value;
			return this;
		}

		public PropertySpec matchIfMissing() {
			this.matchIfMissing = true;
			return this;
		}

		private void collectProperties(PropertyResolver resolver, List<String> missing, List<String> nonMatching) {
			for (String name : this.names) {
				String key = this.prefix + name;
				if (resolver.containsProperty(key)) {
					if (!isMatch(resolver.getProperty(key), this.havingValue)) {
						nonMatching.add(name);
					}
				}
				else {
					if (!this.matchIfMissing) {
						missing.add(name);
					}
				}
			}
		}

		private boolean isMatch(String value, String requiredValue) {
			if (StringUtils.hasLength(requiredValue)) {
				return requiredValue.equalsIgnoreCase(value);
			}
			return !"false".equalsIgnoreCase(value);
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append("(");
			result.append(this.prefix);
			if (this.names.size() == 1) {
				result.append(this.names.get(0));
			}
			else {
				result.append("[");
				result.append(StringUtils.collectionToCommaDelimitedString(this.names));
				result.append("]");
			}
			if (StringUtils.hasLength(this.havingValue)) {
				result.append("=").append(this.havingValue);
			}
			result.append(")");
			return result.toString();
		}

	}

}
