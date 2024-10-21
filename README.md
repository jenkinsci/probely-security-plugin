# Probely Security Scanner Plugin
This plugin uses [Probely](https://probely.com) to scan your web application for 
security vulnerabilities. It enables security testing in your CI/CD pipeline.

Probely is a Web Vulnerability Scanning suite for Agile Teams. It provides 
continuous scanning of your Web Applications and lets you efficiently manage 
the lifecycle of the vulnerabilities found. Some of its main features are:
 * Tests for more than 5000 vulnerabilities 
 * Authenticated scanning
 * Tailored instructions on how to fix the vulnerabilities (including snippets of code)
 * API for every single feature
 * Jira and Slack integration
 * PCI-DSS and OWASP compliance reports
 * Designed for developers, easy to use, easy to understand.
 * Re-test vulnerabilities, define custom headers, multiple users, CVSS score, scheduling, and more.

# Installing the plugin
Installing and setting up the plugin will take you less than 5 minutes.

1. Open Jenkins and click on **Manage Jenkins**

![Manage Jenkins](/images/install_plugin_1.png)

2. Click on **Manage Plugins**

![Manage Plugins](/images/install_plugin_2.png)

3. Click on the **Available** tab

![Available](/images/install_plugin_3.png)

4. On the **Filter** search box, enter **probely**
5. Select the **Probely Security Scanner** plugin
6. Click on **Download now and install after restart**
7. After Jenkins restarts, the plugin will be installed. Continue reading to setup the required Probely API key.

# Generating an API key

Before using the plugin, you first need to create an API key for Jenkins to be able to start a scan with Probely.

## On Probely Standard Version

1. Go to https://app.probely.com and log in
1. Select a target from the drop-down list
1. Go to the **Settings** page
1. Click on the **Integrations** tab
1. Write a name for the API Key. Since we want this API Key for Jenkins, we name it **Jenkins**
1. Click on **Generate New Key**

![Creating an API key](/images/new_api_key.png)

After the API key is created, please take note of the `Target ID` and API key values. They will be required to configure the Plugin credentials later on.

## On Probely Enterprise Version

1. Go to https://plus.probely.app/ and log in
1. Go to the **Target** tab and click on the target
1. Click on the **Settings** button
1. Click on the **Integrations** tab
1. Write a name for the API Key. Since we want this API Key for Jenkins, we name it **Jenkins**
1. Click on **Generate New Key**

![Creating an API key](/images/new_enterprise_api_key.png)

After the API key is created, please take note of the `Target ID` and API key values. They will be required to configure the Plugin credentials later on.

# Configuring the plugin

The plugin can be used both in a Freestyle or a in a Pipeline project. 
We will describe how to use the plugin in both project types.

## Configuring credentials

1. Click on **Credentials**
1. Click on the down arrow near **(global)** to enable the dropdown menu and choose 
**Add credentials**

![Add Credentials](/images/credentials_1.png)

1. On the Kind dropdown menu, choose **Secret text**
1. Enter the API key in the **Secret** textbox
1. Enter a value for the credentials in the **ID** textbox, for example **probely-test-site**
1. Enter an optional Description and click **OK**

![Add Secret](/images/credentials_2.png)

## Using the plugin in a Freestyle project

Freestyle Jenkins projects have been traditionally used to create pipeline-like setups by chaining build steps together. To enable Probely in a Freestyle project, the following steps may be used.

1. Click on **New Item**

![New Item](/images/new_item.png)

2. Enter your project name, choose **Freestyle Project** and click **OK**

![Freestyle Project](/images/freestyle_0.png)

3. Add Probely scan step

We assume that all required steps have been properly configured, such as checking out from your SCM, testing, among others.

1. Add the `Target ID`, as defined in the **Generating an API key** step.
1. Select the right credentials, which were configured in **Configuring credentials**. If the connection to Probely's API is working correctly, and the credentials are valid, you should see the message "Credentials verified successfully".
1. Set additional scan options, if needed. Some examples: waiting for the scan to finish before moving on to the next step, and failing the build if vulnerabilities are found.
1. When all steps are properly configured, click on **Save**

![Probely step](/images/freestyle_1.png)

## Using the plugin in a Pipeline project

Pipeline projects are the new way to create build/test/deploy (and more) pipelines in Jenkins. Pipelines are defined in a `Jenkinfile`, which can be configured in an older imperative syntax, or in a more modern declarative syntax. We describe how to use Probely using a declarative `Jenkinsfile` to build a simple build/test/scan Pipeline.

1. Click on **New Item**

![New Item](/images/new_item.png)

2. Enter your project name, choose **Pipeline Project** and click **OK**

![Pipeline](/images/pipeline_0.png)

3. Create a Jenkinsfile

We assume that the step to check out source code from your SCM is properly configured.
The following `Jenkinsfile` may be used as an example to add Probely to your pipeline.
It should be placed in the root of your source code repository.
This pipeline consists of an hypothetical Java project, built with Gradle, with two stages: 
one running unit tests and the other launching a scan with Probely. 
Note that the `targetId` and `credentialsId` value refer to the credentials configured previously.

```
pipeline {
    agent {
        docker {
            image 'maven:3-alpine' 
        }
    }
    stages {
        stage('Unit tests') { 
            steps {
                sh './gradlew check'
            }
        }
        stage('Scan with Probely') {
            steps {
                probelyScan targetId: '9nl6yy0TWWKv', credentialsId: 'probely-test-site', waitForScan: true, stopIfFailed: true, failThreshold: 'medium'
            }
         }
    }
}
```


> **Note**: if you already have a mechanism to securely store credentials
> (such as HashiCorp's Vault), you can pass the API Key value directly
> to the plugin, using the `authToken` parameter, as opposed to `credentialsId`.


4. Configure Jenkins to use the Jenkins file on your repository

![Pipeline using Jenkinsfile](/images/pipeline_1.png)


# Building and Contributing

Contributions are very welcome. To build the plugin, be sure to install the Java Development Kit (JDK) 1.8 and Maven.

A minimal example on how to build and run the plugin on Ubuntu Linux follows. It will also build on macOS and Windows, provided you have the required packages installed. After running the commands below, you will have a test Jenkins instance running with the plugin.

```bash

sudo apt install openjdk-11-jdk-headless maven
git clone https://github.com/jenkinsci/probely-security-plugin.git
cd probely-security-plugin
mvn clean verify
mvn hpi:run
```
