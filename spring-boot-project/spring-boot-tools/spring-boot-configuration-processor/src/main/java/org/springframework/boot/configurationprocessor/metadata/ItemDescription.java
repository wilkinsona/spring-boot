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

package org.springframework.boot.configurationprocessor.metadata;

import java.util.Objects;

/**
 * Description of an item.
 *
 * @author Andy Wilkinson
 * @since 2.6.11
 */
public final class ItemDescription {

	/**
	 * Instance indicating that a description was unavailable during annotation processing
	 * and that the presence or absence of a description is unknown.
	 */
	public static final ItemDescription UNAVAILABLE = new ItemDescription(null);

	/**
	 * Instance indicating that there was no description for the property.
	 */
	public static final ItemDescription NONE = new ItemDescription(null);

	private String content;

	private ItemDescription(String content) {
		this.content = content;
	}

	public String getContent() {
		return this.content;
	}

	public static ItemDescription of(String content) {
		if (content == null || content.isEmpty()) {
			return NONE;
		}
		return new ItemDescription(content);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ItemDescription other = (ItemDescription) obj;
		return Objects.equals(this.content, other.content);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.content);
	}

	@Override
	public String toString() {
		if (this == UNAVAILABLE) {
			return "<< description unavailable >>";
		}
		return this.content;
	}

}
