package com.nathanglover.gocd.spark.ruleset;

import com.nathanglover.gocd.spark.PipelineListener;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.typesafe.config.Config;
import in.ashwanthkumar.utils.collections.Lists;
import in.ashwanthkumar.utils.func.Function;
import in.ashwanthkumar.utils.func.Predicate;
import in.ashwanthkumar.utils.lang.StringUtils;
import in.ashwanthkumar.utils.lang.option.Option;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

import static com.nathanglover.gocd.spark.ruleset.PipelineRule.merge;

public class Rules {

    private static Logger LOGGER = Logger.getLoggerFor(Rules.class);

    private boolean enabled;

    private String sparkWebHookUrl;
    private String sparkWebHookSecret;
    private String sparkWebHookName;
    private String sparkBearerToken;
    private String sparkRoom;
    private String sparkDisplayName;
    private String sparkUserIconURL;

    private String goServerHost;
    private String goAPIServerHost;
    private String goLogin;
    private String goPassword;

    private boolean displayConsoleLogLinks;
    private boolean displayMaterialChanges;
    private boolean processAllRules;
    private boolean truncateChanges;

    private Proxy proxy;

    private List<PipelineRule> pipelineRules = new ArrayList<PipelineRule>();
    private PipelineListener pipelineListener;

    public boolean isEnabled() {
        return enabled;
    }

    public Rules setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }


    /**
     * Spark Specific Configuration GET/SET
     */
    public String getSparkWebHookUrl() {
        return sparkWebHookUrl;
    }

    public Rules setSparkWebHookUrl(String sparkWebHookUrl) {
        this.sparkWebHookUrl = sparkWebHookUrl;
        return this;
    }

    public String getSparkWebHookSecret() {
        return sparkWebHookSecret;
    }

    public Rules setSparkWebHookSecret(String sparkWebHookSecret) {
        this.sparkWebHookSecret = sparkWebHookSecret;
        return this;
    }

    public String getSparkWebHookName() {
        return sparkWebHookName;
    }

    public Rules setSparkWebHookName(String sparkWebHookName) {
        this.sparkWebHookName = sparkWebHookName;
        return this;
    }

    public String getSparkBearerToken() {
        return sparkBearerToken;
    }

    public Rules setSparkBearerToken(String sparkBearerToken) {
        this.sparkBearerToken = sparkBearerToken;
        return this;
    }

    public String getSparkRoom() {
        return sparkRoom;
    }

    public Rules setSparkRoom(String sparkRoom) {
        this.sparkRoom = sparkRoom;
        return this;
    }

    public String getSparkDisplayName() {
        return sparkDisplayName;
    }

    private Rules setSparkDisplayName(String sparkDisplayName) {
        this.sparkDisplayName = sparkDisplayName;
        return this;
    }

    public String getSparkUserIconURL() {
        return sparkUserIconURL;
    }

    private Rules setSparkUserIconURL(String sparkUserIconURL) {
        this.sparkUserIconURL = sparkUserIconURL;
        return this;
    }


    /**
     * GoCD Server Specific Configuration GET/SET
     */
    public String getGoServerHost() {
        return goServerHost;
    }

    public Rules setGoServerHost(String goServerHost) {
        this.goServerHost = goServerHost;
        return this;
    }

    public String getGoAPIServerHost() {
        if (StringUtils.isNotEmpty(goAPIServerHost)) {
            return goAPIServerHost;
        }
        return getGoServerHost();
    }

    public Rules setGoAPIServerHost(String goAPIServerHost) {
        this.goAPIServerHost = goAPIServerHost;
        return this;
    }

    public String getGoLogin() {
        return goLogin;
    }

    public Rules setGoLogin(String goLogin) {
        this.goLogin = goLogin;
        return this;
    }

    public String getGoPassword() {
        return goPassword;
    }

    public Rules setGoPassword(String goPassword) {
        this.goPassword = goPassword;
        return this;
    }


    /**
     * Control Specific Configuration GET/SET
     */
    public boolean getDisplayConsoleLogLinks() {
        return displayConsoleLogLinks;
    }

    public Rules setDisplayConsoleLogLinks(boolean displayConsoleLogLinks) {
        this.displayConsoleLogLinks = displayConsoleLogLinks;
        return this;
    }

    public boolean getDisplayMaterialChanges() {
        return displayMaterialChanges;
    }

    public Rules setDisplayMaterialChanges(boolean displayMaterialChanges) {
        this.displayMaterialChanges = displayMaterialChanges;
        return this;
    }

    public boolean getProcessAllRules() {
        return processAllRules;
    }

    public Rules setProcessAllRules(boolean processAllRules) {
        this.processAllRules = processAllRules;
        return this;
    }

    public boolean isTruncateChanges() {
        return truncateChanges;
    }

    public Rules setTruncateChanges(boolean truncateChanges) {
        this.truncateChanges = truncateChanges;
        return this;
    }


    /**
     * Proxy Specific Configuration GET/SET
     */
    public Proxy getProxy() {
        return proxy;
    }

    public Rules setProxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }


    /**
     * Pipeline Specific Configuration GET/SET
     */
    public List<PipelineRule> getPipelineRules() {
        return pipelineRules;
    }

    public Rules setPipelineRules(List<PipelineRule> pipelineRules) {
        this.pipelineRules = pipelineRules;
        return this;
    }

    public PipelineListener getPipelineListener() {
        return pipelineListener;
    }

    public List<PipelineRule> find(final String pipeline, final String stage, final String group,
        final String pipelineStatus) {
        Predicate<PipelineRule> predicate = new Predicate<PipelineRule>() {
            public Boolean apply(PipelineRule input) {
                return input.matches(pipeline, stage, group, pipelineStatus);
            }
        };

        if (processAllRules) {
            return Lists.filter(pipelineRules, predicate);
        } else {
            List<PipelineRule> found = new ArrayList<PipelineRule>();
            Option<PipelineRule> match = Lists.find(pipelineRules, predicate);
            if (match.isDefined()) {
                found.add(match.get());
            }
            return found;
        }
    }

    public static Rules fromConfig(Config config) {
        boolean isEnabled = config.getBoolean("enabled");

        String webhookUrl = config.getString("webhookUrl");
        String webhookSecret = config.getString("webhookSecret");
        String webhookName = "GoCD Webhook";
        if (config.hasPath("webhookName")) {
            webhookName = config.getString("webhookName");
        }

        String bearerToken = config.getString("bearerToken");

        String room = null;
        if (config.hasPath("room")) {
            room = config.getString("room");
        }

        String displayName = "gocd-spark-bot";
        if (config.hasPath("sparkDisplayName")) {
            displayName = config.getString("sparkDisplayName");
        }

        String iconURL = "https://raw.githubusercontent.com/ashwanthkumar/assets/c597777ee749c89fec7ce21304d727724a65be7d/images/gocd-logo.png";
        if (config.hasPath("sparkUserIconURL")) {
            iconURL = config.getString("sparkUserIconURL");
        }

        String serverHost = config.getString("server-host");
        String apiServerHost = null;
        if (config.hasPath("api-server-host")) {
            apiServerHost = config.getString("api-server-host");
        }
        String login = null;
        if (config.hasPath("login")) {
            login = config.getString("login");
        }
        String password = null;
        if (config.hasPath("password")) {
            password = config.getString("password");
        }

        boolean displayConsoleLogLinks = true;
        if (config.hasPath("display-console-log-links")) {
            displayConsoleLogLinks = config.getBoolean("display-console-log-links");
        }

        // TODO - Next major release - change this to - separated config
        boolean displayMaterialChanges = true;
        if (config.hasPath("displayMaterialChanges")) {
            displayMaterialChanges = config.getBoolean("displayMaterialChanges");
        }

        boolean processAllRules = false;
        if (config.hasPath("process-all-rules")) {
            processAllRules = config.getBoolean("process-all-rules");
        }

        boolean truncateChanges = true;
        if (config.hasPath("truncate-changes")) {
            truncateChanges = config.getBoolean("truncate-changes");
        }

        Proxy proxy = null;
        if (config.hasPath("proxy")) {
            Config proxyConfig = config.getConfig("proxy");
            if (proxyConfig.hasPath("hostname") && proxyConfig.hasPath("port") && proxyConfig
                .hasPath("type")) {
                String hostname = proxyConfig.getString("hostname");
                int port = proxyConfig.getInt("port");
                String type = proxyConfig.getString("type").toUpperCase();
                Proxy.Type proxyType = Proxy.Type.valueOf(type);
                proxy = new Proxy(proxyType, new InetSocketAddress(hostname, port));
            }
        }

        final PipelineRule defaultRule = PipelineRule.fromConfig(config.getConfig("default"), room);
        List<PipelineRule> pipelineRules = Lists.map(config.getConfigList(
            "pipelines"), input -> merge(PipelineRule.fromConfig(input), defaultRule
        ));

        Rules rules = new Rules()
            .setEnabled(isEnabled)
            .setSparkWebHookUrl(webhookUrl)
            .setSparkWebHookSecret(webhookSecret)
            .setSparkWebHookName(webhookName)
            .setSparkBearerToken(bearerToken)
            .setSparkRoom(room)
            .setSparkDisplayName(displayName)
            .setSparkUserIconURL(iconURL)
            .setGoServerHost(serverHost)
            .setGoAPIServerHost(apiServerHost)
            .setGoLogin(login)
            .setGoPassword(password)
            .setDisplayConsoleLogLinks(displayConsoleLogLinks)
            .setDisplayMaterialChanges(displayMaterialChanges)
            .setProcessAllRules(processAllRules)
            .setTruncateChanges(truncateChanges)
            .setProxy(proxy)
            .setPipelineRules(pipelineRules);
        try {
            rules.pipelineListener = Class.forName(config.getString("listener"))
                .asSubclass(PipelineListener.class).getConstructor(Rules.class).newInstance(rules);
        } catch (Exception e) {
            LOGGER.error("Exception while initializing pipeline listener", e);
            throw new RuntimeException(e);
        }

        return rules;
    }
}
