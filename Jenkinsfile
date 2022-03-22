env.project = "tcl-regex-java"

standardProperties()

def options = [:]

options.afterBuild = {
    withMaven([dk: params.testJdk]) {
        withSonarQubeEnv() {
            sh "mvn -D sonar.login=${env.SONAR_AUTH_TOKEN} sonar:sonar -D sonar.host.url=${env.SONAR_HOST_URL}"
        }
    }
}

standardBuildSteps(options)
