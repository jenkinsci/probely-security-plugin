package com.probely.plugin;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.probely.api.*;
import com.probely.exceptions.ProbelyScanException;
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
import java.io.IOException;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.verb.POST;

public class ProbelyScanBuilder extends Builder implements SimpleBuildStep {

  private final String targetId;
  private String credentialsId;
  private String authToken;
  private boolean waitForScan;
  private boolean stopIfFailed;
  private FindingSeverity failThreshold;

  @DataBoundConstructor
  // Constructor parameters are bound to field names in "config.jelly"
  public ProbelyScanBuilder(String targetId) {
    this.targetId = targetId;
    this.waitForScan = true;
    this.stopIfFailed = true;
    this.failThreshold = FindingSeverity.MEDIUM;
  }

  public String getTargetId() {
    return targetId;
  }

  public String getCredentialsId() {
    return credentialsId;
  }

  @DataBoundSetter
  public void setCredentialsId(String credentialsId) {
    this.credentialsId = credentialsId;
  }

  @DataBoundSetter
  public void setAuthToken(String authToken) {
    this.authToken = authToken;
  }

  public boolean getWaitForScan() {
    return waitForScan;
  }

  @DataBoundSetter
  public void setWaitForScan(boolean waitForScan) {
    this.waitForScan = waitForScan;
  }

  public boolean getStopIfFailed() {
    return stopIfFailed;
  }

  @DataBoundSetter
  public void setStopIfFailed(boolean stopIfFailed) {
    this.stopIfFailed = stopIfFailed;
  }

  public String getFailThreshold() {
    return failThreshold.toString();
  }

  @DataBoundSetter
  public void setFailThreshold(String failThreshold) {
    this.failThreshold = FindingSeverity.fromString(failThreshold);
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
      throws IOException {
    String token = authToken;
    if (StringUtils.isBlank(token) && credentialsId != null) {
      Credentials credentials = CredentialsUtils.getStringCredentials(credentialsId, run);
      token = CredentialsUtils.getSecret(credentials);
    }
    if (StringUtils.isBlank(token)) {
      throw new AuthenticationException(Settings.ERR_CREDS_NOT_FOUND);
    }
    log("Requesting scan for target: " + targetId, listener);
    Client client = new Client(Settings.API_TARGET_URL, token);
    ScanController sc = new ScanController(client, targetId);
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

  private boolean watchScanStep(ScanController controller, TaskListener listener, ScanRules rules)
      throws ProbelyScanException {
    Scan scan = controller.waitForChanges(60);
    // Have vulnerabilities been found in the meanwhile?
    if (rules.isVulnerable(scan)) {
      if (stopIfFailed && scan.isRunning()) {
        log("Build failed. Stopping scan id: " + scan.id + "...", listener);
        try {
          controller.stop();
        } catch (IOException ioe) {
          log("Could not cancel scan id: " + scan.id + ": " + ioe, listener);
        }
      }
      String msg = "Target is vulnerable: " + scan;
      log(msg, listener);
      throw new ProbelyScanException(msg);
    }
    return scan.isRunning();
  }

  private void watchScan(ScanController controller, TaskListener listener)
      throws ProbelyScanException {
    ScanRules rules = new ScanRules(failThreshold);
    Scan scan;
    while (watchScanStep(controller, listener, rules)) {
      scan = controller.getScan();
      log("Scan progress details: " + scan, listener);
    }
    scan = controller.getScan();
    log("Scan has finished. Details: " + scan, listener);
  }

  private void log(String msg, TaskListener listener) {
    listener.getLogger().println(Settings.PLUGIN_DISPLAY_NAME + ": " + msg);
  }

  @Symbol("probelyScan")
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    private long lastEditorId = 0;

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    @Override
    @Nonnull
    public String getDisplayName() {
      return Settings.PLUGIN_DISPLAY_NAME;
    }

    @JavaScriptMethod
    public synchronized String createEditorId() {
      return String.valueOf(lastEditorId++);
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillCredentialsIdItems(
        @AncestorInPath Item item, @QueryParameter final String credentialsId) {
      StandardListBoxModel result = new StandardListBoxModel();
      if (item == null) {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
          return result;
        }
      } else {
        if (!item.hasPermission(Item.EXTENDED_READ)
            && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
          return result;
        }
      }
      return result.includeEmptyValue().includeAs(ACL.SYSTEM, item, StringCredentials.class);
    }

    @POST
    public FormValidation doCheckCredentialsId(
        @AncestorInPath Item item, @QueryParameter final String credentialsId) {
      if (item == null) {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
          return FormValidation.ok();
        }
      } else {
        if (!item.hasPermission(Item.EXTENDED_READ)
            && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
          return FormValidation.ok();
        }
      }
      Credentials credentials = CredentialsUtils.getStringCredentials(credentialsId, item);
      String token = CredentialsUtils.getSecret(credentials);
      if (StringUtils.isBlank(credentialsId) || token == null) {
        return FormValidation.error(Settings.ERR_CREDS_INVALID);
      }

      int timeout = 5;
      Client c = new Client(Settings.API_PROFILE_URL, token, timeout);
      String error = null;
      try {
        UserController uc = new UserController(c);
        if (uc.get() == null) {
          error = Settings.ERR_CREDS_INVALID;
        }
      } catch (AuthenticationException aex) {
        error = Settings.ERR_CREDS_INVALID;
      } catch (IOException ioe) {
        error = Settings.ERR_API_CONN;
      } finally {
        c.close();
      }
      if (error == null) {
        return FormValidation.ok(Settings.MSG_CREDS_VALID);
      } else {
        return FormValidation.error(error);
      }
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillFailThresholdItems() {
      ListBoxModel items = new ListBoxModel();
      for (FindingSeverity fs : FindingSeverity.values()) {
        String description = fs.toString();
        if (fs == FindingSeverity.LOW || fs == FindingSeverity.MEDIUM) {
          description += " or above";
        }
        items.add(description, fs.toString());
      }
      return items;
    }
  }
}
