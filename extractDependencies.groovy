import groovy.util.XmlSlurper

def getConfigurationForDependency(def dependency) {
	def annotationProcessors = ['spring-boot-configuration-processor']
	if (annotationProcessors.contains(dependency.artifactId.text())) {
		return "annotationProcessor"
	}
	else if (dependency.optional.text()) {
		return "optional"
	}
	else if ("test".equals(dependency.scope.text())) {
		return "testImplementation"
	}
	return "implementation"
}

def pom = new XmlSlurper().parse(new File(args[0]))

pom.dependencies.dependency.each { dependency ->
	def configuration = getConfigurationForDependency(dependency)
	println "${configuration} '${dependency.groupId.text()}:${dependency.artifactId.text()}'"
}