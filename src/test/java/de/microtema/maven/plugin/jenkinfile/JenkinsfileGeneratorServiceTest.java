package de.microtema.maven.plugin.jenkinfile;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

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


    @AfterEach
    void tearDown() {

        dockerFile.delete();
        jenkinsfile.delete();
    }

    @Test
    public void existsDocker() {

        when(project.getBasedir()).thenReturn(new File("."));

        assertFalse(sut.existsDockerfile(project));
    }

    @Test
    public void existsDockerWillReturnTrue() {

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
    void isGitRepo() {

        when(project.getBasedir()).thenReturn(new File("."));

        assertTrue(sut.isGitRepo(project));
    }
}
