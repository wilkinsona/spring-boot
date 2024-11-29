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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocInlineTag;

/**
 * A deprecation.
 *
 * @author Andy Wilkinson
 */
public abstract class Deprecation {

	private static final Pattern PATTERN = Pattern
		.compile("since ([0-9]\\.[0-9]\\.[0-9]+)(?: for removal in ([0-9]\\.[0-9]\\.[0-9]+))?.*");

	private final String since;

	private final String removal;

	private final String details;

	protected Deprecation(String since, String removal, String details) {
		this.since = since;
		this.removal = removal;
		this.details = details;
	}

	public String getSince() {
		return this.since;
	}

	public String getRemoval() {
		return this.removal;
	}

	public String getDetails() {
		return this.details;
	}

	static Deprecation from(JavadocBlockTag deprecatedTag, Node commented) {
		String details = deprecatedTag.getContent().getElements().stream().map((element) -> {
			if (element instanceof JavadocInlineTag tag && "link".equals(tag.getName())) {
				return tag.getContent().trim();
			}
			else {
				return element.toText().replace('\n', ' ');
			}
		}).collect(Collectors.joining());
		Matcher matcher = PATTERN.matcher(details);
		String since = "unknown";
		String removal = "unknown";
		if (matcher.matches()) {
			since = matcher.group(1);
			if (matcher.group(2) != null) {
				removal = matcher.group(2);
				details = details.substring(matcher.end(2));
			}
			else {
				details = details.substring(matcher.end(1));
			}
		}
		return from(commented, since, removal, details.trim());
	}

	private static Deprecation from(Node node, String since, String removal, String details) {
		if (node instanceof TypeDeclaration type) {
			return new TypeDeprecation((String) type.getFullyQualifiedName().get(), since, removal, details);
		}
		else if (node instanceof MethodDeclaration method) {
			String typeName = parentTypeNameOf(method);
			List<String> parameters = describeParameters(method);
			return new MethodDeprecation(typeName, method.getName().toString(), parameters, since, removal, details);
		}
		else if (node instanceof FieldDeclaration field) {
			String typeName = parentTypeNameOf(field);
			return new FieldDeprecation(typeName, field.getVariable(0).getName().toString(), since, removal, details);
		}
		else if (node instanceof ConstructorDeclaration constructor) {
			String typeName = parentTypeNameOf(constructor);
			List<String> parameters = describeParameters(constructor);
			return new ConstructorDeprecation(typeName, parameters, since, removal, details);
		}
		else if (node instanceof EnumConstantDeclaration enumConstant) {
			String typeName = parentTypeNameOf(enumConstant);
			return new EnumConstantDeprecation(typeName, enumConstant.getName().toString(), since, removal, details);
		}
		else if (node instanceof AnnotationMemberDeclaration annotationMember) {
			String typeName = parentTypeNameOf(annotationMember);
			return new AnnotationMemberDeprecation(typeName, annotationMember.getName().toString(), since, removal,
					details);
		}
		throw new IllegalStateException("Unexpected node of type " + node.getClass().getName());
	}

	private static String parentTypeNameOf(Node node) {
		return ((TypeDeclaration<?>) node.getParentNode().get()).getFullyQualifiedName().get();
	}

	private static List<String> describeParameters(CallableDeclaration<?> callable) {
		return callable.getParameters().stream().map((parameter) -> {
			try {
				return parameter.resolve().describeType();
			}
			catch (IllegalStateException ex) {
				throw new RuntimeException("Failed to resolve %s".formatted(parameter));
			}
		}).toList();
	}

}
