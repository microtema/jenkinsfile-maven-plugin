package de.microtema.maven.plugin.jenkinfile;


import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        when(project.getProperties()).thenReturn(new Properties());

        String answer = sut.fixSonarStage("");

        assertNull(null, answer);
    }

    @Test
    void fixSonarStage() {

        when(service.hasSourceCode(project)).thenReturn(true);
        Properties properties = new Properties();
        properties.put("sonar.key", "123456");
        when(project.getProperties()).thenReturn(properties);

        String answer = sut.fixSonarStage("mvn sonar:sonar -Dsonar.branch.name=$BRANCH_NAME $MAVEN_ARGS");

        assertEquals("mvn sonar:sonar -Dsonar.branch.name=$BRANCH_NAME $MAVEN_ARGS", answer);
    }

    @Test
    void getSonarExcludes() {

        List<String> answer = sut.getSonarExcludes();

        assertTrue(answer.isEmpty());
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
    void fixupDbMigrationStage() {

        when(service.existsDbMigrationScripts(project)).thenReturn(true);

        assertEquals("template", sut.fixupDbMigrationStage("template"));
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
}
