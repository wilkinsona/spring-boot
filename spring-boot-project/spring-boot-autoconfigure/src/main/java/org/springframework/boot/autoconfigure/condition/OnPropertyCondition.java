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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.OnPropertyFunctionalCondition.PropertySpec;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Adapter that enables annotation-based usage of {@link OnPropertyFunctionalCondition}.
 *
 * @author Maciej Walkowiak
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @see ConditionalOnProperty
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
class OnPropertyCondition extends AnnotationCondition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		List<AnnotationAttributes> allAnnotationAttributes = annotationAttributesFromMultiValueMap(
				metadata.getAllAnnotationAttributes(ConditionalOnProperty.class.getName()));
		List<PropertySpec> propertySpecs = new ArrayList<>();
		for (AnnotationAttributes annotationAttributes : allAnnotationAttributes) {
			propertySpecs.add(specFor(annotationAttributes));
		}
		return new OnPropertyFunctionalCondition(getLocation(metadata), propertySpecs).matches(context);
	}

	private List<AnnotationAttributes> annotationAttributesFromMultiValueMap(
			MultiValueMap<String, Object> multiValueMap) {
		List<Map<String, Object>> maps = new ArrayList<>();
		multiValueMap.forEach((key, value) -> {
			for (int i = 0; i < value.size(); i++) {
				Map<String, Object> map;
				if (i < maps.size()) {
					map = maps.get(i);
				}
				else {
					map = new HashMap<>();
					maps.add(map);
				}
				map.put(key, value.get(i));
			}
		});
		List<AnnotationAttributes> annotationAttributes = new ArrayList<>(maps.size());
		for (Map<String, Object> map : maps) {
			annotationAttributes.add(AnnotationAttributes.fromMap(map));
		}
		return annotationAttributes;
	}

	private PropertySpec specFor(AnnotationAttributes annotationAttributes) {
		String prefix = annotationAttributes.getString("prefix").trim();
		if (StringUtils.hasText(prefix) && !prefix.endsWith(".")) {
			prefix = prefix + ".";
		}
		return new PropertySpec(prefix, annotationAttributes.getString("havingValue"), getNames(annotationAttributes),
				annotationAttributes.getBoolean("matchIfMissing"));
	}

	private String[] getNames(Map<String, Object> annotationAttributes) {
		String[] value = (String[]) annotationAttributes.get("value");
		String[] name = (String[]) annotationAttributes.get("name");
		Assert.state(value.length > 0 || name.length > 0,
				"The name or value attribute of @ConditionalOnProperty must be specified");
		Assert.state(value.length == 0 || name.length == 0,
				"The name and value attributes of @ConditionalOnProperty are exclusive");
		return (value.length > 0) ? value : name;
	}

}
