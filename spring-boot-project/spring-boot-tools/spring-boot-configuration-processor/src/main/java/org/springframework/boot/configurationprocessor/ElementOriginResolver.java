/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.configurationprocessor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.springframework.boot.configurationprocessor.ast.TreesElementOriginResolver;

/**
 * Resolver for the origin of a {@link TypeElement}.
 *
 * @author Andy Wilkinson
 * @since 2.6.11
 */
@FunctionalInterface
public interface ElementOriginResolver {

	/**
	 * Implementation of {@link ElementOriginResolver} that always returns an unknown
	 * origin.
	 */
	ElementOriginResolver UNKNOWN = (element) -> ElementOrigin.UNKNOWN;

	/**
	 * Resolves the origin of the given {@code element};.
	 * @param element the element
	 * @return the origin of the element, never {@code null}
	 */
	ElementOrigin resolveOrigin(Element element);

	/**
	 * Creates a new {@link ElementOriginResolver} for the given {@code environment}.
	 * @param environment the processing environment
	 * @return the resolver for the environment
	 */
	static ElementOriginResolver forEnvironment(ProcessingEnvironment environment) {
		try {
			return new TreesElementOriginResolver(environment);
		}
		catch (Exception ex) {
			return UNKNOWN;
		}
	}

}
