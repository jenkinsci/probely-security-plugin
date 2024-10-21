package com.probely.plugin;

import com.probely.api.AuthenticationException;
import com.probely.api.Client;
import com.probely.api.ScanController;
import com.probely.api.UserController;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ProbelyScanBuilderTest {

  private static final String targetId = System.getenv("PROBELY_TARGET_ID");
  private static final String authToken = System.getenv("PROBELY_API_TOKEN");

  @Rule public JenkinsRule jenkins = new JenkinsRule();

  @Test
  public void testBuildEmptyCredentials() throws Exception {
    FreeStyleProject project = jenkins.createFreeStyleProject();
    ProbelyScanBuilder builder = new ProbelyScanBuilder(targetId);

    project.getBuildersList().add(builder);
    FreeStyleBuild build = project.scheduleBuild2(0).get();
    jenkins.assertLogContains("Credentials not found", build);
  }

  @Test
  public void testInvalidCredentials() throws Exception {
    Client api = new Client(Settings.API_PROFILE_URL, "invalid_token");
    UserController userController = new UserController(api);
    try {
      userController.get();
    } catch (AuthenticationException e) {
      assert e.getMessage().equals("Unauthorized");
    }
  }

  @Test
  public void testScanController() throws Exception {
    Client api = new Client(Settings.API_TARGET_URL, authToken);
    ScanController scanController = new ScanController(api, targetId);
    scanController.start();
    scanController.refresh();
    scanController.waitForChanges(5);
    scanController.stop();
    scanController.close();
  }

  @Test
  public void testUserController() throws Exception {
    Client api = new Client(Settings.API_PROFILE_URL, authToken);
    UserController userController = new UserController(api);
    userController.get();
  }

  @Test
  public void testClient() throws Exception {
    Client api = new Client(Settings.API_URL, authToken);
    CloseableHttpClient client = api.getHttpClient();
    assert client != null;
  }

  @Test
  public void testClientClose() throws Exception {
    Client api = new Client(Settings.API_URL, authToken);
    api.close();
  }
}
