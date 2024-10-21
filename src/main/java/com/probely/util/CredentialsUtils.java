package com.probely.util;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.model.Item;
import hudson.model.Run;
import hudson.security.ACL;
import org.acegisecurity.Authentication;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

public class CredentialsUtils {
  public static String getSecret(Credentials credentials) {
    if (credentials == null) {
      return null;
    }
    if (!(credentials instanceof StringCredentials)) {
      return null;
    }
    StringCredentials strCredentials = (StringCredentials) credentials;
    String secret = strCredentials.getSecret().getPlainText();
    if (secret == null || secret.isEmpty()) {
      return null;
    }
    return secret;
  }

  public static Credentials getStringCredentials(String credentialsId, Run<?, ?> run) {
    return CredentialsProvider.findCredentialById(credentialsId, StringCredentials.class, run);
  }

  public static Credentials getStringCredentials(String credentialsId, Item item) {
    Authentication acl = ACL.SYSTEM;
    DomainRequirement requirement = new DomainRequirement();
    return CredentialsMatchers.firstOrNull(
        CredentialsProvider.lookupCredentials(StringCredentials.class, item, acl, requirement),
        CredentialsMatchers.withId(credentialsId));
  }
}
