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

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationSelector;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author awilkinson
 */
public class ProgrammaticAnnotatedTypeMetadata implements AnnotatedTypeMetadata {

	private final Class<?> annotationType;

	public ProgrammaticAnnotatedTypeMetadata(Class<?> annotationType) {
		this.annotationType = annotationType;
	}

	@Override
	public MergedAnnotations getAnnotations() {
		return new MergedAnnotations() {

			@Override
			public Iterator<MergedAnnotation<Annotation>> iterator() {
				throw new UnsupportedOperationException("Auto-generated method stub");
			}

			@Override
			public <A extends Annotation> boolean isPresent(Class<A> annotationType) {
				throw new UnsupportedOperationException("Auto-generated method stub");
			}

			@Override
			public boolean isPresent(String annotationType) {
				throw new UnsupportedOperationException("Auto-generated method stub");
			}

			@Override
			public <A extends Annotation> boolean isDirectlyPresent(Class<A> annotationType) {
				throw new UnsupportedOperationException("Auto-generated method stub");
			}

			@Override
			public boolean isDirectlyPresent(String annotationType) {
				throw new UnsupportedOperationException("Auto-generated method stub");
			}

			@Override
			public <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType) {
				throw new UnsupportedOperationException("Auto-generated method stub");
			}

			@Override
			public <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
					Predicate<? super MergedAnnotation<A>> predicate) {
				throw new UnsupportedOperationException("Auto-generated method stub");
			}

			@Override
			public <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
					Predicate<? super MergedAnnotation<A>> predicate, MergedAnnotationSelector<A> selector) {
				throw new UnsupportedOperationException("Auto-generated method stub");
			}

			@Override
			public <A extends Annotation> MergedAnnotation<A> get(String annotationType) {
				throw new UnsupportedOperationException("Auto-generated method stub");
			}

			@Override
			public <A extends Annotation> MergedAnnotation<A> get(String annotationType,
					Predicate<? super MergedAnnotation<A>> predicate) {
				throw new UnsupportedOperationException("Auto-generated method stub");
			}

			@Override
			public <A extends Annotation> MergedAnnotation<A> get(String annotationType,
					Predicate<? super MergedAnnotation<A>> predicate, MergedAnnotationSelector<A> selector) {
				throw new UnsupportedOperationException("Auto-generated method stub");
			}

			@Override
			public <A extends Annotation> Stream<MergedAnnotation<A>> stream(Class<A> annotationType) {
				throw new UnsupportedOperationException("Auto-generated method stub");
			}

			@Override
			public <A extends Annotation> Stream<MergedAnnotation<A>> stream(String annotationType) {
				return Stream.of();
			}

			@Override
			public Stream<MergedAnnotation<Annotation>> stream() {
				throw new UnsupportedOperationException("Auto-generated method stub");
			}

		};

	}

}
