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

import org.junit.jupiter.api.Test;

import org.springframework.web.servlet.DispatcherServlet;

import static org.springframework.boot.autoconfigure.condition.Conditional.conditional;

/**
 * @author awilkinson
 */
public class ConditionalTests {

	@Test
	void example() {
		conditional("ConditionalTests#example") // Location used in evaluation messages
				.onClass(() -> DispatcherServlet.class) // Supplier to allow type-safe class references
				.onBean((bean) -> bean.ofType(() -> DispatcherServlet.class)) // Supplier to allow type-safe class references
				.onProperty((property) -> property.prefix("management.cloudfoundry").name("enabled").matchIfMissing()) //
				.on((context) -> true); // Functional interface to plug in any logic you want
	}

}
