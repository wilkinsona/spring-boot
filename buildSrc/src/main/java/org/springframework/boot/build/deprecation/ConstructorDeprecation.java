/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.build.deprecation;

import java.util.List;

/**
 * Deprecation of a constructor.
 *
 * @author Andy Wilkinson
 */
public class ConstructorDeprecation extends Deprecation {

	private final String type;

	private final List<String> parameters;

	ConstructorDeprecation(String type, List<String> parameters, String since, String removal, String details) {
		super(since, removal, details);
		this.type = type;
		this.parameters = parameters;
	}

	public String getType() {
		return this.type;
	}

	public List<String> getParameters() {
		return this.parameters;
	}

	@Override
	public String toString() {
		return this.type + "(" + String.join(", ", this.parameters) + ")";
	}

}
