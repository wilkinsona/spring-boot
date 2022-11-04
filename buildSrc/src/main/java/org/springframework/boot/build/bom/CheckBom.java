/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.build.bom;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.TaskAction;

import org.springframework.boot.build.bom.Library.Exclusion;
import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.Module;

/**
 * Checks the validity of a bom.
 *
 * @author Andy Wilkinson
 */
public class CheckBom extends DefaultTask {

	private final ListProperty<ResolvedModule> resolvedModules;

	@Inject
	public CheckBom(BomExtension bom) {
		this.resolvedModules = getProject().getObjects().listProperty(ResolvedModule.class);
		this.resolvedModules.set(getProject().provider(() -> {
			List<ResolvedModule> results = new ArrayList<>();
			for (Library library : bom.getLibraries()) {
				for (Group group : library.getGroups()) {
					for (Module module : group.getModules()) {
						if (!module.getExclusions().isEmpty()) {
							Configuration detachedConfiguration = getProject().getConfigurations()
									.detachedConfiguration(getProject().getDependencies().create(group.getId() + ":"
											+ module.getName() + ":" + library.getVersion().getVersion()));
							results.add(new ResolvedModule(group.getId() + ":" + module.getName(),
									detachedConfiguration.getIncoming().getResolutionResult().getRoot(),
									module.getExclusions()));
						}
					}
				}
			}
			return results;
		}));

	}

	@TaskAction
	void checkBom() {
		for (ResolvedModule module : this.resolvedModules.get()) {
			checkExclusions(module);
		}
	}

	private Set<String> dependencyIds(ResolvedComponentResult componentResult) {
		Set<String> dependencyIds = new LinkedHashSet<>();
		dependencyIds(componentResult, dependencyIds);
		return dependencyIds;
	}

	private void dependencyIds(ResolvedComponentResult componentResult, Set<String> dependencyIds) {
		String componentId = componentResult.getModuleVersion().getGroup() + ":"
				+ componentResult.getModuleVersion().getName() + ":" + componentResult.getModuleVersion().getVersion();
		if (dependencyIds.add(componentId)) {
			for (DependencyResult result : componentResult.getDependencies()) {
				if (result instanceof ResolvedDependencyResult) {
					dependencyIds(((ResolvedDependencyResult) result).getSelected(), dependencyIds);
				}
			}
		}
	}

	private void checkExclusions(ResolvedModule module) {
		Set<String> resolved = dependencyIds(module.resolvedComponentResult);
		Set<String> exclusions = module.exclusions.stream()
				.map((exclusion) -> exclusion.getGroupId() + ":" + exclusion.getArtifactId())
				.collect(Collectors.toSet());
		Set<String> unused = new TreeSet<>();
		for (String exclusion : exclusions) {
			if (!resolved.contains(exclusion)) {
				if (exclusion.endsWith(":*")) {
					String group = exclusion.substring(0, exclusion.indexOf(':') + 1);
					if (resolved.stream().noneMatch((candidate) -> candidate.startsWith(group))) {
						unused.add(exclusion);
					}
				}
				else {
					unused.add(exclusion);
				}
			}
		}
		exclusions.removeAll(resolved);
		if (!unused.isEmpty()) {
			throw new InvalidUserDataException("Unnecessary exclusions on " + module.id + ": " + exclusions);
		}
	}

	private static final class ResolvedModule {

		private final String id;

		private final ResolvedComponentResult resolvedComponentResult;

		private final List<Exclusion> exclusions;

		private ResolvedModule(String id, ResolvedComponentResult resolvedComponentResult, List<Exclusion> exclusions) {
			this.id = id;
			this.resolvedComponentResult = resolvedComponentResult;
			this.exclusions = exclusions;
		}

	}

}
