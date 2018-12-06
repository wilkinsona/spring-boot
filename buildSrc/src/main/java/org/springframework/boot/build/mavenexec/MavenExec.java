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

package org.springframework.boot.build.mavenexec;

import java.io.File;
import java.util.ArrayList;

import org.gradle.api.attributes.Attribute;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.process.CommandLineArgumentProvider;

/**
 * Gradle {@link org.gradle.api.Task} to get Maven distribution and invoke Maven goal of
 * same name is this task name.
 *
 * @author Cédric Champeau
 * @author Eric Wendelin
 */
public class MavenExec extends Exec {

	private File mavenHome;

	public MavenExec() {
		super();

		setGroup("Maven tasks");
		setExecutable(new ExecutableProvider());
		getArgumentProviders().add((CommandLineArgumentProvider) () -> getTasks().get());
	}

	@InputFiles
	public FileCollection getMavenDistribution() {
		return getProject().getConfigurations().getByName("maven").getIncoming()
				.artifactView((view) -> {
					view.attributes((attrs) -> attrs.attribute(
							Attribute.of("artifactType", String.class), "exploded"));
				}).getFiles();
	}

	@Input
	public ListProperty<String> getTasks() {
		ListProperty<String> tasks = getProject().getObjects().listProperty(String.class);
		ArrayList<String> taskNames = new ArrayList<>();
		taskNames.add(getName());
		tasks.set(taskNames);
		return tasks;
	}

	// Lazily get Maven home dir
	public File getMavenHome() {
		if (this.mavenHome == null) {
			this.mavenHome = getMavenDistribution().getSingleFile();
		}

		getLogger().lifecycle("Executing Maven from " + this.mavenHome.getAbsolutePath());

		return this.mavenHome;
	}

	private class ExecutableProvider {

		private MavenExtension mavenExtension;

		private MavenExtension getMavenExtension() {
			if (this.mavenExtension == null) {
				this.mavenExtension = getProject().getExtensions()
						.findByType(MavenExtension.class);
			}
			return this.mavenExtension;
		}

		@Override
		public String toString() {
			return getMavenHome() + "/apache-maven-" + getMavenExtension().version
					+ "/bin/mvn";
		}

	}

}
