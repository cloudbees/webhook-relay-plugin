package org.jenkinsci.plugins.webhookrelay;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.util.logging.Logger;

public class WebhookRelayPlugin implements Describable<WebhookRelayPlugin>, ExtensionPoint {

    private static final Logger LOGGER = Logger.getLogger(WebhookRelayPlugin.class.getName());

    public Descriptor<WebhookRelayPlugin> getDescriptor() {
        return (WebhookRelayPluginDescriptor) Jenkins.getInstance()
                .getDescriptorOrDie(getClass());
    }

    @Initializer(before = InitMilestone.COMPLETED)
    public static void init() {
        WebhookRelayPluginDescriptor desc = Jenkins.getInstance()
                .getDescriptorByType(WebhookRelayPluginDescriptor.class);
        if (desc != null) {
            WebhookRelayManager.getInstance().reconnect(WebhookRelayStorage.relayURI);
        }
    }

    @Extension
    public static class WebhookRelayPluginDescriptor extends
            Descriptor<WebhookRelayPlugin> {

        @Exported
        private String relayURI;


        public WebhookRelayPluginDescriptor() {
            load();
            WebhookRelayStorage.relayURI = relayURI;
            LOGGER.info("relayURI - " + relayURI);
        }

        public String getRelayURI() {
            return WebhookRelayStorage.relayURI;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) {

            WebhookRelayStorage.relayURI = formData.getString("relayURI");
            this.relayURI = WebhookRelayStorage.relayURI;

            WebhookRelayManager.getInstance().reconnect(WebhookRelayStorage.relayURI);
            save();
            return false;
        }

        @Override
        public String getDisplayName() {
            return "Webhook Relay";
        }
    }

}
