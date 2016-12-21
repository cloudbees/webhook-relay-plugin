package org.jenkinsci.plugins.webhookrelay;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.util.logging.Logger;

public class WebhookRelayPlugin
implements Describable<WebhookRelayPlugin>, ExtensionPoint {

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
            //desc.load();
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
            //this.relayURI = relayURI;
            WebhookRelayStorage.relayURI = relayURI;
            LOGGER.info("Here we are XXX - " + relayURI);
        }

        public String getRelayURI() {
            return WebhookRelayStorage.relayURI;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData)
                throws hudson.model.Descriptor.FormException {

            WebhookRelayStorage.relayURI = formData.getString("relayURI");
            this.relayURI = WebhookRelayStorage.relayURI;

            WebhookRelayManager.getInstance().reconnect(WebhookRelayStorage.relayURI);
            save();
            return false;//super.configure(req, formData);
        }

        @Override
        public String getDisplayName() {
            return "Webhook Relay";
        }
    }

}
