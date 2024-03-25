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

package org.springframework.boot.configurationmetadata.changelog;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Deprecation.Level;

/**
 * A difference the metadata.
 *
 * @param type the type of the difference
 * @param oldProperty the old property
 * @param newProperty the new property
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
record Difference(DifferenceType type, ConfigurationMetadataProperty oldProperty,
		ConfigurationMetadataProperty newProperty) {

	static Difference compute(ConfigurationMetadataProperty oldProperty, ConfigurationMetadataProperty newProperty) {
		if (newProperty == null) {
			Level oldLevel = (oldProperty.getDeprecation() != null) ? oldProperty.getDeprecation().getLevel() : null;
			return (oldLevel != Level.ERROR) ? new Difference(DifferenceType.DELETED, oldProperty, null) : null;
		}
		if (newProperty.isDeprecated()) {
			Level newLevel = newProperty.getDeprecation().getLevel();
			Level oldLevel = (oldProperty.getDeprecation() != null) ? oldProperty.getDeprecation().getLevel() : null;
			if (newLevel != oldLevel) {
				return new Difference((newLevel == Level.ERROR) ? DifferenceType.DELETED : DifferenceType.DEPRECATED,
						oldProperty, newProperty);
			}
		}
		return null;
	}

}
