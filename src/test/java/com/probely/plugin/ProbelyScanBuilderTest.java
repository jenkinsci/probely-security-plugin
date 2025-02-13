package com.probely.plugin;

import com.probely.api.AuthenticationException;
import com.probely.api.Client;
import com.probely.api.ScanController;
import com.probely.api.UserController;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProbelyScanBuilderTest {

  private static final String targetId = System.getenv("PROBELY_TARGET_ID");
  private static final String authToken = System.getenv("PROBELY_API_TOKEN");

  @Test
  @WithJenkins
  void testBuildEmptyCredentials(JenkinsRule jenkins) throws Exception {
    FreeStyleProject project = jenkins.createFreeStyleProject();
    ProbelyScanBuilder builder = new ProbelyScanBuilder(targetId);

    project.getBuildersList().add(builder);
    FreeStyleBuild build = project.scheduleBuild2(0).get();
    jenkins.assertLogContains("Credentials not found", build);
  }

  @Test
  void testInvalidCredentials() {
    Client api = new Client(Settings.API_PROFILE_URL, "invalid_token");
    UserController userController = new UserController(api);
    AuthenticationException e = assertThrows(AuthenticationException.class, userController::get);
    assertEquals("Unauthorized", e.getMessage());
  }

  @Test
  void testScanController() throws Exception {
    Client api = new Client(Settings.API_TARGET_URL, authToken);
    ScanController scanController = new ScanController(api, targetId);
    scanController.start();
    scanController.refresh();
    scanController.waitForChanges(5);
    scanController.stop();
    scanController.close();
  }

  @Test
  void testUserController() throws Exception {
    Client api = new Client(Settings.API_PROFILE_URL, authToken);
    UserController userController = new UserController(api);
    userController.get();
  }

  @Test
  void testClient() {
    Client api = new Client(Settings.API_URL, authToken);
    CloseableHttpClient client = api.getHttpClient();
    assertNotNull(client);
  }

  @Test
  void testClientClose() {
    Client api = new Client(Settings.API_URL, authToken);
    api.close();
  }
}
