package de.microtema.maven.plugin.jenkinfile;


import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JenkinsfileGeneratorMojoTest {

    @InjectMocks
    JenkinsfileGeneratorMojo sut;

    @Mock
    JenkinsfileGeneratorService service;

    @Mock
    MavenProject project;

    File jenkinsfile = new File("./Jenkinsfile");

    @BeforeEach
    void setUp() {

        sut.project = project;
        sut.service = service;
        sut.appName = "app";
        sut.baseNamespace = "ns";
    }

    @AfterEach
    void tearDown() {

        jenkinsfile.delete();
    }

    @Test
    void executeOnNonGitRepo() {

        when(service.isGitRepo(project)).thenReturn(false);

        sut.execute();

        assertFalse(jenkinsfile.exists());
    }

    @Test
    void executeOnNonUpdateFalse() {

        when(service.isGitRepo(project)).thenReturn(true);

        sut.update = false;

        sut.execute();

        assertFalse(jenkinsfile.exists());
    }

    @Test
    void execute() {

        when(service.getRootPath(project)).thenReturn(".");
        when(service.isGitRepo(project)).thenReturn(true);

        sut.update = true;
        sut.prodStages = Arrays.asList("foo", "nf-bar");

        sut.execute();

        assertTrue(jenkinsfile.exists());
    }

    @Test
    void getTestStageName() {

        sut.aquaITFolderId = "123456";

        String stage = sut.getTestStageName();

        assertEquals("test", stage);
    }

    @Test
    void getTestStageNameWillReturnTest() {

        String stage = sut.getTestStageName();

        assertEquals("unit-test", stage);
    }

    @Test
    void fixupAquaStage() {

        when(service.hasSourceCode(project)).thenReturn(true);

        sut.aquaProjectId = "1000";

        String stage = sut.fixupAquaStage("AQUA_PROJECT_ID = @AQUA_PROJECT_ID@");

        assertEquals("AQUA_PROJECT_ID = '1000'", stage);
    }


    @Test
    void getStageOrNull() {

        assertNull(sut.getStageOrNull("stage {}", false));
    }

    @Test
    void getStageOrNullWillReturnStage() {

        assertEquals("stage{}", sut.getStageOrNull("stage{}", true));
    }

    @Test
    void maskEnvironmentVariable() {

        assertEquals("'foo'", sut.maskEnvironmentVariable("foo"));
    }

    @Test
    void initDefaults() {

        sut.initDefaults();

        assertEquals(3, sut.stages.size());
    }

    @Test
    void initDefaultsWillBeIgnored() {

        sut.stages.put("foo", "bar");

        sut.initDefaults();

        assertEquals(1, sut.stages.size());
    }

    @Test
    void paddLine() {

        assertEquals("    stage('name') {}", sut.paddLine("stage('name') {}", 4));
    }

    @Test
    void testGetUnitTestStageName() {

        assertEquals("unit-test", sut.getTestStageName());
    }

    @Test
    void testGetTestStageName() {

        sut.aquaITFolderId = "12345";

        assertEquals("test", sut.getTestStageName());
    }

    @Test
    void buildTriggersOnEmptyUpstreams() {

        String answer = sut.buildTriggers();

        assertEquals("triggers {\n" +
                "    upstream upstreamProjects: \"\", threshold: hudson.model.Result.SUCCESS\n" +
                "}\n", answer);
    }

    @Test
    void buildTriggers() {

        sut.upstreamProjects = Arrays.asList("foo");

        String answer = sut.buildTriggers();

        assertEquals("triggers {\n" +
                "    upstream upstreamProjects: \"foo/${env.BRANCH_NAME.replaceAll('/', '%2F')}\", threshold: hudson.model.Result.SUCCESS\n" +
                "}\n", answer);
    }

    @Test
    void buildTriggersOnMultiplesUpstreams() {

        sut.upstreamProjects = Arrays.asList("foo", "bar");

        String answer = sut.buildTriggers();

        assertEquals("triggers {\n" +
                "    upstream upstreamProjects: \"foo/${env.BRANCH_NAME.replaceAll('/', '%2F')},bar/${env.BRANCH_NAME.replaceAll('/', '%2F')}\", threshold: hudson.model.Result.SUCCESS\n" +
                "}\n", answer);
    }

    @Test
    void fixSonarStageWillReturnNull() {

        when(service.hasSourceCode(project)).thenReturn(true);

        String answer = sut.fixSonarStage("");

        assertNull(null, answer);
    }

    @Test
    void fixSonarStage() {

        when(service.hasSourceCode(project)).thenReturn(true);
        when(service.hasSonarProperties(project)).thenReturn(true);

        String answer = sut.fixSonarStage("mvn sonar:sonar -Dsonar.branch.name=$BRANCH_NAME $MAVEN_ARGS");

        assertEquals("mvn sonar:sonar -Dsonar.branch.name=$BRANCH_NAME $MAVEN_ARGS", answer);
    }

    @Test
    void fixSonarStageWithExcludes() {

        when(service.hasSourceCode(project)).thenReturn(true);
        when(service.hasSonarProperties(project)).thenReturn(true);
        when(service.getSonarExcludes(project)).thenReturn(Collections.singletonList("foo"));

        String answer = sut.fixSonarStage("mvn sonar:sonar -Dsonar.branch.name=$BRANCH_NAME $MAVEN_ARGS");

        assertEquals("mvn sonar:sonar -pl !foo -Dsonar.branch.name=$BRANCH_NAME $MAVEN_ARGS", answer);
    }

    @Test
    void fixSonarStageWithMultiplesExcludes() {

        when(service.hasSourceCode(project)).thenReturn(true);
        when(service.hasSonarProperties(project)).thenReturn(true);
        when(service.getSonarExcludes(project)).thenReturn(Arrays.asList("foo", "bar"));

        String answer = sut.fixSonarStage("mvn sonar:sonar -Dsonar.branch.name=$BRANCH_NAME $MAVEN_ARGS");

        assertEquals("mvn sonar:sonar -pl !foo !bar -Dsonar.branch.name=$BRANCH_NAME $MAVEN_ARGS", answer);
    }

    @Test
    void fixupEnvironment() {

        when(service.existsDockerfile(project)).thenReturn(false);

        String answer = sut.getJenkinsStage("environment");

        assertEquals("environment {\n" +
                "    CURRENT_TIME = sh(script: 'date +%Y-%m-%d-%H-%M', returnStdout: true).trim()\n" +
                "    CHANGE_AUTHOR_EMAIL = sh(script: \"git --no-pager show -s --format='%ae'\", returnStdout: true).trim()\n" +
                "\n" +
                "}\n", answer);
    }

    @Test
    void fixupEnvironmentWithEnvs() {

        when(service.existsDockerfile(project)).thenReturn(true);

        sut.bootstrapUrl = "http://localhost";

        String answer = sut.getJenkinsStage("environment");

        assertEquals("environment {\n" +
                "    CURRENT_TIME = sh(script: 'date +%Y-%m-%d-%H-%M', returnStdout: true).trim()\n" +
                "    CHANGE_AUTHOR_EMAIL = sh(script: \"git --no-pager show -s --format='%ae'\", returnStdout: true).trim()\n" +
                "    APP = 'app'\n" +
                "    BASE_NAMESPACE = 'ns'\n" +
                "    DEPLOYABLE = sh(script: 'oc whoami', returnStdout: true).trim().startsWith(\"system:serviceaccount:${env.BASE_NAMESPACE}\")\n" +
                "}\n",answer);
    }

    @Test
    void fixupInitializeStage() {

        String answer = sut.getJenkinsStage("initialize");

        assertEquals("stage('Initialize') {\n" +
                "\n" +
                "    environment {\n" +
                "        BOOTSTRAP_URL = ''\n" +
                "    }\n" +
                "\n" +
                "    steps {\n" +
                "\n" +
                "        sh 'whoami'\n" +
                "        sh 'oc whoami'\n" +
                "        sh 'mvn -version'\n" +
                "        sh 'echo commit-id: $GIT_COMMIT'\n" +
                "        sh 'echo change author: $CHANGE_AUTHOR_EMAIL'\n" +
                "    }\n" +
                "}\n",answer);
    }

    @Test
    void fixupInitializeStageWillBootstrapUrl() {

        sut.bootstrapUrl = "localhost";

        String answer = sut.getJenkinsStage("initialize");

        assertEquals("stage('Initialize') {\n" +
                "\n" +
                "    environment {\n" +
                "        BOOTSTRAP_URL = 'localhost'\n" +
                "    }\n" +
                "\n" +
                "    steps {\n" +
                "        script {\n" +
                "            if (env.BOOTSTRAP_URL.toLowerCase() == env.GIT_URL.toLowerCase()) {\n" +
                "                env.MAVEN_ARGS = '-s ./settings.xml'\n" +
                "            } else {\n" +
                "                dir('bootstrap') {\n" +
                "                    try {\n" +
                "                        git branch: env.BRANCH_NAME, url: env.BOOTSTRAP_URL, credentialsId: 'SCM_CREDENTIALS'\n" +
                "                    } catch (e) {\n" +
                "                        git branch: 'develop', url: env.BOOTSTRAP_URL, credentialsId: 'SCM_CREDENTIALS'\n" +
                "                    }\n" +
                "                    env.MAVEN_ARGS = '-s ./bootstrap/settings.xml'\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        sh 'whoami'\n" +
                "        sh 'oc whoami'\n" +
                "        sh 'mvn -version'\n" +
                "        sh 'echo commit-id: $GIT_COMMIT'\n" +
                "        sh 'echo change author: $CHANGE_AUTHOR_EMAIL'\n" +
                "    }\n" +
                "}\n", answer);
    }

    @Test
    void fixupReadinessStage() {

        when(service.existsDockerfile(project)).thenReturn(false);

        assertNull(sut.fixupReadinessStage("foo"));
    }


    @Test
    void fixupReadinessStageWillReturnTemplate() {

        when(service.existsDockerfile(project)).thenReturn(true);

        assertEquals("template", sut.fixupReadinessStage("template"));
    }

    @Test
    void fixupReadinessStageWithStages() {

        sut.stages.put("foo", "develop");

        when(service.existsDockerfile(project)).thenReturn(true);

        assertEquals("\n" +
                "        stage('FOO (develop)') {\n" +
                "        \n" +
                "            environment {\n" +
                "                STAGE_NAME = 'foo'\n" +
                "                NAMESPACE = \"${env.BASE_NAMESPACE}-${env.STAGE_NAME}\"\n" +
                "            }\n" +
                "        \n" +
                "            options {\n" +
                "                timeout(time: 10, unit: 'MINUTES')\n" +
                "            }\n" +
                "        \n" +
                "            when {\n" +
                "                branch 'develop'\n" +
                "            }\n" +
                "        \n" +
                "            steps {\n" +
                "        \n" +
                "                script {\n" +
                "                     Throwable caughtException = null\n" +
                "        \n" +
                "                     try {\n" +
                "                         catchError(buildResult: 'SUCCESS', stageResult: 'ABORTED') {\n" +
                "                             waitForReadiness()\n" +
                "                         }\n" +
                "                     } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {\n" +
                "                         error \"Caught ${e.toString()}\"\n" +
                "                     } catch (Throwable e) {\n" +
                "                         caughtException = e\n" +
                "                     }\n" +
                "        \n" +
                "                     if (caughtException) {\n" +
                "                         error caughtException.message\n" +
                "                     }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n", sut.fixupReadinessStage("@STAGES@"));
    }

    @Test
    void fixupDeploymentStageWithStages() {

        sut.stages.put("dev", "develop");

        when(service.existsDockerfile(project)).thenReturn(true);

        assertEquals("stage('Deployment') {\n" +
                "\n" +
                "    when {\n" +
                "        environment name: 'DEPLOYABLE', value: 'true'\n" +
                "    }\n" +
                "\n" +
                "    parallel {\n" +
                "\n" +
                "        stage('DEV (develop)') {\n" +
                "            environment {\n" +
                "                STAGE_NAME = 'dev'\n" +
                "                BRANCH_PATTERN = 'develop'\n" +
                "            }\n" +
                "            when {\n" +
                "                allOf {\n" +
                "                    branch env.BRANCH_PATTERN\n" +
                "                    environment name: 'DEPLOYABLE', value: 'true'\n" +
                "                }\n" +
                "            }\n" +
                "            steps {\n" +
                "                withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {\n" +
                "                    withEnv([\"OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-${env.STAGE_NAME}\"]) {\n" +
                "                        createAndMergeOpsRepoMergeRequest()\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "    }\n" +
                "}\n", sut.getJenkinsStage("deployment"));
    }

    @Test
    void fixupDeplymentStageWithMultipleStages() {

        sut.stages.put("qa", "release-*,hotfix-*");

        when(service.existsDockerfile(project)).thenReturn(true);

        assertEquals("stage('Deployment') {\n" +
                "\n" +
                "    when {\n" +
                "        environment name: 'DEPLOYABLE', value: 'true'\n" +
                "    }\n" +
                "\n" +
                "    parallel {\n" +
                "\n" +
                "        stage('QA (release-*)') {\n" +
                "            environment {\n" +
                "                STAGE_NAME = 'qa'\n" +
                "                BRANCH_PATTERN = 'release-*'\n" +
                "            }\n" +
                "            when {\n" +
                "                allOf {\n" +
                "                    branch env.BRANCH_PATTERN\n" +
                "                    environment name: 'DEPLOYABLE', value: 'true'\n" +
                "                }\n" +
                "            }\n" +
                "            steps {\n" +
                "                withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {\n" +
                "                    withEnv([\"OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-${env.STAGE_NAME}\"]) {\n" +
                "                        createAndMergeOpsRepoMergeRequest()\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('QA (hotfix-*)') {\n" +
                "            environment {\n" +
                "                STAGE_NAME = 'qa'\n" +
                "                BRANCH_PATTERN = 'hotfix-*'\n" +
                "            }\n" +
                "            when {\n" +
                "                allOf {\n" +
                "                    branch env.BRANCH_PATTERN\n" +
                "                    environment name: 'DEPLOYABLE', value: 'true'\n" +
                "                }\n" +
                "            }\n" +
                "            steps {\n" +
                "                withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {\n" +
                "                    withEnv([\"OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-${env.STAGE_NAME}\"]) {\n" +
                "                        createAndMergeOpsRepoMergeRequest()\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "    }\n" +
                "}\n", sut.getJenkinsStage("deployment"));
    }

    @Test
    void fixupDbMigrationStage() {

        when(service.existsDbMigrationScripts(project)).thenReturn(true);

        assertEquals("template", sut.fixupDbMigrationStage("template"));
    }

    @Test
    void fixupDbMigrationStageWithStages() {

        sut.stages.put("dev", "develop");

        when(service.existsDbMigrationScripts(project)).thenReturn(true);

        assertEquals("stage('DB Migration') {\n" +
                "\n" +
                "    when {\n" +
                "        environment name: 'DEPLOYABLE', value: 'true'\n" +
                "    }\n" +
                "\n" +
                "    parallel {\n" +
                "\n" +
                "      stage('DEV (develop)') {\n" +
                "      \n" +
                "          environment {\n" +
                "              MAVEN_PROFILE = 'dev'\n" +
                "          }\n" +
                "      \n" +
                "          when {\n" +
                "              branch 'develop'\n" +
                "          }\n" +
                "      \n" +
                "          steps {\n" +
                "              sh 'mvn flyway:migrate -P $MAVEN_PROFILE -Doracle.jdbc.fanEnabled=false $MAVEN_ARGS'\n" +
                "          }\n" +
                "      }\n" +
                "\n" +
                "    }\n" +
                "}\n", sut.getJenkinsStage("db-migration"));
    }

    @Test
    void testFixupAquaStageWithJUnitFolderId() {

        when(service.hasSourceCode(project)).thenReturn(true);

        sut.aquaJunitFolderId = "123456";

        assertEquals("\n" +
                "        stage('Unit Tests') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    def reports = findFiles glob: '**/*Test.xml'\n" +
                "                    reports.each { sendToAqua it, '123456', 'Komponententest' }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n", sut.fixupAquaStage("@STAGES@"));
    }

    @Test
    void testFixupAquaStageWithFolderId() {

        when(service.hasSourceCode(project)).thenReturn(true);

        sut.aquaJunitFolderId = "123456";
        sut.aquaITFolderId = "654321";

        assertEquals("\n" +
                "        stage('Unit Tests') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    def reports = findFiles glob: '**/*Test.xml'\n" +
                "                    reports.each { sendToAqua it, '123456', 'Komponententest' }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('Integration Tests') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    def reports = findFiles glob: '**/*IT.xml'\n" +
                "                    reports.each { sendToAqua it, '654321', 'Integrationstest' }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n", sut.fixupAquaStage("@STAGES@"));
    }

    @Test
    void fixupDeploymentProd() {

        assertNull(sut.fixupDeploymentProd("template"));
    }

    @Test
    void fixupDeploymentProdWithStages() {

        when(service.existsDockerfile(project)).thenReturn(true);

        sut.prodStages = Collections.singletonList("aws");

        String answer = sut.fixupDeploymentProd("@STAGES@");

        assertEquals("\n" +
                "        stage('AWS') {\n" +
                "            steps {\n" +
                "                createOpsRepoMergeRequest opsRepositoryName: 'aws'\n" +
                "            }\n" +
                "        }\n", answer);
    }

    @Test
    void testGetStageOrNullWillReturnNull() {

        assertNull(sut.getStageOrNull("foo", false));
    }

    @Test
    void testGetStageOrNullWillReturnTemplate() {

        assertEquals("template", sut.getStageOrNull("template", true));
    }

    @Test
    void testMaskEnvironmentVariable() {

        assertEquals("'foo'", sut.maskEnvironmentVariable("foo"));
    }

    @Test
    void getJenkinsStage() {

        String answer = sut.getJenkinsStage("agent");

        assertEquals("agent {\n" +
                "    label 'jdk8'\n" +
                "}\n", answer);
    }


    @Test
    void getStageNameWithDashes() {

        assertEquals("BAR", sut.getStageName("foo-bar"));
    }

    @Test
    void getStageName() {

        assertEquals("FOO", sut.getStageName("foo"));
    }

    @Test
    void getFunctionsOnEmpty() {

        assertEquals(StringUtils.EMPTY, sut.getFunctions());
    }

    @Test
    void getFunctions() {

        sut.functionTemplates.add(sut.getJenkinsStage("aqua-function"));

        assertEquals("\n" +
                "def sendToAqua(file, folderId, testType) {\n" +
                "\n" +
                "    def response = sh script: \"\"\"\n" +
                "    curl -X POST \\\n" +
                "    -H \"X-aprojectid: ${env.AQUA_PROJECT_ID}\" \\\n" +
                "    -H \"X-afolderid: ${folderId}\" \\\n" +
                "    -H \"X-aprodukt: ${env.AQUA_PRODUCT_ID}\" \\\n" +
                "    -H \"X-aausbringung: ${env.AQUA_RELEASE}\" \\\n" +
                "    -H \"X-astufe: ${env.AQUA_LEVEL}\" \\\n" +
                "    -H \"X-ateststufe: ${testType}\" \\\n" +
                "    -H \"X-commit: ${env.GIT_COMMIT}\" \\\n" +
                "    --data-binary @${file.path} \\\n" +
                "    \"${env.AQUA_URL}\"\n" +
                "    \"\"\", returnStdout: true\n" +
                "\n" +
                "    if (response != 'OK') {\n" +
                "        error \"Unable to report ${file.path} test in aqua ${folderId} folder!\"\n" +
                "    }\n" +
                "}\n", sut.getFunctions());
    }

    @Test
    void fixSonarQualityGateStage() {

        when(service.hasSourceCode(any())).thenReturn(false);

        assertNull(sut.fixSonarQualityGateStage(""));
    }

    @Test
    void fixSonarQualityGateStageWillReturnFalse() {

        when(service.hasSourceCode(any())).thenReturn(true);
        when(service.hasSonarProperties(any())).thenReturn(false);

        assertNull(sut.fixSonarQualityGateStage(""));
    }

    @Test
    void fixSonarQualityGateStageWillReturnTrue(@Mock MavenProject project, @Mock Properties properties) {

        when(service.hasSourceCode(any())).thenReturn(true);
        when(service.hasSonarProperties(any())).thenReturn(true);
        when(project.getProperties()).thenReturn(properties);
        when(properties.getProperty("sonar.login")).thenReturn("123456789");

        sut.project = project;

        assertEquals("'123456789'", sut.fixSonarQualityGateStage("@SONAR_TOKEN@"));
    }

    @Test
    void fixupPerformanceTestStage() {

        when(service.existsJmeterFile(any())).thenReturn(false);

        assertNull(sut.fixupPerformanceTestStage(""));
    }

    @Test
    void fixupPerformanceTestStageWillReturnStage() {

        when(service.existsJmeterFile(any())).thenReturn(true);

        sut.stages.put("dev", "develop");

        assertEquals("\n" +
                "        stage('DEV (develop)') {\n" +
                "        \n" +
                "            environment {\n" +
                "                MAVEN_PROFILE = 'dev'\n" +
                "            }\n" +
                "        \n" +
                "            when {\n" +
                "                branch 'develop'\n" +
                "            }\n" +
                "        \n" +
                "            steps {\n" +
                "                sh 'mvn validate -P performance-$MAVEN_PROFILE $MAVEN_ARGS'\n" +
                "            }\n" +
                "        \n" +
                "            post {\n" +
                "                always {\n" +
                "                    script {\n" +
                "                        publishJMeterReport()\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n", sut.fixupPerformanceTestStage("@STAGES@"));
    }

    @Test
    void fixupPerformanceTestStageWillReturnMultipleStages() {

        when(service.existsJmeterFile(any())).thenReturn(true);

        sut.stages.put("dev", "develop,feature-*");

        assertEquals("\n" +
                "        stage('DEV (develop)') {\n" +
                "        \n" +
                "            environment {\n" +
                "                MAVEN_PROFILE = 'dev'\n" +
                "            }\n" +
                "        \n" +
                "            when {\n" +
                "                branch 'develop'\n" +
                "            }\n" +
                "        \n" +
                "            steps {\n" +
                "                sh 'mvn validate -P performance-$MAVEN_PROFILE $MAVEN_ARGS'\n" +
                "            }\n" +
                "        \n" +
                "            post {\n" +
                "                always {\n" +
                "                    script {\n" +
                "                        publishJMeterReport()\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        stage('DEV (feature-*)') {\n" +
                "        \n" +
                "            environment {\n" +
                "                MAVEN_PROFILE = 'dev'\n" +
                "            }\n" +
                "        \n" +
                "            when {\n" +
                "                branch 'feature-*'\n" +
                "            }\n" +
                "        \n" +
                "            steps {\n" +
                "                sh 'mvn validate -P performance-$MAVEN_PROFILE $MAVEN_ARGS'\n" +
                "            }\n" +
                "        \n" +
                "            post {\n" +
                "                always {\n" +
                "                    script {\n" +
                "                        publishJMeterReport()\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n", sut.fixupPerformanceTestStage("@STAGES@"));
    }

    @Test
    void fixupRegressionTestStageOnEmpty() {

        assertNull(sut.fixupRegressionTestStage(""));
    }

    @Test
    void fixupRegressionTestStageOnEmptyStages() {

        sut.downstreamProjects.add("e2e");

        assertEquals(StringUtils.EMPTY, sut.fixupRegressionTestStage(""));
    }

    @Test
    void fixupRegressionTestStage() {

        sut.downstreamProjects.add("e2e");

        sut.stages.put("dev", "develop");

        assertEquals("\n" +
                "        stage('DEV (e2e/develop)') {\n" +
                "        \n" +
                "            environment {\n" +
                "                JOB_NAME = 'e2e'\n" +
                "                STAGE_NAME = 'dev'\n" +
                "                VERSION = sh(script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout $MAVEN_ARGS', returnStdout: true).trim()\n" +
                "            }\n" +
                "        \n" +
                "            when {\n" +
                "                branch 'develop'\n" +
                "            }\n" +
                "        \n" +
                "            steps {\n" +
                "        \n" +
                "                script {\n" +
                "                    triggerJob \"../${env.JOB_NAME}/${env.BRANCH_NAME}\"\n" +
                "                }\n" +
                "            }\n" +
                "        }\n", sut.fixupRegressionTestStage("@STAGES@"));
    }


    @Test
    void fixupRegressionTestStageMultiple() {

        sut.downstreamProjects.add("e2e");
        sut.downstreamProjects.add("performance");

        sut.stages.put("dev", "develop");

        assertEquals("stage('Regression Tests (1)') {\n" +
                "\n" +
                "    parallel {\n" +
                "\n" +
                "        stage('DEV (e2e/develop)') {\n" +
                "        \n" +
                "            environment {\n" +
                "                JOB_NAME = 'e2e'\n" +
                "                STAGE_NAME = 'dev'\n" +
                "                VERSION = sh(script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout $MAVEN_ARGS', returnStdout: true).trim()\n" +
                "            }\n" +
                "        \n" +
                "            when {\n" +
                "                branch 'develop'\n" +
                "            }\n" +
                "        \n" +
                "            steps {\n" +
                "        \n" +
                "                script {\n" +
                "                    triggerJob \"../${env.JOB_NAME}/${env.BRANCH_NAME}\"\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "stage('Regression Tests (2)') {\n" +
                "\n" +
                "    parallel {\n" +
                "\n" +
                "        stage('DEV (performance/develop)') {\n" +
                "        \n" +
                "            environment {\n" +
                "                JOB_NAME = 'performance'\n" +
                "                STAGE_NAME = 'dev'\n" +
                "                VERSION = sh(script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout $MAVEN_ARGS', returnStdout: true).trim()\n" +
                "            }\n" +
                "        \n" +
                "            when {\n" +
                "                branch 'develop'\n" +
                "            }\n" +
                "        \n" +
                "            steps {\n" +
                "        \n" +
                "                script {\n" +
                "                    triggerJob \"../${env.JOB_NAME}/${env.BRANCH_NAME}\"\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "    }\n" +
                "}\n" +
                "\n", sut.getJenkinsStage("regression-test"));
    }
}
