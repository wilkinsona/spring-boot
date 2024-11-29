/*
 * Copyright 2025 the original author or authors.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * A report of a project's deprecations.
 *
 * @author Andy Wilkinson
 */
public final class DeprecationReport {

	private final List<TypeDeprecation> types;

	private final List<FieldDeprecation> fields;

	private final List<ConstructorDeprecation> constructors;

	private final List<MethodDeprecation> methods;

	private final List<EnumConstantDeprecation> enumConstants;

	private final List<AnnotationMemberDeprecation> annotationMembers;

	@JsonCreator
	private DeprecationReport(List<TypeDeprecation> types, List<FieldDeprecation> fields,
			List<ConstructorDeprecation> constructors, List<MethodDeprecation> methods,
			List<EnumConstantDeprecation> enumConstants, List<AnnotationMemberDeprecation> annotationMembers) {
		this.types = types;
		this.fields = fields;
		this.constructors = constructors;
		this.methods = methods;
		this.enumConstants = enumConstants;
		this.annotationMembers = annotationMembers;
	}

	public List<TypeDeprecation> getTypes() {
		return this.types;
	}

	public List<FieldDeprecation> getFields() {
		return this.fields;
	}

	public List<ConstructorDeprecation> getConstructors() {
		return this.constructors;
	}

	public List<MethodDeprecation> getMethods() {
		return this.methods;
	}

	public List<EnumConstantDeprecation> getEnumConstants() {
		return this.enumConstants;
	}

	public List<AnnotationMemberDeprecation> getAnnotationMembers() {
		return this.annotationMembers;
	}

	static DeprecationReport from(Map<Class<? extends Deprecation>, List<Deprecation>> deprecations) {
		return new DeprecationReport(deprecationsOfType(TypeDeprecation.class, deprecations),
				deprecationsOfType(FieldDeprecation.class, deprecations),
				deprecationsOfType(ConstructorDeprecation.class, deprecations),
				deprecationsOfType(MethodDeprecation.class, deprecations),
				deprecationsOfType(EnumConstantDeprecation.class, deprecations),
				deprecationsOfType(AnnotationMemberDeprecation.class, deprecations));
	}

	private static <T extends Deprecation> List<T> deprecationsOfType(Class<T> type,
			Map<Class<? extends Deprecation>, List<Deprecation>> deprecations) {
		return asListOfType(type, deprecations.get(type));
	}

	private static <T extends Deprecation> List<T> asListOfType(Class<T> type, List<Deprecation> deprecations) {
		return (deprecations != null) ? deprecations.stream().map(type::cast).toList() : Collections.emptyList();
	}

}
