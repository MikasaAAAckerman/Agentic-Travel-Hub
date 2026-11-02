# 本地 CI/CD

三个脚本，在 Windows 本机上模拟"提交 → 测试 → 打包 → 部署"这条链路。
不装 Jenkins、不连云端，`powershell` + `mvn` + `docker` 就够。

## 快速开始

```powershell
# 1. 跑一次 CI：环境检查 -> 编译 -> 测试 -> 打包 -> 产物指纹
powershell -ExecutionPolicy Bypass -File scripts\ci.ps1

# 2. 装 git 钩子：以后每次 git push 前自动跑 CI，不过就拦住
powershell -ExecutionPolicy Bypass -File scripts\install-hooks.ps1

# 3. 部署：打镜像 -> 起容器 -> 健康检查 -> 失败自动回滚
#    前提：Docker Desktop 已启动
powershell -ExecutionPolicy Bypass -File scripts\cd.ps1
```

## ci.ps1

| 阶段 | 做什么 | 失败后果 |
|---|---|---|
| 1 环境检查 | JDK 21+ / Maven / git 分支与提交号 | 直接退出 |
| 2 编译 | `mvn clean compile` | 中断，不跑测试 |
| 3 测试 | `mvn test`，汇总各模块 surefire | 中断，不打包 |
| 4 打包 | `mvn package -DskipTests` | 中断 |
| 5 产物指纹 | jar 大小 + SHA256，写 `target/ci-meta.json` | 中断 |

产出：

- `target/ci-logs/*.log` —— 每个阶段的完整 Maven 日志
- `target/ci-report.txt` —— 本次流水线摘要（提交号、用例数、产物指纹）
- `target/ci-meta.json` —— 给 CD 读的元数据

加 `-SkipTests` 可跳过测试阶段。

## cd.ps1

1. 检查 Docker 守护进程、jar 是否存在
2. 版本号取自 `target/ci-meta.json` 的 commit（或 git 短 SHA）
3. 上一版镜像存为 `previous`，再构建新镜像
4. `docker compose -f docker/app-compose.yml up -d`
5. 轮询 `/actuator/health`，超时则回滚到 `previous`

参数：`-Tag`、`-Port`、`-HealthTimeoutSeconds`。

## 说明

- 容器内访问宿主机服务要走 `host.docker.internal`，`docker/app-compose.yml` 已配好。
- 宿主机 5432 没起来时，应用能启动但健康检查是 `DOWN`，脚本会如实报出来而不是假装成功。
- 紧急跳过 pre-push 钩子：`git push --no-verify`。

## 和 Jenkins 的关系

这套脚本就是**流水线定义**本身。Jenkins 只是把同样的命令搬到 Web 上，加定时任务、
多分支、构建历史和并发执行。脚本能跑通，迁到 Jenkins 就是写个 Jenkinsfile 调用它。