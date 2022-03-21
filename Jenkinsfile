pipeline {
    agent {
        node {
            label 'master'
        }
    }
    options { quietPeriod(2400) }
    stages {
        stage('Build/Test') {
            steps {
                configFileProvider([configFile(fileId: 'metersphere-maven', targetLocation: 'settings.xml')]) {
                    sh "./mvnw clean package --settings ./settings.xml"
                }
            }
        }
        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'target/*.hpi', followSymlinks: false
            }
        }
        stage('Release') {
            when { tag "v*" }
            steps {
                withCredentials([string(credentialsId: 'gitrelease', variable: 'TOKEN')]) {
                    withEnv(["TOKEN=$TOKEN"]) {
                        sh script: '''
                            release=$(curl -XPOST -H "Authorization:token $TOKEN" --data "{\\"tag_name\\": \\"${TAG_NAME}\\", \\"name\\": \\"${TAG_NAME}\\", \\"body\\": \\"\\", \\"draft\\": false, \\"prerelease\\": true}" https://api.github.com/repos/metersphere/jenkins-plugin/releases)
                            id=$(echo "$release" | sed -n -e \'s/"id":\\ \\([0-9]\\+\\),/\\1/p\' | head -n 1 | sed \'s/[[:blank:]]//g\')
                            cd target
                            curl -XPOST -H "Authorization:token $TOKEN" -H "Content-Type:application/octet-stream" --data-binary @metersphere-jenkins-plugin.hpi https://uploads.github.com/repos/metersphere/jenkins-plugin/releases/${id}/assets?name=metersphere-jenkins-plugin-${TAG_NAME}.hpi
                        '''
                    }
                }
            }
        }
    }
    post('Notification') {
        always {
            sh "echo \$WEBHOOK\n"
            withCredentials([string(credentialsId: 'wechat-bot-webhook', variable: 'WEBHOOK')]) {
                qyWechatNotification failSend: true, mentionedId: '', mentionedMobile: '', webhookUrl: "$WEBHOOK"
            }
        }
    }
}