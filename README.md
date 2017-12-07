

<div align = "center">
    <h1>GoCD <em>Spark</em> Build Notifier </h1>
    <p>CiscoSpark based GoCD build notifier</p>
    <a href="https://manparvesh.mit-license.org/" target="_blank"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License"></a>
    <a href="https://www.android.com/" target="_blank"><img src="https://img.shields.io/badge/Platform-Java-blue.svg" alt="License"></a>
    <a href="https://travis-ci.org/t04glovern/gocd-spark-build-notifier" target="_blank"><img src="https://travis-ci.org/t04glovern/gocd-spark-build-notifier.svg?branch=master" alt="Build Status"></a>
    <a href="https://codecov.io/gh/t04glovern/gocd-spark-build-notifier"><img src="https://codecov.io/gh/t04glovern/gocd-spark-build-notifier/branch/master/graph/badge.svg" />
</a>
</div>

## Setup
Download jar from [releases](https://github.com/t04glovern/gocd-spark-build-notifier/releases) & place it in /plugins/external & restart Go Server.

## Configuration
All configurations are in [HOCON](https://github.com/typesafehub/config) format. Plugin searches for the configuration file in the following order

1. File defined by the environment variable `GO_NOTIFY_CONF`.
2. `go_notify.conf` at the user's home directory. Typically it's the `go` user's home directory (`/var/go`).
3. `go_notify.conf` present at the `CRUISE_SERVER_DIR` environment variable location.

Minimalistic configuration would be something like
```hocon
gocd.spark {
  login = "someuser"
  password = "somepassword"
  server-host = "http://localhost:8153/"
  api-server-host = "http://localhost:8153/"
  webhookUrl = "https://hooks.spark.com/services/...."

  # optional fields
  channel = "#build"
  sparkDisplayName = "gocd-spark-bot"
  sparkUserIconURL = "http://example.com/spark-bot.png"
  display-console-log-links = true
  displayMaterialChanges = true
  process-all-rules = true
  proxy {
    hostname = "localhost"
    port = "5555"
    type = "socks" # acceptable values are http / socks
  }
}
```
- `login` - Login for a Go user who is authorized to access the REST API.
- `password` - Password for the user specified above. You might want to create a less privileged user for this plugin.
- `server-host` - FQDN of the Go Server. All links on the spark channel will be relative to this host.
- `api-server-host` - This is an optional attribute. Set this field to localhost so server will use this endpoint to get `PipelineHistory` and `PipelineInstance`  
- `webhookUrl` - Spark Webhook URL
- `channel` - Override the default channel where we should send the notifications in spark. You can also give a value starting with `@` to send it to any specific user.
- `display-console-log-links` - Display console log links in the notification. Defaults to true, set to false if you want to hide.
- `displayMaterialChanges` - Display material changes in the notification (git revisions for example). Defaults to true, set to false if you want to hide.
- `process-all-rules` - If true, all matching rules are applied instead of just the first.
- `truncate-changes` - If true, displays only the latest 5 changes for all the materials. (Default: true)
- `proxy` - Specify proxy related settings for the plugin.
  - `proxy.hostname` - Proxy Host
  - `proxy.port` - Proxy Port
  - `proxy.type` - `socks` or `http` are the only accepted values.

## Pipeline Rules
By default the plugin pushes a note about all failed stages across all pipelines to Spark. You have fine grain control over this operation.
```hocon
gocd.spark {
  server-host = "http://localhost:8153/"
  webhookUrl = "https://hooks.spark.com/services/...."

  pipelines = [{
    name = "gocd-spark-build"
    stage = "build"
    group = ".*"
    state = "failed|passed"
    channel = "#oss-build-group"
    owners = ["ashwanthkumar"]
    webhookUrl = "https://hooks.spark.com/services/another-team-hook-id..."
  },
  {
    name = ".*"
    stage = ".*"
    state = "failed"
  }]
}
```
`gocd.spark.pipelines` contains all the rules for the go-server. It is a list of rules (see below for what the parameters mean) for various pipelines. The plugin will pick the first matching pipeline rule from the pipelines collection above, so your most specific rule should be first, with the most generic rule at the bottom. Alternatively, set the `process-all-rules` option to `true` and all matching rules will be applied.
- `name` - Regex to match the pipeline name
- `stage` - Regex to match the stage name
- `group` - Regex to match the pipeline group name
- `state` - State of the pipeline at which we should send a notification. You can provide multiple values separated by pipe (`|`) symbol. Valid values are passed, failed, cancelled, building, fixed, broken or all.
- `channel` - (Optional) channel where we should send the spark notification. This setting for a rule overrides the global setting
- `owners` - (Optional) list of spark user handles who must be tagged in the message upon notifications
- `webhookUrl` - (Optional) Use this webhook url instead of the global one. Useful if you're using multiple spark teams.

## License
[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)
