package com.probely.plugin;

import com.probely.api.ScanRequest;
import com.probely.util.ApiUtils;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ProbelyScanBuilderTest {

    private final static String targetId = "test-target";
    private final static String authToken = "test-token";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testBuildEmptyCredentials() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        ProbelyScanBuilder builder = new ProbelyScanBuilder(targetId, authToken);

        project.getBuildersList().add(builder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("Credentials not found", build);
    }

    @Test
    public void testInvalidCredentials() throws Exception {
        CloseableHttpClient client = ApiUtils.buildHttpClient(5000);
        try {
            ScanRequest sr = new ScanRequest(authToken, Settings.API_PROFILE_URL, targetId, client);
            sr.start();
        } catch (ClientProtocolException cex) {
            // Expected
        } finally {
            ApiUtils.closeHttpClient(client);
        }
    }
}