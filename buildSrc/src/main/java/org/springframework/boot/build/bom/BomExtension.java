/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.build.bom;

import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.artifacts.dsl.DependencyHandler;

public class BomExtension {

	private final Map<String, String> properties = new LinkedHashMap<>();

	private final Map<String, String> dependencies = new LinkedHashMap<>();

	private final Map<String, String> bomImports = new LinkedHashMap<>();

	private final DependencyHandler dependencyHandler;

	public BomExtension(DependencyHandler dependencyHandler) {
		this.dependencyHandler = dependencyHandler;
	}

	public void property(String name, String value) {
		this.properties.put(name, value);
	}

	public void bomImport(String groupId, String artifactId, String version) {
		this.bomImports.put(groupId + ":" + artifactId, version);
		this.dependencyHandler.add("api", this.dependencyHandler.enforcedPlatform(createDependencyNotation(groupId, artifactId, version)));
	}

	public void dependency(String groupId, String artifactId, String version) {
		this.dependencies.put(groupId + ":" + artifactId, version);
		this.dependencyHandler.getConstraints().add("api", createDependencyNotation(groupId, artifactId, version));
	}

	private String createDependencyNotation(String groupId, String artifactId, String version) {
		return groupId + ":" + artifactId + ":" + resolveVersion(version);
	}

	private String resolveVersion(String version) {
		while (version.startsWith("${")) {
			String resolved = properties.get(version.substring(2, version.length() - 1));
			if (resolved != null) {
				version = resolved;
			}
			else {
				break;
			}
		}
		return version;
	}


	Map<String, String> getProperties() {
		return this.properties;
	}

	Map<String, String> getDependencies() {
		return this.dependencies;
	}

	Map<String, String> getImports() {
		return this.bomImports;
	}

	String getVersion(String groupId, String artifactId) {
		String coordinates = groupId + ":" + artifactId;
		String dependencyVersion = this.dependencies.get(coordinates);
		return dependencyVersion != null ? dependencyVersion : this.bomImports.get(coordinates);
	}

}
