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
    public void setUp() {

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
    public void executeOnNonGitRepo() {

        when(service.isGitRepo(project)).thenReturn(false);

        sut.execute();

        assertFalse(jenkinsfile.exists());
    }

    @Test
    public void executeOnNonUpdateFalse() {

        when(service.isGitRepo(project)).thenReturn(true);

        sut.update = false;

        sut.execute();

        assertFalse(jenkinsfile.exists());
    }

    @Test
    public void execute() {

        when(service.getRootPath(project)).thenReturn(".");
        when(service.isGitRepo(project)).thenReturn(true);

        sut.update = true;
        sut.prodStages = new String[]{"foo", "nf-bar"};

        sut.execute();

        assertTrue(jenkinsfile.exists());
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
    public void fixupAquaStage() {

        when(service.hasSourceCode(project)).thenReturn(true);

        sut.aquaProjectId = "1000";

        String stage = sut.fixupAquaStage("AQUA_PROJECT_ID = @AQUA_PROJECT_ID@");

        assertEquals("AQUA_PROJECT_ID = '1000'", stage);
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
    public void maskEnvironmentVariable() {

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
    void buildStages() {
    }

    @Test
    void paddLine() {

        assertEquals("    stage('name') {}\n", sut.paddLine("stage('name') {}", 4));
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

        assertEquals("triggers {        upstream(upstreamProjects: \"\", threshold: hudson.model.Result.SUCCESS)    }", sut.buildTriggers().replaceAll(System.lineSeparator(), ""));
    }

    @Test
    void buildTriggers() {

        sut.upstreamProjects = new String[]{"foo"};

        assertEquals("triggers {        upstream(upstreamProjects: \"foo/${env.BRANCH_NAME.replaceAll('/', '%2F')}\", threshold: hudson.model.Result.SUCCESS)    }", sut.buildTriggers().replaceAll(System.lineSeparator(), ""));
    }

    @Test
    void buildTriggersOnMultiplesUpstreams() {

        sut.upstreamProjects = new String[]{"foo", "bar"};

        assertEquals("triggers {        upstream(upstreamProjects: \"foo/${env.BRANCH_NAME.replaceAll('/', '%2F')},bar/${env.BRANCH_NAME.replaceAll('/', '%2F')}\", threshold: hudson.model.Result.SUCCESS)    }", sut.buildTriggers().replaceAll(System.lineSeparator(), ""));
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

        String answer = sut.getJenkinsStage("environment").replaceAll(System.lineSeparator(), "");

        assertEquals("environment {        CURRENT_TIME = sh(script: 'date +%Y-%m-%d-%H-%M', returnStdout: true).trim()        CHANGE_AUTHOR_EMAIL = sh(script: \"git --no-pager show -s --format='%ae'\", returnStdout: true).trim()    }", answer.replaceAll("\n", ""));
    }

    @Test
    void fixupEnvironmentWithEnvs() {

        when(service.existsDockerfile(project)).thenReturn(true);

        String answer = sut.getJenkinsStage("environment").replaceAll(System.lineSeparator(), "");

        assertEquals("environment {        CURRENT_TIME = sh(script: 'date +%Y-%m-%d-%H-%M', returnStdout: true).trim()        CHANGE_AUTHOR_EMAIL = sh(script: \"git --no-pager show -s --format='%ae'\", returnStdout: true).trim()        APP = 'app'        BASE_NAMESPACE = 'ns'        DEPLOYABLE = sh(script: 'oc whoami', returnStdout: true).trim().startsWith(\"system:serviceaccount:${env.BASE_NAMESPACE}\")    }", answer.replaceAll("\n", ""));
    }

    @Test
    void fixupInitializeStage() {

        String answer = sut.getJenkinsStage("initialize").replaceAll(System.lineSeparator(), "");

        assertEquals("    stage('Initialize') {        environment {            BOOTSTRAP_URL = ''        }        steps {                        sh 'whoami'            sh 'oc whoami'            sh 'mvn -version'            sh 'echo commit-id: $GIT_COMMIT'            sh 'echo change author: $CHANGE_AUTHOR_EMAIL'        }    }", answer.replaceAll("\n", ""));

    }

    @Test
    void fixupInitializeStageWillBootstrapUrl() {

        sut.bootstrapUrl = "localhost";

        String answer = sut.getJenkinsStage("initialize").replaceAll(System.lineSeparator(), "");

        assertEquals("    stage('Initialize') {        environment {            BOOTSTRAP_URL = 'localhost'        }        steps {            script {                if (env.BOOTSTRAP_URL.toLowerCase() == env.GIT_URL.toLowerCase()) {                    env.MAVEN_ARGS = '-s ./settings.xml'                } else {                    dir('bootstrap') {                        try {                            git branch: env.BRANCH_NAME, url: env.BOOTSTRAP_URL, credentialsId: 'SCM_CREDENTIALS'                        } catch (e) {                            git branch: 'develop', url: env.BOOTSTRAP_URL, credentialsId: 'SCM_CREDENTIALS'                        }                        env.MAVEN_ARGS = '-s ./bootstrap/settings.xml'                    }                }           }            sh 'whoami'            sh 'oc whoami'            sh 'mvn -version'            sh 'echo commit-id: $GIT_COMMIT'            sh 'echo change author: $CHANGE_AUTHOR_EMAIL'        }    }", answer);
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
                "              stage('FOO') {\n" +
                "      \n" +
                "                  when {\n" +
                "                      branch 'develop'\n" +
                "                  }\n" +
                "      \n" +
                "                  steps {\n" +
                "                      script {\n" +
                "      \n" +
                "                          def namespace = \"${env.BASE_NAMESPACE}-etu\"\n" +
                "      \n" +
                "                          def waitForPodReadinessImpl = {\n" +
                "      \n" +
                "                              def pods = sh(script: \"oc get pods --namespace ${namespace} | grep -E '${env.APP}.*' | grep -v build | grep -v deploy\", returnStdout: true)\n" +
                "                              .trim().split('\\n')\n" +
                "                              .collect { it.split(' ')[0] }\n" +
                "      \n" +
                "                              echo \"${pods}\"\n" +
                "      \n" +
                "                              pods.find {\n" +
                "                                  try {\n" +
                "                                      sh(script: \"oc describe pod ${it} --namespace ${namespace} | grep -c 'git-commit=${env.GIT_COMMIT}'\", returnStdout: true).trim().toInteger()\n" +
                "                                  } catch (e) {\n" +
                "                                      false\n" +
                "                                  }\n" +
                "                              }\n" +
                "                          }\n" +
                "      \n" +
                "                          while (!waitForPodReadinessImpl.call()) {\n" +
                "                              echo 'Pod is not available or not ready! Retry after few seconds...'\n" +
                "                              sleep(time: 30, unit: \"SECONDS\")\n" +
                "                          }\n" +
                "      \n" +
                "                          echo 'Pod is ready and updated'\n" +
                "                      }\n" +
                "                  }\n" +
                "      \n" +
                "              }\n", sut.fixupReadinessStage("@STAGES@"));
    }

    @Test
    void fixupDbMigrationStage() {

        when(service.existsDbMigrationScripts(project)).thenReturn(true);

        assertEquals("template", sut.fixupDbMigrationStage("template"));
    }

    @Test
    void fixupDbMigrationStageWithStages() {

        sut.stages.put("foo", "develop");

        when(service.existsDbMigrationScripts(project)).thenReturn(true);

        assertEquals("\n" +
                "              stage('FOO') {\n" +
                "      \n" +
                "                  when {\n" +
                "                      branch 'develop'\n" +
                "                  }\n" +
                "      \n" +
                "                  steps {\n" +
                "                      sh 'mvn flyway:migrate -P foo -Doracle.jdbc.fanEnabled=false $MAVEN_ARGS'\n" +
                "                  }\n" +
                "      \n" +
                "              }\n", sut.fixupDbMigrationStage("@STAGES@"));
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

        sut.prodStages = new String[]{"aws"};

        String answer = sut.fixupDeploymentProd("@STAGES@").replaceAll(System.lineSeparator(), "");

        assertEquals("            stage('AWS') {                steps {                    createOpsRepoMergeRequest opsRepositoryName: 'aws'                }            }", answer);
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

        String answer = sut.getJenkinsStage("agent").replaceAll(System.lineSeparator(), "");

        assertEquals("agent {        label 'mvn8'    }", answer);
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
