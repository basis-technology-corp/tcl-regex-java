env.project = "tcl-regex-java"

def properties = [
    buildParams: [
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
            defaultValue: 'openjdk-21',
            description: 'The JDK to use for building',
        ),
        string(
            name: 'testJdks',
            defaultValue: 'openjdk-11 openjdk-17',
            description: 'The JDKs to use for tests, separated by spaces',
        ),
    ],
]

standardProperties(properties)

def afterBuildClosures = []

def buildOptions = [
    mavenOptionsExtra: [jdk: params.buildJdk],
    afterBuild: { afterBuildClosures.each { it() } },
]


afterBuildClosures += {
    stage('sonar-report') {
        withMaven() {
            withSonarQubeEnv() {
                sh "mvn -D sonar.login=${env.SONAR_AUTH_TOKEN} sonar:sonar -D sonar.host.url=${env.SONAR_HOST_URL}"
            }
        }
    }
}

if (params.testJdks) {
    afterBuildClosures += {
        params.testJdks.split(' ').each { testJdk ->
            stage("Tests in ${testJdk}") {
                withMaven([
                    jdk: testJdk,
                ]) {
                    sh 'mvn -B -e -D enforcer.skip -D maven.javadoc.skip verify'
                }
            }
        }
    }
}

buildOptions.buildArgs = ' '

standardBuildSteps(buildOptions)
