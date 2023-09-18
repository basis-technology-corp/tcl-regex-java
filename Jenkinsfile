env.project = "tcl-regex-java"

properties([
    parameters([
        booleanParam(
	    name: 'release',
            defaultValue: false,
            description: 'Release the project'
	),
        string(
	    name: 'version',
            defaultValue: '',
            description: 'Version to release, or empty to use the default next version'
	),
	string(
	    name: 'buildJdk',
	    defaultValue: 'openjdk-17',
            description: 'The JDK to use for building'
	)
    ])
])

standardProperties(properties)

def options = [:]

options.afterBuild = {
    withMaven([jdk: params.testJdk]) {
        withSonarQubeEnv() {
            sh "mvn -D sonar.login=${env.SONAR_AUTH_TOKEN} sonar:sonar -D sonar.host.url=${env.SONAR_HOST_URL}"
        }
    }
}

options.buildArgs = ' '

standardBuildSteps(options)
