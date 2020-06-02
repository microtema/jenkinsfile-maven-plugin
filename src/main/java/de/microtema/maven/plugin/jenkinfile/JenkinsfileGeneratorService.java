package de.microtema.maven.plugin.jenkinfile;

import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JenkinsfileGeneratorService {

    static String MIGRATION_PATH = "src/main/resources/db/migration";
    static String JAVA_PATH = "src/main/java";

    String getRootPath(MavenProject project) {

        return project.getBasedir().getPath();
    }

    boolean existsDockerfile(MavenProject project) {

        return new File(getRootPath(project) + "/Dockerfile").exists();
    }

    boolean existsDbMigrationScripts(MavenProject project) {

        return new File(getRootPath(project), MIGRATION_PATH).exists();
    }

    boolean hasSourceCode(MavenProject project) {

        if (new File(getRootPath(project), JAVA_PATH).exists()) {
            return true;
        }

        if (project.getModules().isEmpty()) {
            return false;
        }

        return project.getModules().stream().noneMatch(it -> ((String) it).startsWith("../"));
    }

    boolean isGitRepo(MavenProject project) {

        return new File(getRootPath(project), ".git").exists();
    }

    List<String> getSonarExcludes(MavenProject project) {

        List<String> excludes = new ArrayList<>(project.getModules());

        excludes.removeIf(it -> it.startsWith("../"));
        excludes.removeIf(it -> new File(new File(getRootPath(project), it), JAVA_PATH).exists());

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
