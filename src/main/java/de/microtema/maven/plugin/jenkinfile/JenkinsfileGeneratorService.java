package de.microtema.maven.plugin.jenkinfile;

import org.apache.maven.project.MavenProject;

import java.io.File;

public class JenkinsfileGeneratorService {

    String getRootPath(MavenProject project) {

        return project.getBasedir().getPath();
    }

    boolean existsDockerfile(MavenProject project) {

        return new File(getRootPath(project) + "/Dockerfile").exists();
    }

    boolean existsDbMigrationScripts(MavenProject project) {

        return new File(getRootPath(project) + "/src/main/resources/db/migration").exists();
    }

    boolean hasSourceCode(MavenProject project) {

        if (new File(getRootPath(project) + "/src/main/test").exists()) {
            return true;
        }

        return project.getModules().stream().noneMatch(it -> ((String) it).startsWith("../"));
    }

    boolean isGitRepo(MavenProject project) {

        return new File(getRootPath(project), "/.git").exists();
    }
}
