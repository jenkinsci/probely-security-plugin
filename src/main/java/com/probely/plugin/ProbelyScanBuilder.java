package com.probely.plugin;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.probely.api.*;
import com.probely.exceptions.ProbelyScanException;
import com.probely.util.ApiUtils;
import com.probely.util.CredentialsUtils;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import javax.annotation.Nonnull;
import java.io.IOException;


public class ProbelyScanBuilder extends Builder implements SimpleBuildStep {

    private final String targetId;
    private final String credentialsId;
    private boolean waitForScan;
    private boolean stopIfVulnerable;

    @DataBoundConstructor
    // Constructor parameters are bound to field names in "config.jelly"
    public ProbelyScanBuilder(String targetId, String credentialsId) {
        this.targetId = targetId;
        this.credentialsId = credentialsId;
        this.waitForScan = true;
        this.stopIfVulnerable = true;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public boolean getWaitForScan() {
        return waitForScan;
    }

    @DataBoundSetter
    public void setWaitForScan(boolean waitForScan) {
        this.waitForScan = waitForScan;
    }

    public boolean getStopIfVulnerable() {
        return stopIfVulnerable;
    }

    @DataBoundSetter
    public void setStopIfVulnerable(boolean stopIfVulnerable) {
        this.stopIfVulnerable = stopIfVulnerable;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws IOException {
        Credentials credentials = CredentialsUtils.getStringCredentials(credentialsId, run);
        String authToken = CredentialsUtils.getSecret(credentials);
        if (authToken == null) {
            throw new AuthenticationException(Settings.ERR_CREDS_NOT_FOUND);
        }
        log("Requesting scan for target: " + targetId, listener);
        CloseableHttpClient httpClient = ApiUtils.buildHttpClient();
        ScanController sc = new ScanController(authToken, Settings.API_TARGET_URL, targetId, httpClient);
        Scan scan = sc.start();
        log("Requested scan: " + scan, listener);
        try {
            if (waitForScan) {
                watchScan(sc, listener);
            }
        } finally {
            sc.close();
        }
    }

    private boolean watchScanStep(ScanController controller, TaskListener listener, ScanRules rules) throws ProbelyScanException {
        Scan scan = controller.waitForChanges(60);
        // Have vulnerabilities been found in the meanwhile?
        if (rules.isVulnerable(scan)) {
            if (stopIfVulnerable) {
                controller.stop();
            }
            String msg = "Target is vulnerable: " + scan;
            log(msg, listener);
            throw new ProbelyScanException(msg);
        }
        return scan.isRunning();
    }

    private void watchScan(ScanController controller, TaskListener listener) throws ProbelyScanException {
        ScanRules rules = new ScanRules(FindingSeverity.LOW);
        Scan scan = controller.getScan();
        while (watchScanStep(controller, listener, rules)) {
            scan = controller.getScan();
            log("Scan progress details: " + scan, listener);
        }
        log("Scan has finished. Details: " + scan, listener);
    }

    private void log(String msg, TaskListener listener) {
        listener.getLogger().println(Settings.PLUGIN_DISPLAY_NAME + ": " + msg);
    }

    @Symbol("probelyScan")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return Settings.PLUGIN_DISPLAY_NAME;
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item,
                                                     @QueryParameter final String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                    return result;
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) &&
                        !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result;
                }
            }
            return result
                    .includeEmptyValue()
                    .includeAs(ACL.SYSTEM, item, StringCredentials.class);
        }

        @POST
        public FormValidation doCheckCredentialsId(@AncestorInPath Item item,
                                                   @QueryParameter final String credentialsId) {
            if (item == null) {
                if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) &&
                        !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return FormValidation.ok();
                }
            }
            Credentials credentials = CredentialsUtils.getStringCredentials(credentialsId, item);
            String authToken = CredentialsUtils.getSecret(credentials);
            if (StringUtils.isBlank(credentialsId) || authToken == null) {
                return FormValidation.error(Settings.ERR_CREDS_INVALID);
            }

            int timeout = 5000;
            CloseableHttpClient httpClient = ApiUtils.buildHttpClient(timeout);
            String error = null;
            try {
                UserController uc = new UserController(authToken, Settings.API_PROFILE_URL, httpClient);
                if (uc.get() == null) {
                    error = Settings.ERR_CREDS_INVALID;
                }
            } catch (AuthenticationException aex) {
                error = Settings.ERR_CREDS_INVALID;
            } catch (IOException ioe) {
                error = Settings.ERR_API_CONN;
            } finally {
                ApiUtils.closeHttpClient(httpClient);
            }
            if (error == null) {
                return FormValidation.ok(Settings.MSG_CREDS_VALID);
            } else {
                return FormValidation.error(error);
            }
        }
    }
}
