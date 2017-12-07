package com.nathanglover.gocd.spark.ruleset;

import in.ashwanthkumar.utils.collections.Sets;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class RulesReaderTest {

    @Test
    public void shouldReadTestConfig() {
        Rules rules = RulesReader.read("configs/test-config-1.conf");
        assertThat(rules.isEnabled(), is(true));
        assertThat(rules.getSparkRoom(), is("#gocd"));
        assertThat(rules.getGoServerHost(), is("http://localhost:8080/"));
        assertThat(rules.getPipelineRules().size(), is(2));
        assertThat(rules.getPipelineRules().size(), is(2));
        assertThat(rules.getDisplayConsoleLogLinks(), is(false));
        assertThat(rules.getDisplayMaterialChanges(), is(false));

        PipelineRule pipelineRule1 = new PipelineRule()
                .setNameRegex("gocd-spark-build-notifier")
                .setStageRegex(".*")
                .setGroupRegex(".*")
                .setRoom("#gocd")
                .setStatus(Sets.of(PipelineStatus.FAILED));
        assertThat(rules.getPipelineRules(), CoreMatchers.hasItem(pipelineRule1));

        PipelineRule pipelineRule2 = new PipelineRule()
                .setNameRegex("my-java-utils")
                .setStageRegex("build")
                .setGroupRegex("ci")
                .setRoom("#gocd-build")
                .setStatus(Sets.of(PipelineStatus.FAILED));
        assertThat(rules.getPipelineRules(), CoreMatchers.hasItem(pipelineRule2));

        assertThat(rules.getPipelineListener(), notNullValue());
    }

    @Test
    public void shouldReadMinimalConfig() {
        Rules rules = RulesReader.read("configs/test-config-minimal.conf");

        assertThat(rules.isEnabled(), is(true));

        assertThat(rules.getGoLogin(), is("someuser"));
        assertThat(rules.getGoPassword(), is("somepassword"));
        assertThat(rules.getGoServerHost(), is("http://localhost:8153/"));
        assertThat(rules.getSparkWebHookUrl(), is("https://api.ciscospark.com/v1/webhooks/"));

        assertThat(rules.getSparkRoom(), is("#build"));
        assertThat(rules.getSparkDisplayName(), is("gocd-spark-bot"));
        assertThat(rules.getSparkUserIconURL(), is("http://example.com/spark-bot.png"));

        // Default rules
        assertThat(rules.getPipelineRules().size(), is(1));
        assertThat(rules.getDisplayConsoleLogLinks(), is(true));
        assertThat(rules.getDisplayMaterialChanges(), is(true));

        PipelineRule pipelineRule = new PipelineRule()
                .setNameRegex(".*")
                .setStageRegex(".*")
                .setGroupRegex(".*")
                .setRoom("#build")
                .setStatus(Sets.of(PipelineStatus.CANCELLED, PipelineStatus.BROKEN, PipelineStatus.FAILED, PipelineStatus.FIXED));
        assertThat(rules.getPipelineRules(), CoreMatchers.hasItem(pipelineRule));

        assertThat(rules.getPipelineListener(), notNullValue());
    }

    @Test
    public void shouldReadMinimalConfigWithPipeline() {
        Rules rules = RulesReader.read("configs/test-config-minimal-with-pipeline.conf");
        assertThat(rules.isEnabled(), is(true));
        assertThat(rules.getSparkRoom(), nullValue());
        assertThat(rules.getGoServerHost(), is("https://go-instance:8153/"));
        assertThat(rules.getSparkWebHookUrl(), is("https://api.ciscospark.com/v1/webhooks/"));
        assertThat(rules.getPipelineRules().size(), is(1));

        PipelineRule pipelineRule = new PipelineRule()
                .setNameRegex(".*")
                .setStageRegex(".*")
                .setGroupRegex(".*")
                .setRoom("#foo")
                .setStatus(Sets.of(PipelineStatus.FAILED))
                .setWebhookUrl("https://api.ciscospark.com/v1/webhooks/for-pipeline");
        assertThat(rules.getPipelineRules(), CoreMatchers.hasItem(pipelineRule));

        assertThat(rules.getPipelineListener(), notNullValue());
    }

    @Test
    public void shouldReadMinimalConfigWithPipelineAndEnvironmentVariables() {
        Rules rules = RulesReader.read("configs/test-config-minimal-with-env-variables.conf");
        assertThat(rules.isEnabled(), is(true));
        assertThat(rules.getSparkRoom(), nullValue());
        assertThat(rules.getGoServerHost(), is("https://go-instance:8153/"));
        assertThat(rules.getSparkWebHookUrl(), is("https://api.ciscospark.com/v1/webhooks/"));
        assertThat(rules.getPipelineRules().size(), is(1));
        assertThat(rules.getGoLogin(), is(System.getenv("HOME")));

        PipelineRule pipelineRule = new PipelineRule()
                .setNameRegex(".*")
                .setStageRegex(".*")
                .setGroupRegex(".*")
                .setRoom("#foo")
                .setStatus(Sets.of(PipelineStatus.FAILED));
        assertThat(rules.getPipelineRules(), CoreMatchers.hasItem(pipelineRule));

        assertThat(rules.getPipelineListener(), notNullValue());
        assertThat(rules.getProxy(), nullValue());
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionIfConfigInvalid() {
        RulesReader.read("test-config-invalid.conf");
    }

    @Test
    public void shouldReadProxyConfig() {
        Rules rules = RulesReader.read("configs/test-config-with-proxy.conf");
        assertThat(rules.isEnabled(), is(true));
        Proxy expectedProxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 5555));
        assertThat(rules.getProxy(), is(expectedProxy));
    }
}
