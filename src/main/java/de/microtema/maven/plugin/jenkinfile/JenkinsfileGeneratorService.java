package de.microtema.maven.plugin.jenkinfile;

import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JenkinsfileGeneratorService {

    String getRootPath(MavenProject project) {

        return project.getBasedir().getPath();
    }

    boolean existsDockerfile(MavenProject project) {

        return new File(getRootPath(project) + "/Dockerfile").exists();
    }

    boolean existsDbMigrationScripts(MavenProject project) {

        return new File(getRootPath(project), "src/main/resources/db/migration").exists();
    }

    boolean hasSourceCode(MavenProject project) {

        if (new File(getRootPath(project), "src/main/test").exists()) {
            return true;
        }

        return project.getModules().stream().noneMatch(it -> ((String) it).startsWith("../"));
    }

    boolean isGitRepo(MavenProject project) {

        return new File(getRootPath(project), ".git").exists();
    }

    List<String> getSonarExcludes(MavenProject project) {

        List<String> excludes = new ArrayList<>(project.getModules());

        excludes.removeIf(it -> it.startsWith("../"));
        excludes.removeIf(it -> new File(new File(getRootPath(project), it), "src/main/java").exists());

        return excludes;
    }

    boolean hasSonarProperties(MavenProject project) {

        long count = 0;

        count = project.getProperties().entrySet()
                .stream()
                .filter(it -> String.valueOf(it.getKey()).startsWith("sonar.")).count();

        return count > 0;
    }
}
