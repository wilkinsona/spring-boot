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
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.condition.OnPropertyFunctionalCondition.PropertySpec;
import org.springframework.context.annotation.ConditionContext;

/**
 * @author awilkinson
 */
public class Conditional implements FunctionalCondition {

	private final String location;

	private final List<FunctionalCondition> conditions = new ArrayList<>();

	private Conditional(String location) {
		this.location = location;
	}

	public static Conditional conditional(String location) {
		return new Conditional(location);
	}

	public Conditional onClass(Supplier<Class<?>> clazz) {
		return on(new OnClassFunctionalCondition(this.location, clazz));
	}

	public Conditional onProperty(Consumer<PropertySpec> configurer) {
		PropertySpec spec = new PropertySpec();
		configurer.accept(spec);
		OnPropertyFunctionalCondition condition = new OnPropertyFunctionalCondition(this.location, Arrays.asList(spec));
		return on(condition);
	}

	public Conditional onBean(Consumer<OnBeanFunctionalCondition> configurer) {
		OnBeanFunctionalCondition condition = new OnBeanFunctionalCondition(this.location);
		configurer.accept(condition);
		return on(condition);
	}

	public Conditional on(FunctionalCondition condition) {
		return this;
	}

	@Override
	public boolean matches(ConditionContext context) {
		for (FunctionalCondition condition : this.conditions) {
			if (!condition.matches(context)) {
				return false;
			}
		}
		return true;
	}

}
