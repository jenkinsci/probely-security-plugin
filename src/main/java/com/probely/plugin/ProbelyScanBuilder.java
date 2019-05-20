package com.probely.plugin;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.probely.api.*;
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
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;


public class ProbelyScanBuilder extends Builder implements SimpleBuildStep {

    private String targetId;
    private String credentialsId;

    @DataBoundConstructor
    public ProbelyScanBuilder(String targetId, String credentialsId) {
        this.targetId = targetId;
        this.credentialsId = credentialsId;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws IOException {
        Credentials credentials = CredentialsUtils.getStringCredentials(credentialsId, run);
        String authToken = CredentialsUtils.getSecret(credentials);
        if (authToken == null) {
            throw new AuthenticationException(Settings.ERR_CREDS_NOT_FOUND);
        }
        CloseableHttpClient httpClient = ApiUtils.buildHttpClient();
        ScanRequest scanRequest = new ScanRequest(authToken, Settings.API_TARGET_URL, targetId, httpClient);
        Scan scan = scanRequest.start();
        httpClient.close();
        log("Started scan on target: " + targetId + ". Status: " + scan.status, listener);
    }

    private void log(String msg, TaskListener listener) {
        listener.getLogger().println(msg);
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
                UserRequest request = new UserRequest(authToken, Settings.API_PROFILE_URL, httpClient);
                if (request.get() == null) {
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
