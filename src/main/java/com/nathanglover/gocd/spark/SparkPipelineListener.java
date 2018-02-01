package com.nathanglover.gocd.spark;

import static in.ashwanthkumar.utils.lang.StringUtils.startsWith;

import com.ciscospark.Message;
import com.ciscospark.NotAuthenticatedException;
import com.ciscospark.Spark;
import com.ciscospark.Webhook;
import com.nathanglover.gocd.spark.jsonapi.MaterialRevision;
import com.nathanglover.gocd.spark.jsonapi.Modification;
import com.nathanglover.gocd.spark.jsonapi.Pipeline;
import com.nathanglover.gocd.spark.jsonapi.Stage;
import com.nathanglover.gocd.spark.ruleset.PipelineRule;
import com.nathanglover.gocd.spark.ruleset.PipelineStatus;
import com.nathanglover.gocd.spark.ruleset.Rules;
import com.thoughtworks.go.plugin.api.logging.Logger;
import in.ashwanthkumar.utils.collections.Lists;
import in.ashwanthkumar.utils.func.Function;
import in.ashwanthkumar.utils.lang.StringUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SparkPipelineListener extends PipelineListener {

    public static final int DEFAULT_MAX_CHANGES_PER_MATERIAL_IN_SPARK = 5;
    private Logger LOG = Logger.getLoggerFor(SparkPipelineListener.class);

    private Spark spark;

    public SparkPipelineListener(Rules rules) {
        super(rules);
        // Initialize the client
        try {
            spark = Spark.builder()
                .baseUrl(URI.create("https://api.ciscospark.com/v1"))
                .accessToken(rules.getSparkBearerToken())
                .build();
        } catch (NotAuthenticatedException e) {
            LOG.error("Cisco Spark Authentication failed: " + e.getMessage());
        }

        updateSparkRoom(rules.getSparkRoom());
    }

    @Override
    public void onBuilding(PipelineRule rule, GoNotificationMessage message) throws Exception {
        updateSparkRoom(rule.getRoom());
        updateWebhookUrl(rule.getWebhookUrl());
        sendMessageToRooms(sparkAttachment(rule, message, PipelineStatus.BUILDING));
    }

    @Override
    public void onPassed(PipelineRule rule, GoNotificationMessage message) throws Exception {
        updateSparkRoom(rule.getRoom());
        updateWebhookUrl(rule.getWebhookUrl());
        sendMessageToRooms(sparkAttachment(rule, message, PipelineStatus.PASSED));
    }

    @Override
    public void onFailed(PipelineRule rule, GoNotificationMessage message) throws Exception {
        updateSparkRoom(rule.getRoom());
        updateWebhookUrl(rule.getWebhookUrl());
        sendMessageToRooms(sparkAttachment(rule, message, PipelineStatus.FAILED));
    }

    @Override
    public void onBroken(PipelineRule rule, GoNotificationMessage message) throws Exception {
        updateSparkRoom(rule.getRoom());
        updateWebhookUrl(rule.getWebhookUrl());
        sendMessageToRooms(sparkAttachment(rule, message, PipelineStatus.BROKEN));
    }

    @Override
    public void onFixed(PipelineRule rule, GoNotificationMessage message) throws Exception {
        updateSparkRoom(rule.getRoom());
        updateWebhookUrl(rule.getWebhookUrl());
        sendMessageToRooms(sparkAttachment(rule, message, PipelineStatus.FIXED));
    }

    @Override
    public void onCancelled(PipelineRule rule, GoNotificationMessage message) throws Exception {
        updateSparkRoom(rule.getRoom());
        updateWebhookUrl(rule.getWebhookUrl());
        sendMessageToRooms(sparkAttachment(rule, message, PipelineStatus.CANCELLED));
    }

    private String sparkAttachment(PipelineRule rule, GoNotificationMessage message,
        PipelineStatus pipelineStatus) throws URISyntaxException {
        String title = String
            .format("Stage [%s] %s %s", message.fullyQualifiedJobName(), verbFor(pipelineStatus),
                pipelineStatus).replaceAll("\\s+", " ");
        StringBuilder buildAttachment = new StringBuilder("")
            .append(message.goServerUrl(rules.getGoServerHost()));

        List<String> consoleLogLinks = new ArrayList<String>();
        // Describe the build.
        try {
            Pipeline details = message.fetchDetails(rules);
            Stage stage = pickCurrentStage(details.stages, message);
            buildAttachment
                .append("\n" + "Triggered by: ")
                .append(stage.approvedBy);
            if (details.buildCause.triggerForced) {
                buildAttachment
                    .append("\n" + "Reason: Manual Trigger");
            } else {
                buildAttachment
                    .append("\n" + "Reason: ")
                    .append(details.buildCause.triggerMessage);
            }
            buildAttachment
                .append("\n" + "Label: ")
                .append(details.label);
            if (rules.getDisplayConsoleLogLinks()) {
                consoleLogLinks = createConsoleLogLinks(rules.getGoServerHost(), details, stage,
                    pipelineStatus);
            }
        } catch (Exception e) {
            buildAttachment
                .append("\n" + "(Couldn't fetch build details; see server log.) ");
            LOG.warn("Couldn't fetch build details", e);
        }
        buildAttachment
            .append("\n" + "Status: ")
            .append(pipelineStatus.name())
            .append("\n");

        // Describe the root changes that made up this build.
        if (rules.getDisplayMaterialChanges()) {
            try {
                List<MaterialRevision> changes = message.fetchChanges(rules);
                StringBuilder sb = new StringBuilder();
                for (MaterialRevision change : changes) {
                    boolean isTruncated = false;
                    if (rules.isTruncateChanges() && change.modifications.size()
                        > DEFAULT_MAX_CHANGES_PER_MATERIAL_IN_SPARK) {
                        change.modifications = Lists
                            .take(change.modifications, DEFAULT_MAX_CHANGES_PER_MATERIAL_IN_SPARK);
                        isTruncated = true;
                    }
                    for (Modification mod : change.modifications) {
                        String url = change.modificationUrl(mod);
                        if (url != null) {
                            sb.append("<").append(url).append("|").append(mod.revision).append(">");
                            sb.append(": ");
                        } else if (mod.revision != null) {
                            sb.append(mod.revision);
                            sb.append(": ");
                        }
                        String comment = mod.summarizeComment();
                        if (comment != null) {
                            sb.append(comment);
                        }
                        if (mod.userName != null) {
                            sb.append(" - ");
                            sb.append(mod.userName);
                        }
                        sb.append("\n");
                    }
                    String fieldNamePrefix = (isTruncated) ? String
                        .format("Latest %d", DEFAULT_MAX_CHANGES_PER_MATERIAL_IN_SPARK) : "All";
                    String fieldName = String
                        .format("%s changes for %s", fieldNamePrefix, change.material.description);
                    buildAttachment
                        .append(fieldName)
                        .append(sb.toString());
                }
            } catch (Exception e) {
                buildAttachment
                    .append("\n" + "Changes: ")
                    .append("(Couldn't fetch changes; see server log.)");
                LOG.warn("Couldn't fetch changes", e);
            }
        }

        if (!consoleLogLinks.isEmpty()) {
            String logLinks = Lists.mkString(consoleLogLinks, "", "", "\n");
            buildAttachment
                .append("\n" + "Console Logs: ")
                .append(logLinks);
        }

        if (!rule.getOwners().isEmpty()) {
            List<String> sparkOwners = Lists.map(rule.getOwners(), new Function<String, String>() {
                @Override
                public String apply(String input) {
                    return String.format("<@%s>", input);
                }
            });
            buildAttachment
                .append("\n" + "Owners: ")
                .append(Lists.mkString(sparkOwners, ","));
        }
        LOG.info("Pushing " + title + " notification to Spark");
        return buildAttachment.toString();
    }

    private List<String> createConsoleLogLinks(String host, Pipeline pipeline, Stage stage,
        PipelineStatus pipelineStatus) throws URISyntaxException {
        List<String> consoleLinks = new ArrayList<String>();
        for (String job : stage.jobNames()) {
            URI link;
            // We should be linking to Console Tab when the status is building,
            // while all others will be the console.log artifact.
            if (pipelineStatus == PipelineStatus.BUILDING) {
                link = new URI(String
                    .format("%s/go/tab/build/detail/%s/%d/%s/%d/%s#tab-console", host,
                        pipeline.name, pipeline.counter, stage.name, stage.counter, job));
            } else {
                link = new URI(String
                    .format("%s/go/files/%s/%d/%s/%d/%s/cruise-output/console.log", host,
                        pipeline.name, pipeline.counter, stage.name, stage.counter, job));
            }
            // TODO - May be it's only useful to show the failed job logs instead of all jobs?
            consoleLinks.add("<" + link.normalize().toASCIIString() + "| View " + job + " logs>");
        }
        return consoleLinks;
    }

    private Stage pickCurrentStage(Stage[] stages, GoNotificationMessage message) {
        for (Stage stage : stages) {
            if (message.getStageName().equals(stage.name)) {
                return stage;
            }
        }

        throw new IllegalArgumentException(
            "The list of stages from the pipeline (" + message.getPipelineName()
                + ") doesn't have the active stage (" + message.getStageName()
                + ") for which we got the notification.");
    }

    private String verbFor(PipelineStatus pipelineStatus) {
        switch (pipelineStatus) {
            case BROKEN:
            case FIXED:
            case BUILDING:
                return "is";
            case FAILED:
            case PASSED:
                return "has";
            case CANCELLED:
                return "was";
            default:
                return "";
        }
    }

    private void updateSparkRoom(String sparkRoom) {
        LOG.debug(String.format("Updating target spark room to %s", sparkRoom));
        // by default post it to where ever the hook is configured to do so
        if (startsWith(sparkRoom, "#")) {
            sendMessageToRooms(sparkRoom.substring(1));
        } else if (startsWith(sparkRoom, "@")) {
            sendMessageToRooms(sparkRoom.substring(1));
        }
    }

    private void updateWebhookUrl(String webbookUrl) {
        LOG.debug(String.format("Updating target webhookUrl to %s", webbookUrl));
        // by default pick the global webhookUrl
        if (StringUtils.isNotEmpty(webbookUrl)) {

            AtomicBoolean webhookFound = new AtomicBoolean(false);

            spark.webhooks().iterate().forEachRemaining(hook -> {
                if (hook.getTargetUrl().equals(webbookUrl)) {
                    webhookFound.set(true);
                }
            });

            if (!webhookFound.get()) {
                Webhook webhook = new Webhook();
                webhook.setName(rules.getSparkDisplayName());
                webhook.setResource("messages");
                webhook.setEvent("created");
                webhook.setFilter("mentionedPeople=me");
                webhook.setSecret(rules.getSparkWebHookSecret());
                webhook.setTargetUrl(URI.create(webbookUrl));
                webhook = spark.webhooks().post(webhook);
            }
        }
    }

    private void sendMessageToRooms(String sendMessage) {
        // List the rooms that I'm in
        try {
            spark.rooms()
                .iterate()
                .forEachRemaining(room -> {
                    // Post a text message to the room
                    Message message = new Message();
                    message.setRoomId(room.getId());
                    message.setText(sendMessage);
                    spark.messages().post(message);
                });
        } catch (NotAuthenticatedException e) {
            LOG.error("Cisco Spark Authentication failed: " + e.getMessage());
        }

    }
}
