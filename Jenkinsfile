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
	)
     ])
])

standardProperties(properties)

def options = [
    mavenOptionsExtra: [jdk: 'openjdk-17'],
]

options.afterBuild = {
    withMaven([jdk: params.testJdk]) {
        withSonarQubeEnv() {
            sh "mvn -D sonar.login=${env.SONAR_AUTH_TOKEN} sonar:sonar -D sonar.host.url=${env.SONAR_HOST_URL}"
        }
    }
}

options.buildArgs = ' '

standardBuildSteps(options)
