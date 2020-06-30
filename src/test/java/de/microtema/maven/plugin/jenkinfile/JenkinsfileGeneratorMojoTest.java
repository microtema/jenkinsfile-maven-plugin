package de.microtema.maven.plugin.jenkinfile;


import edu.emory.mathcs.backport.java.util.Collections;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
                "            when {\n" +
                "                branch 'develop'\n" +
                "            }\n" +
                "        \n" +
                "            steps {\n" +
                "        \n" +
                "                script {\n" +
                "        \n" +
                "                    def waitForPodReadinessImpl = {\n" +
                "        \n" +
                "                        def pods = sh(script: 'oc get pods --namespace $NAMESPACE | grep -v build | grep -v deploy | awk \\'/$APP/ {print $1}\\'', returnStdout: true).trim().split('\\n')\n" +
                "        \n" +
                "                        pods.find {\n" +
                "        \n" +
                "                            env.POD_NAME = it\n" +
                "        \n" +
                "                            try {\n" +
                "                                sh(script: 'oc describe pod $POD_NAME --namespace $NAMESPACE | grep -c \\'git-commit=$GIT_COMMIT\\'', returnStdout: true).trim().toInteger()\n" +
                "                            } catch (e) {\n" +
                "                                false\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "        \n" +
                "                    while (!waitForPodReadinessImpl.call()) {\n" +
                "                        echo 'Pod is not available or not ready! Retry after few seconds...'\n" +
                "                        sleep time: 30, unit: 'SECONDS'\n" +
                "                    }\n" +
                "        \n" +
                "                    echo \"Pod ${env.POD_NAME} is ready and updated\"\n" +
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
                "            }\n" +
                "            when {\n" +
                "                branch 'develop'\n" +
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
                "            }\n" +
                "            when {\n" +
                "                branch 'release-*'\n" +
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
                "            }\n" +
                "            when {\n" +
                "                branch 'hotfix-*'\n" +
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
    void testFixupAquaStage() {

        when(service.hasSourceCode(project)).thenReturn(true);

        assertEquals("template", sut.fixupAquaStage("template"));
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
                "    label 'mvn8'\n" +
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
}
