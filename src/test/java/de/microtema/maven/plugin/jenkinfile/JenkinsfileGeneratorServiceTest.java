package de.microtema.maven.plugin.jenkinfile;

import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JenkinsfileGeneratorServiceTest {

    @InjectMocks
    JenkinsfileGeneratorService sut;

    @Mock
    MavenProject project;

    File dockerFile = new File("./Dockerfile");

    File jenkinsfile = new File("./Jenkinsfile");

    File sourceFolder = new File("./src/main/test");
    File moduleFolder = new File("./module/src/main/test");

    @AfterEach
    void tearDown() {

        dockerFile.delete();
        jenkinsfile.delete();
        sourceFolder.delete();
        moduleFolder.delete();
    }

    @Test
    void existsDocker() {

        when(project.getBasedir()).thenReturn(new File("."));

        assertFalse(sut.existsDockerfile(project));
    }

    @Test
    void existsDockerWillReturnTrue() {

        when(project.getBasedir()).thenReturn(new File("."));

        dockerFile.mkdir();

        assertTrue(sut.existsDockerfile(project));
    }

    @Test
    void getRootPath() {

        when(project.getBasedir()).thenReturn(new File("."));

        assertEquals(".", sut.getRootPath(project));
    }

    @Test
    void existsDockerfile() {
        dockerFile.mkdir();

        when(project.getBasedir()).thenReturn(new File("."));

        assertTrue(sut.existsDockerfile(project));
    }

    @Test
    void existsDbMigrationScripts() {

        when(project.getBasedir()).thenReturn(new File("."));

        assertFalse(sut.existsDbMigrationScripts(project));
    }

    @Test
    void hasSourceCode() {

        when(project.getBasedir()).thenReturn(new File("."));

        assertTrue(sut.hasSourceCode(project));
    }

    @Test
    void hasSourceCodeOnSrcModules() {

        when(project.getModules()).thenReturn(Collections.singletonList("../foo"));

        when(project.getBasedir()).thenReturn(new File("."));

        assertFalse(sut.hasSourceCode(project));
    }

    @Test
    void hasSourceCodeOnSrcModulesWillReturnTrue() {

        when(project.getModules()).thenReturn(Collections.singletonList("foo"));

        when(project.getBasedir()).thenReturn(new File("."));

        assertTrue(sut.hasSourceCode(project));
    }

    @Test
    void hasSourceCodeOnSrcFolder() {

        sourceFolder.mkdir();

        when(project.getBasedir()).thenReturn(new File("."));

        assertTrue(sut.hasSourceCode(project));
    }

    @Test
    void isGitRepo() {

        when(project.getBasedir()).thenReturn(new File("."));

        assertTrue(sut.isGitRepo(project));
    }

    @Test
    void getSonarExcludes() {

        List<String> answer = sut.getSonarExcludes(project);

        assertTrue(answer.isEmpty());
    }

    @Test
    void getSonarExcludesWillReturnEmpty() {

        when(project.getModules()).thenReturn(Collections.singletonList("../"));

        List<String> answer = sut.getSonarExcludes(project);

        assertTrue(answer.isEmpty());
    }

    @Test
    void getSonarExcludesWillReturnOne() {

        moduleFolder.mkdirs();

        when(project.getBasedir()).thenReturn(new File("."));
        when(project.getModules()).thenReturn(Collections.singletonList("module"));

        List<String> answer = sut.getSonarExcludes(project);

        assertEquals(1, answer.size());
        assertEquals("module", answer.get(0));
    }

    @Test
    void hasSonarProperties() {

        when(project.getProperties()).thenReturn(new Properties());

        boolean answer = sut.hasSonarProperties(project);

        assertFalse(answer);
    }

    @Test
    void hasSonarPropertiesWillReturnTrue() {

        Properties properties = new Properties();
        properties.put("sonar.key", "123456789");

        when(project.getProperties()).thenReturn(properties);

        boolean answer = sut.hasSonarProperties(project);

        assertTrue(answer);
    }
}
