package de.microtema.maven.plugin.jenkinfile;

import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.*;

public class JenkinsfileGeneratorMojoTest {

    JenkinsfileGeneratorMojo sut;

    File dockerFile = new File("./Dockerfile");
    File jenkinsfile = new File("./Jenkinsfile");

    @Before
    public void setUp() {

        sut = new JenkinsfileGeneratorMojo();

        dockerFile.delete();
        jenkinsfile.delete();
    }

    @Test
    public void execute() throws Exception {

        sut.execute();

        assertTrue(jenkinsfile.exists());
    }

    @Test
    public void buildStages() {
        String stages = sut.buildStages();

        assertLinesEqual("stages {\n" +
                "stage('Initialize') {\n" +
                "\n" +
                "    steps {\n" +
                "        script {\n" +
                "            dir('bootstrap') {\n" +
                "                try {\n" +
                "                    git branch: env.BRANCH_NAME, url: env.BOOTSTRAP_URL, credentialsId: 'SCM_CREDENTIALS'\n" +
                "                } catch (e) {\n" +
                "                    sh \"echo unable to find branch! ${e}  retry with develop branch...\"\n" +
                "                    git branch: 'develop', url: '', credentialsId: 'SCM_CREDENTIALS'\n" +
                "                }\n" +
                "            }\n" +
                "            pipelineUtils = load './bootstrap/jenkins/pipeline-utils.groovy'\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Versioning') {\n" +
                "\n" +
                "    when {\n" +
                "        anyOf {\n" +
                "            branch 'release-*'\n" +
                "            branch 'bugfix-*'\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    steps {\n" +
                "        sh 'mvn release:update-versions -DdevelopmentVersion=2.1.0-SNAPSHOT $MAVEN_ARGS'\n" +
                "        sh 'mvn versions:set -DnewVersion=$VERSION-$CURRENT_TIME-$BUILD_NUMBER $MAVEN_ARGS'\n" +
                "    }\n" +
                "}\n" +
                "stage('Compile') {\n" +
                "    steps {\n" +
                "        script {\n" +
                "            sh 'mvn compile -U $MAVEN_ARGS'\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Test') {\n" +
                "    steps {\n" +
                "        sh 'mvn test $MAVEN_ARGS'\n" +
                "    }\n" +
                "\n" +
                "    post {\n" +
                "        always {\n" +
                "            junit 'target/surefire-reports/**/*Test.xml'\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Sonar Reports') {\n" +
                "    steps {\n" +
                "        sh 'mvn sonar:sonar -Dsonar.branch.name=$BRANCH_NAME $MAVEN_ARGS'\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Build [Maven-Artifact]') {\n" +
                "    steps {\n" +
                "       sh 'mvn install -Dmaven.test.skip=true -DskipTests=true $MAVEN_ARGS'\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Security Check') {\n" +
                "    steps {\n" +
                "        sh 'mvn dependency-check:help -P security $MAVEN_ARGS'\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Tag [Release]') {\n" +
                "\n" +
                "    when {\n" +
                "        branch 'master'\n" +
                "    }\n" +
                "\n" +
                "    steps {\n" +
                "        script {\n" +
                "            withGit {\n" +
                "                try {\n" +
                "                    sh 'git tag $VERSION $GIT_COMMIT'\n" +
                "                    sh 'git push origin $VERSION'\n" +
                "                } catch (e) {\n" +
                "\n" +
                "                    if (env.BRANCH_NAME != 'master') {\n" +
                "                        throw e\n" +
                "                    }\n" +
                "\n" +
                "                    sh 'echo there is already a tag for this version $VERSION'\n" +
                "                    sh \"echo ${e.toString()}\"\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Publish [Maven-Artifact]') {\n" +
                "    steps {\n" +
                "        script {\n" +
                "            try {\n" +
                "                 sh 'mvn deploy -Dmaven.test.skip=true -DskipTests=true $MAVEN_ARGS'\n" +
                "            } catch (e) {\n" +
                "\n" +
                "                if (env.BRANCH_NAME != 'master') {\n" +
                "                    throw e\n" +
                "                }\n" +
                "\n" +
                "                sh 'echo there is already a publication for this version $VERSION'\n" +
                "                sh \"echo ${e.toString()}\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Aqua Reports') {\n" +
                "\n" +
                "    environment {\n" +
                "        AQUA_PROJECT_ID = ''\n" +
                "        AQUA_PRODUCT_ID = ''\n" +
                "        AQUA_RELEASE = ''\n" +
                "        AQUA_LEVEL = ''\n" +
                "        JUNIT_TEST_AQUA_FOLDER_ID = ''\n" +
                "        INTEGRATION_TEST_AQUA_FOLDER_ID = ''\n" +
                "    }\n" +
                "\n" +
                "    when {\n" +
                "        anyOf {\n" +
                "            branch 'develop'\n" +
                "            branch 'release-*'\n" +
                "            branch 'master'\n" +
                "        }\n" +
                "    }\n" +
                "    steps {\n" +
                "        script {\n" +
                "            def sendToAqua = { file, folderId, testType ->\n" +
                "\n" +
                "                def response = sh(script: \"\"\"\n" +
                "                curl -X POST \\\n" +
                "                -H \"X-aprojectid: ${env.AQUA_PROJECT_ID}\" \\\n" +
                "                -H \"X-afolderid: ${folderId}\" \\\n" +
                "                -H \"X-aprodukt: ${env.AQUA_PRODUCT_ID}\" \\\n" +
                "                -H \"X-aausbringung: ${env.AQUA_RELEASE}\" \\\n" +
                "                -H \"X-astufe: ${env.AQUA_LEVEL}\" \\\n" +
                "                -H \"X-ateststufe: ${testType}\" \\\n" +
                "                -H \"X-commit: ${env.GIT_COMMIT}\" \\\n" +
                "                --data-binary @${file.path} \\\n" +
                "                \"http://ju2aqua.ju2aqua-itu.svc.cluster.local/stream/\"\n" +
                "                \"\"\", returnStdout: true)\n" +
                "\n" +
                "                if (response != 'OK') {\n" +
                "                    error \"Unable to report ${file.path} test in aqua ${folderId} folder!\"\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "            def reports = findFiles(glob: \"**/*Test.xml\")\n" +
                "            reports.each { sendToAqua(it, env.JUNIT_TEST_AQUA_FOLDER_ID, 'Komponententest') }\n" +
                "\n" +
                "            reports = findFiles(glob: \"**/*IT.xml\")\n" +
                "            reports.each { sendToAqua(it, env.INTEGRATION_TEST_AQUA_FOLDER_ID, 'Integrationstest') }\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "}", stages);
    }

    @Test
    public void buildStagesWithDockerFile() {

        dockerFile.mkdir();

        String stages = sut.buildStages();

        assertLinesEqual("stages {\n" +
                "stage('Initialize') {\n" +
                "\n" +
                "    steps {\n" +
                "        script {\n" +
                "            dir('bootstrap') {\n" +
                "                try {\n" +
                "                    git branch: env.BRANCH_NAME, url: env.BOOTSTRAP_URL, credentialsId: 'SCM_CREDENTIALS'\n" +
                "                } catch (e) {\n" +
                "                    sh \"echo unable to find branch! ${e}  retry with develop branch...\"\n" +
                "                    git branch: 'develop', url: '', credentialsId: 'SCM_CREDENTIALS'\n" +
                "                }\n" +
                "            }\n" +
                "            pipelineUtils = load './bootstrap/jenkins/pipeline-utils.groovy'\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Versioning') {\n" +
                "\n" +
                "    when {\n" +
                "        anyOf {\n" +
                "            branch 'release-*'\n" +
                "            branch 'bugfix-*'\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    steps {\n" +
                "        sh 'mvn release:update-versions -DdevelopmentVersion=2.1.0-SNAPSHOT $MAVEN_ARGS'\n" +
                "        sh 'mvn versions:set -DnewVersion=$VERSION-$CURRENT_TIME-$BUILD_NUMBER $MAVEN_ARGS'\n" +
                "    }\n" +
                "}\n" +
                "stage('Compile') {\n" +
                "    steps {\n" +
                "        script {\n" +
                "            sh 'mvn compile -U $MAVEN_ARGS'\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Test') {\n" +
                "    steps {\n" +
                "        sh 'mvn test $MAVEN_ARGS'\n" +
                "    }\n" +
                "\n" +
                "    post {\n" +
                "        always {\n" +
                "            junit 'target/surefire-reports/**/*Test.xml'\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Sonar Reports') {\n" +
                "    steps {\n" +
                "        sh 'mvn sonar:sonar -Dsonar.branch.name=$BRANCH_NAME $MAVEN_ARGS'\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Build [Maven-Artifact]') {\n" +
                "    steps {\n" +
                "       sh 'mvn install -Dmaven.test.skip=true -DskipTests=true $MAVEN_ARGS'\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Security Check') {\n" +
                "    steps {\n" +
                "        sh 'mvn dependency-check:help -P security $MAVEN_ARGS'\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Build [Docker-Image]') {\n" +
                "\n" +
                "    when {\n" +
                "        anyOf {\n" +
                "            branch 'develop'\n" +
                "            branch 'feature-*'\n" +
                "            branch 'release-*'\n" +
                "            branch 'master'\n" +
                "            environment name: 'DEPLOYABLE', value: 'true'\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    steps {\n" +
                "        buildDockerImage semVer: true\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Deployment') {\n" +
                "\n" +
                "    when {\n" +
                "        environment name: 'DEPLOYABLE', value: 'true'\n" +
                "    }\n" +
                "\n" +
                "    parallel {\n" +
                "        stage('ETU (develop)') {\n" +
                "            when {\n" +
                "                branch 'develop'\n" +
                "            }\n" +
                "            steps {\n" +
                "                withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {\n" +
                "                    withEnv([\"OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-etu\"]) {\n" +
                "                        createAndMergeOpsRepoMergeRequest()\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('ETU (release-*)') {\n" +
                "            when {\n" +
                "                branch 'release-*'\n" +
                "            }\n" +
                "            steps {\n" +
                "                withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {\n" +
                "                    withEnv([\"OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}\"]) {\n" +
                "                        createBranchIfNotExistsAndCommitChanges()\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('ETU (feature-*)') {\n" +
                "            when {\n" +
                "                branch 'feature-*'\n" +
                "            }\n" +
                "            steps {\n" +
                "                withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {\n" +
                "                    withEnv([\"OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-etu\"]) {\n" +
                "                        createBranchIfNotExistsAndCommitChanges()\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('ITU (master)') {\n" +
                "            when {\n" +
                "                branch 'master'\n" +
                "            }\n" +
                "            steps {\n" +
                "                withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {\n" +
                "                    withEnv([\"OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-itu\"]) {\n" +
                "                        createAndMergeOpsRepoMergeRequest()\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('SATU (master)') {\n" +
                "            when {\n" +
                "                branch 'master'\n" +
                "            }\n" +
                "            steps {\n" +
                "                withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {\n" +
                "                    withEnv([\"OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-satu\"]) {\n" +
                "                        createAndMergeOpsRepoMergeRequest()\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Aqua Reports') {\n" +
                "\n" +
                "    environment {\n" +
                "        AQUA_PROJECT_ID = ''\n" +
                "        AQUA_PRODUCT_ID = ''\n" +
                "        AQUA_RELEASE = ''\n" +
                "        AQUA_LEVEL = ''\n" +
                "        JUNIT_TEST_AQUA_FOLDER_ID = ''\n" +
                "        INTEGRATION_TEST_AQUA_FOLDER_ID = ''\n" +
                "    }\n" +
                "\n" +
                "    when {\n" +
                "        anyOf {\n" +
                "            branch 'develop'\n" +
                "            branch 'release-*'\n" +
                "            branch 'master'\n" +
                "        }\n" +
                "    }\n" +
                "    steps {\n" +
                "        script {\n" +
                "            def sendToAqua = { file, folderId, testType ->\n" +
                "\n" +
                "                def response = sh(script: \"\"\"\n" +
                "                curl -X POST \\\n" +
                "                -H \"X-aprojectid: ${env.AQUA_PROJECT_ID}\" \\\n" +
                "                -H \"X-afolderid: ${folderId}\" \\\n" +
                "                -H \"X-aprodukt: ${env.AQUA_PRODUCT_ID}\" \\\n" +
                "                -H \"X-aausbringung: ${env.AQUA_RELEASE}\" \\\n" +
                "                -H \"X-astufe: ${env.AQUA_LEVEL}\" \\\n" +
                "                -H \"X-ateststufe: ${testType}\" \\\n" +
                "                -H \"X-commit: ${env.GIT_COMMIT}\" \\\n" +
                "                --data-binary @${file.path} \\\n" +
                "                \"http://ju2aqua.ju2aqua-itu.svc.cluster.local/stream/\"\n" +
                "                \"\"\", returnStdout: true)\n" +
                "\n" +
                "                if (response != 'OK') {\n" +
                "                    error \"Unable to report ${file.path} test in aqua ${folderId} folder!\"\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "            def reports = findFiles(glob: \"**/*Test.xml\")\n" +
                "            reports.each { sendToAqua(it, env.JUNIT_TEST_AQUA_FOLDER_ID, 'Komponententest') }\n" +
                "\n" +
                "            reports = findFiles(glob: \"**/*IT.xml\")\n" +
                "            reports.each { sendToAqua(it, env.INTEGRATION_TEST_AQUA_FOLDER_ID, 'Integrationstest') }\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "}", stages);
    }

    @Test
    public void getTestStageName() {

        sut.aquaITFolderId = "123456";

        String stage = sut.getTestStageName();

        assertEquals("test", stage);
    }

    @Test
    public void getTestStageNameWillReturnTest() {

        String stage = sut.getTestStageName();

        assertEquals("unit-test", stage);
    }

    @Test
    public void buildTriggers() {

        String stage = sut.buildTriggers();

        assertNull(stage);
    }

    @Test
    public void buildTriggersWithUpstreamProjects() {

        sut.upstreamProjects = new String[]{"foo-bar"};

        String stage = sut.buildTriggers();

        assertEquals("triggers {\n" +
                "upstream(\n" +
                "  upstreamProjects: \"foo-bar/${env.BRANCH_NAME.replaceAll('/', '%2F')}\",\n" +
                "  threshold: hudson.model.Result.SUCCESS)\n" +
                "\n" +
                "}", stage);

    }

    @Test
    public void generateClosure() {

        assertEquals("stage {\n" +
                "}", sut.generateClosure("stage", null, ""));
    }

    @Test
    public void generateClosureWithName() {

        assertEquals("stage('Test') {\n" +
                "}", sut.generateClosure("stage", "Test", ""));
    }

    @Test
    public void fixSonarStage() {

        String stage = sut.fixSonarStage("mvn sonar:sonar -Dsonar.branch.name=$BRANCH_NAME $MAVEN_ARGS");

        assertEquals("mvn sonar:sonar -Dsonar.branch.name=$BRANCH_NAME $MAVEN_ARGS", stage);
    }

    @Test
    public void fixSonarStageWithExclusions() {

        sut.sonarExcludes = new String[]{"foo-bar"};

        String stage = sut.fixSonarStage("mvn sonar:sonar -Dsonar.branch.name=$BRANCH_NAME $MAVEN_ARGS");

        assertEquals("mvn sonar:sonar -pl !foo-bar -Dsonar.branch.name=$BRANCH_NAME $MAVEN_ARGS", stage);
    }

    @Test
    public void fixupInitializeStage() {

        sut.bootstrapUrl = "http://microtema.de";

        String stage = sut.fixupInitializeStage("BOOTSTRAP_URL = @BOOTSTRAP_URL@");

        assertEquals("BOOTSTRAP_URL = 'http://microtema.de'", stage);
    }

    @Test
    public void fixupEnvironment() {

        sut.baseNamespace = "app";

        String stage = sut.fixupEnvironment("BASE_NAMESPACE = @BASE_NAMESPACE@");

        assertEquals("BASE_NAMESPACE = 'app'", stage);
    }

    @Test
    public void fixupAquaStage() {

        sut.aquaProjectId = "1000";

        String stage = sut.fixupAquaStage("AQUA_PROJECT_ID = @AQUA_PROJECT_ID@");

        assertEquals("AQUA_PROJECT_ID = '1000'", stage);
    }

    @Test
    public void fixupDeploymentProd() {

        dockerFile.mkdir();

        sut.prodStages = new String[]{"ns-foo"};
        sut.supportedProdStages = new String[]{"foo", "bar"};

        String stage = sut.fixupDeploymentProd("foo = @foo@, bar = @bar@");
        assertEquals("foo = 'ns-foo', bar = 'false'", stage);
    }

    @Test
    public void getProdStage() {

        String stageName = "";
        assertEquals("false", sut.getProdStage(stageName));
    }


    @Test
    public void getProdStageWillReturnStage() {

        sut.prodStages = new String[]{
                "foo"
        };
        String stageName = "foo";
        assertEquals("foo", sut.getProdStage(stageName));
    }

    @Test
    public void getProdStageWillReturnFalse() {

        sut.prodStages = new String[]{
                "bar"
        };
        String stageName = "foo";
        assertEquals("false", sut.getProdStage(stageName));
    }

    @Test
    public void getStageOrNull() {

        assertNull(sut.getStageOrNull("stage{}", false));
    }

    @Test
    public void getStageOrNullWillReturnStage() {

        assertEquals("stage{}", sut.getStageOrNull("stage{}", true));
    }

    @Test
    public void existsDocker() {

        assertFalse(sut.existsDocker());
    }

    @Test
    public void existsDockerWillReturnTrue() {

        dockerFile.mkdir();

        assertTrue(sut.existsDocker());
    }

    @Test
    public void maskEnvironmentVariable() {

        assertEquals("'foo'", sut.maskEnvironmentVariable("foo"));
    }

    @Test
    public void getJenkinsTestStage() {

        String stage = sut.getJenkinsStage("test");

        assertNotNull(stage);

        assertLinesEqual("stage('Test') {\n" +
                "    parallel {\n" +
                "        stage('Unit Tests') {\n" +
                "            steps {\n" +
                "                sh 'mvn test $MAVEN_ARGS'\n" +
                "            }\n" +
                "\n" +
                "            post {\n" +
                "                always {\n" +
                "                    junit 'target/surefire-reports/**/*Test.xml'\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('Integration Tests') {\n" +
                "\n" +
                "            steps {\n" +
                "                sh 'mvn test-compile failsafe:integration-test $MAVEN_ARGS'\n" +
                "            }\n" +
                "\n" +
                "            post {\n" +
                "                always {\n" +
                "                    junit 'target/failsafe-reports/**/*IT.xml'\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}\n", stage);
    }

    @Test
    public void getJenkinsAgentStage() {

        String stage = sut.getJenkinsStage("agent");

        assertNotNull(stage);

        assertLinesEqual("agent {\n" +
                "    label 'mvn8'\n" +
                "}", stage);
    }

    void assertLinesEqual(String expectedString, String actualString) {
        BufferedReader expectedLinesReader = new BufferedReader(new StringReader(expectedString));
        BufferedReader actualLinesReader = new BufferedReader(new StringReader(actualString));

        try {
            int lineNumber = 0;

            String actualLine;
            while ((actualLine = actualLinesReader.readLine()) != null) {
                String expectedLine = expectedLinesReader.readLine();
                Assert.assertEquals("Line " + lineNumber, expectedLine, actualLine);
                lineNumber++;
            }

            if (expectedLinesReader.readLine() != null) {
                Assert.fail("Actual string does not contain all expected lines");
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        } finally {
            try {
                expectedLinesReader.close();
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
            try {
                actualLinesReader.close();
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        }
    }
}
