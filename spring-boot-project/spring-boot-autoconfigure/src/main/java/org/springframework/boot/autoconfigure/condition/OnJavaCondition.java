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

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnJava.Range;
import org.springframework.boot.system.JavaVersion;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Adapter that enables annotation-based usage of {@link OnJavaFunctionalCondition}.
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 * @see ConditionalOnJava
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class OnJavaCondition extends AnnotationCondition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnJava.class.getName());
		Range range = (Range) attributes.get("range");
		JavaVersion version = (JavaVersion) attributes.get("value");
		return new OnJavaFunctionalCondition(getLocation(metadata), range, version).matches(context);
	}

}
