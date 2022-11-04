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

package org.springframework.boot.build.autoconfigure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.language.jvm.tasks.ProcessResources;

import org.springframework.asm.ClassReader;
import org.springframework.asm.Opcodes;
import org.springframework.core.CollectionFactory;
import org.springframework.util.StringUtils;

/**
 * A {@link Task} for generating metadata describing a project's auto-configuration
 * classes.
 *
 * @author Andy Wilkinson
 */
public class AutoConfigurationMetadata extends DefaultTask {

	private final DirectoryProperty resourcesDirectory;

	private final String moduleName;

	private final RegularFileProperty outputFile;

	private FileCollection classesDirectories;

	public AutoConfigurationMetadata() {
		this.resourcesDirectory = getProject().getObjects().directoryProperty();
		this.outputFile = getProject().getObjects().fileProperty();
		this.moduleName = getProject().getName();
		getProject().getConfigurations()
				.maybeCreate(AutoConfigurationPlugin.AUTO_CONFIGURATION_METADATA_CONFIGURATION_NAME);
	}

	@Internal
	public DirectoryProperty getResourcesDirectory() {
		return this.resourcesDirectory;
	}

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	Provider<RegularFile> getSpringFactories() {
		return this.resourcesDirectory.map((resources) -> resources.file("META-INF/spring.factories"));
	}

	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	FileCollection getClassesDirectories() {
		return this.classesDirectories;
	}

	public void setSourceSet(SourceSet sourceSet) {
		this.resourcesDirectory.fileProvider(
				getProject().getTasks().named(sourceSet.getProcessResourcesTaskName(), ProcessResources.class)
						.map(ProcessResources::getDestinationDir));
		this.classesDirectories = sourceSet.getOutput().getClassesDirs();
	}

	@OutputFile
	public RegularFileProperty getOutputFile() {
		return this.outputFile;
	}

	@TaskAction
	void documentAutoConfiguration() throws IOException {
		Properties autoConfiguration = readAutoConfiguration();
		File outputFile = this.outputFile.get().getAsFile();
		outputFile.getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(outputFile)) {
			autoConfiguration.store(writer, null);
		}
	}

	private Properties readAutoConfiguration() throws IOException {
		Properties autoConfiguration = CollectionFactory.createSortedProperties(true);
		Properties springFactories = readSpringFactories(this.getSpringFactories().get().getAsFile());
		String enableAutoConfiguration = springFactories
				.getProperty("org.springframework.boot.autoconfigure.EnableAutoConfiguration");
		Set<String> classNames = StringUtils.commaDelimitedListToSet(enableAutoConfiguration);
		Set<String> publicClassNames = new LinkedHashSet<>();
		for (String className : classNames) {
			File classFile = findClassFile(className);
			if (classFile == null) {
				throw new IllegalStateException("Auto-configuration class '" + className + "' not found.");
			}
			try (InputStream in = new FileInputStream(classFile)) {
				int access = new ClassReader(in).getAccess();
				if ((access & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC) {
					publicClassNames.add(className);
				}
			}
		}
		autoConfiguration.setProperty("autoConfigurationClassNames", String.join(",", publicClassNames));
		autoConfiguration.setProperty("module", this.moduleName);
		return autoConfiguration;
	}

	private File findClassFile(String className) {
		String classFileName = className.replace(".", "/") + ".class";
		for (File classesDir : this.classesDirectories) {
			File classFile = new File(classesDir, classFileName);
			if (classFile.isFile()) {
				return classFile;
			}
		}
		return null;
	}

	private Properties readSpringFactories(File file) throws IOException {
		Properties springFactories = new Properties();
		try (Reader in = new FileReader(file)) {
			springFactories.load(in);
		}
		return springFactories;
	}

}
