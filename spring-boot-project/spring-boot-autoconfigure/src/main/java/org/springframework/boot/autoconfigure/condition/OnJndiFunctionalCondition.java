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

import java.util.List;

import javax.naming.NamingException;

import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.util.StringUtils;

/**
 * {@link FunctionalCondition} that checks for JNDI locations.
 *
 * @author Phillip Webb
 */
@Order(Ordered.LOWEST_PRECEDENCE - 20)
class OnJndiFunctionalCondition extends SpringBootFunctionalCondition {

	private final List<String> jndiLocations;

	OnJndiFunctionalCondition(String location, List<String> jndiLocations) {
		super(location);
		this.jndiLocations = jndiLocations;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context) {
		try {
			return getMatchOutcome();
		}
		catch (NoClassDefFoundError ex) {
			return ConditionOutcome
					.noMatch(ConditionMessage.forCondition(ConditionalOnJndi.class).because("JNDI class not found"));
		}
	}

	private ConditionOutcome getMatchOutcome() {
		if (!isJndiAvailable()) {
			return ConditionOutcome
					.noMatch(ConditionMessage.forCondition(ConditionalOnJndi.class).notAvailable("JNDI environment"));
		}
		if (this.jndiLocations.isEmpty()) {
			return ConditionOutcome
					.match(ConditionMessage.forCondition(ConditionalOnJndi.class).available("JNDI environment"));
		}
		JndiLocator locator = getJndiLocator(this.jndiLocations);
		String location = locator.lookupFirstLocation();
		String details = "(" + StringUtils.collectionToCommaDelimitedString(this.jndiLocations) + ")";
		if (location != null) {
			return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnJndi.class, details)
					.foundExactly("\"" + location + "\""));
		}
		return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnJndi.class, details)
				.didNotFind("any matching JNDI location").atAll());
	}

	protected boolean isJndiAvailable() {
		return JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable();
	}

	protected JndiLocator getJndiLocator(List<String> jndiLocations) {
		return new JndiLocator(jndiLocations);
	}

	protected static class JndiLocator extends JndiLocatorSupport {

		private List<String> locations;

		public JndiLocator(List<String> locations) {
			this.locations = locations;
		}

		public String lookupFirstLocation() {
			for (String location : this.locations) {
				try {
					lookup(location);
					return location;
				}
				catch (NamingException ex) {
					// Swallow and continue
				}
			}
			return null;
		}

	}

}
