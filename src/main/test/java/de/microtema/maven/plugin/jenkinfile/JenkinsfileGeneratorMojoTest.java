package de.microtema.maven.plugin.jenkinfile;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JenkinsfileGeneratorMojoTest {

    JenkinsfileGeneratorMojo sut;

    File dockerFile = new File("./Dockerfile");
    File jenkinsfile = new File("./Jenkinsfile");

    @Before
    public void setUp() {

        sut = new JenkinsfileGeneratorMojo();

        dockerFile.delete();
        //jenkinsfile.delete();
    }

    @Test
    public void execute() throws Exception {

        dockerFile.mkdir();

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
    public void existsDocker() {

        assertFalse(sut.existsDockerfile());
    }

    @Test
    public void existsDockerWillReturnTrue() {

        dockerFile.mkdir();

        assertTrue(sut.existsDockerfile());
    }

    @Test
    public void maskEnvironmentVariable() {

        assertEquals("'foo'", sut.maskEnvironmentVariable("foo"));
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
