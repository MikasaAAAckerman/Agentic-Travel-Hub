pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        skipDefaultCheckout(false)
    }

    parameters {
        booleanParam(
            name: 'DEPLOY',
            defaultValue: false,
            description: '勾选后在流水线末尾执行部署（需要 Docker Desktop 已启动）'
        )
    }

    environment {
        MAVEN_OPTS = '-Dfile.encoding=UTF-8'
    }

    stages {
        stage('环境检查') {
            steps {
                bat 'java -version && mvn -v'
                bat 'git rev-parse --abbrev-ref HEAD && git rev-parse --short HEAD'
            }
        }

        stage('编译') {
            steps {
                bat 'mvn -B clean compile -DskipTests'
            }
        }

        stage('测试') {
            steps {
                // -Pci 排除依赖 PostgreSQL / 向量库 / API Key 的集成测试，见 hub-starter/pom.xml
                bat 'mvn -B test -Pci'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('打包') {
            steps {
                bat 'mvn -B package -DskipTests'
                archiveArtifacts artifacts: 'hub-starter/target/*.jar', fingerprint: true
            }
        }

        stage('部署') {
            when {
                expression { return params.DEPLOY }
            }
            steps {
                echo '调用本地 CD 脚本：打镜像 -> 起容器 -> 健康检查'
                bat 'powershell -NoProfile -ExecutionPolicy Bypass -File scripts\\cd.ps1'
            }
        }
    }

    post {
        success {
            echo '流水线通过'
        }
        failure {
            echo '流水线失败，检查上面的阶段日志'
        }
        always {
            echo "构建号 ${env.BUILD_NUMBER} 结束"
        }
    }
}