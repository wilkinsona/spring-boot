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

package org.springframework.boot.configurationprocessor.ast;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

import org.springframework.boot.configurationprocessor.ElementOrigin;
import org.springframework.boot.configurationprocessor.ElementOriginResolver;

/**
 * {@link Trees}-based implementation of {@link ElementOriginResolver}.
 *
 * @author Andy Wilkinson
 * @since 2.6.11
 */
public class TreesElementOriginResolver implements ElementOriginResolver {

	private final Trees trees;

	public TreesElementOriginResolver(ProcessingEnvironment environment) throws Exception {
		this.trees = Trees.instance(environment);
	}

	@Override
	public ElementOrigin resolveOrigin(Element element) {
		try {
			return (this.trees.getTree(element) != null) ? ElementOrigin.SOURCE : ElementOrigin.CLASS;
		}
		catch (Exception ex) {
			return ElementOrigin.UNKNOWN;
		}
	}

}
