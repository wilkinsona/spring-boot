/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.access;

import java.util.function.Predicate;

import org.springframework.boot.actuate.endpoint.OperationType;

/**
 * The different types of endpoint operation access.
 *
 * @author Andy Wilkinson
 * @since 3.4.0
 */
// TODO OperationAccess?
public enum EndpointAccess {

	/**
	 * Only allow access to {@link OperationType#READ read} operations.
	 */
	READ_ONLY(OperationType.READ::equals),

	/**
	 * Only allow access to {@link OperationType#WRITE write} and
	 * {@link OperationType#DELETE delete delete} operations is permitted.
	 */
	WRITE_ONLY(OperationType.WRITE::equals),

	/**
	 * Allow access to {@link OperationType#READ read}, {@link OperationType#WRITE write},
	 * and {@link OperationType#DELETE delete} operations.
	 */
	READ_WRITE((operationType) -> true);

	private final Predicate<OperationType> predicate;

	EndpointAccess(Predicate<OperationType> predicate) {
		this.predicate = predicate;
	}

	public boolean allow(OperationType type) {
		return this.predicate.test(type);
	}

}
