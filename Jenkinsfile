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
                    sh "mvn clean package --settings ./settings.xml"
                }
            }
        }
        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'target/*.hpi', followSymlinks: false
            }
        }
        stage('Notification') {
            steps {
                withCredentials([string(credentialsId: 'wechat-bot-webhook', variable: 'WEBHOOK')]) {
                    qyWechatNotification failSend: true, mentionedId: '', mentionedMobile: '', webhookUrl: '${WEBHOOK}'
                }
            }
        }
    }
}