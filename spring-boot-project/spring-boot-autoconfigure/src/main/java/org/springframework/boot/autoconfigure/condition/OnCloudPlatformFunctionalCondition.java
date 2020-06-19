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

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.ConditionContext;

/**
 * {@link FunctionalCondition} that checks for a required {@link CloudPlatform}.
 *
 * @author Madhura Bhave
 */
class OnCloudPlatformFunctionalCondition extends SpringBootFunctionalCondition {

	private final CloudPlatform cloudPlatform;

	public OnCloudPlatformFunctionalCondition(String location, CloudPlatform cloudPlatform) {
		super(location);
		this.cloudPlatform = cloudPlatform;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context) {
		String name = this.cloudPlatform.name();
		ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnCloudPlatform.class);
		if (this.cloudPlatform.isActive(context.getEnvironment())) {
			return ConditionOutcome.match(message.foundExactly(name));
		}
		return ConditionOutcome.noMatch(message.didNotFind(name).atAll());
	}

}
