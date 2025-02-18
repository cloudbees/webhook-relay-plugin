package org.jenkinsci.plugins.webhookrelay;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class WebhookRelayStorage {
    @SuppressFBWarnings(value="MS_PKGPROTECT", justification="questionable design, should be fixed") // TODO FIXME
    public static String relayURI;
}