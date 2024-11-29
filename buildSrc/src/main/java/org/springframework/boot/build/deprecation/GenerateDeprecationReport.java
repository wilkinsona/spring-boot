/*
 * Copyright 2024 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.CommentsCollection;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;

import org.springframework.util.Assert;
import org.springframework.util.function.ThrowingFunction;

/**
 * {@link Task} to generate a report about a project's deprecated Java APIs.
 *
 * @author Andy Wilkinson
 */
public abstract class GenerateDeprecationReport extends SourceTask {

	private FileCollection sourceDirectories;

	private FileCollection dependencies;

	@CompileClasspath
	FileCollection getDependencies() {
		return this.dependencies;
	}

	@OutputFile
	public abstract RegularFileProperty getReportFile();

	public void setDependencies(FileCollection dependencies) {
		this.dependencies = dependencies;
	}

	public void setSourceDirectories(FileCollection sourceDirectories) {
		this.sourceDirectories = sourceDirectories;
		setSource(sourceDirectories.getAsFileTree());
	}

	@TaskAction
	void execute() throws IOException {
		DeprecationReport report = createReport();
		writeReport(report);
	}

	private DeprecationReport createReport() {
		JavaParser parser = createParser();
		Map<Class<? extends Deprecation>, List<Deprecation>> deprecations = withSourceFiles(
				(sourceFiles) -> sourceFiles.flatMap((file) -> analyze(file.toFile(), parser))
					.collect(Collectors.groupingBy(Deprecation::getClass)));
		DeprecationReport report = DeprecationReport.from(deprecations);
		return report;
	}

	private void writeReport(DeprecationReport report) throws IOException, StreamWriteException, DatabindException {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		File outputFile = getReportFile().get().getAsFile();
		outputFile.getParentFile().mkdirs();
		objectMapper.writeValue(outputFile, report);
	}

	private <T> T withSourceFiles(Function<Stream<Path>, T> transformer) {
		try (Stream<Path> sourceFiles = this.sourceDirectories.getFiles()
			.stream()
			.map(File::toPath)
			.filter(Files::isDirectory)
			.flatMap(ThrowingFunction.of((srcDir) -> Files.walk(srcDir)))) {
			return transformer.apply(sourceFiles.filter(this::isJavaSourceFile));
		}
	}

	private boolean isJavaSourceFile(Path path) {
		return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java");
	}

	private JavaParser createParser() {
		return new JavaParser(new ParserConfiguration().setSymbolResolver(createSymbolSolver())
			.setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));
	}

	private SymbolResolver createSymbolSolver() {
		TypeSolver dependenciesTypeSolver = createDependenciesTypeSolver();
		return new JavaSymbolSolver(new CombinedTypeSolver(dependenciesTypeSolver, createClassesTypeSolver()));
	}

	private TypeSolver createDependenciesTypeSolver() {
		URL[] urls = this.dependencies.getFiles()
			.stream()
			.map(File::toURI)
			.map(ThrowingFunction.of(URI::toURL))
			.toArray(URL[]::new);
		URLClassLoader classLoader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader());
		TypeSolver dependenciesTypeSolver = new ClassLoaderTypeSolver(classLoader);
		return dependenciesTypeSolver;
	}

	private TypeSolver createClassesTypeSolver() {
		return new CombinedTypeSolver(this.sourceDirectories.getFiles()
			.stream()
			.filter(File::isDirectory)
			.map((srcDir) -> new JavaParserTypeSolver(srcDir))
			.collect(Collectors.toList()));
	}

	private Stream<Deprecation> analyze(File file, JavaParser parser) {
		try {
			ParseResult<CompilationUnit> result = parser.parse(file);
			if (result.isSuccessful()) {
				return result.getCommentsCollection().map(this::analyze).orElseGet(Stream::empty);
			}
			else {
				throw new IllegalStateException("Failed to parse '%s'".formatted(file));
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private Stream<Deprecation> analyze(CommentsCollection comments) {
		List<Deprecation> deprecations = new ArrayList<>();
		for (JavadocComment comment : comments.getJavadocComments()) {
			comment.getCommentedNode().ifPresent((commented) -> {
				JavadocBlockTag deprecated = deprecatedTagFrom(comment);
				if (deprecated != null) {
					deprecations.add(Deprecation.from(deprecated, commented));
				}
			});
		}
		return deprecations.stream();
	}

	private JavadocBlockTag deprecatedTagFrom(JavadocComment comment) {
		List<JavadocBlockTag> deprecatedTags = comment.parse()
			.getBlockTags()
			.stream()
			.filter(this::isDeprecated)
			.toList();
		if (deprecatedTags.isEmpty()) {
			return null;
		}
		Assert.isTrue(deprecatedTags.size() == 1,
				() -> "Javadoc comment must have at most 1 @deprecated tag. Found %d".formatted(deprecatedTags.size()));
		return deprecatedTags.get(0);
	}

	private boolean isDeprecated(JavadocBlockTag tag) {
		return "deprecated".equals(tag.getTagName());
	}

}
