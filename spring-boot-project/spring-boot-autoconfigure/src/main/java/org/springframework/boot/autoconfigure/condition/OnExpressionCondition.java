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

import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Adapter that enables annotation-based usage of {@link OnExpressionFunctionalCondition}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @see ConditionalOnExpression
 */
@Order(Ordered.LOWEST_PRECEDENCE - 20)
class OnExpressionCondition extends AnnotationCondition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String expression = (String) metadata.getAnnotationAttributes(ConditionalOnExpression.class.getName())
				.get("value");
		return new OnExpressionFunctionalCondition(getLocation(metadata), expression).matches(context);
	}

}
