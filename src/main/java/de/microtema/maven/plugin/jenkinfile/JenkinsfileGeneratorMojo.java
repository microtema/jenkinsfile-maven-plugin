package de.microtema.maven.plugin.jenkinfile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.COMPILE)
public class JenkinsfileGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "upstream-projects")
    String[] upstreamProjects = new String[0];

    @Parameter(property = "prod-stages")
    String[] prodStages = new String[0];

    @Parameter(property = "sonar-excludes")
    String[] sonarExcludes = new String[0];

    @Parameter(property = "base-namespace")
    String baseNamespace;

    @Parameter(property = "aqua-project-id")
    String aquaProjectId;

    @Parameter(property = "aqua-product-id")
    String aquaProductId;

    @Parameter(property = "aqua-release")
    String aquaRelease;

    @Parameter(property = "aqua-level")
    String aquaLevel;

    @Parameter(property = "aqua-junit-folder-id")
    String aquaJunitFolderId;

    @Parameter(property = "aqua-it-folder-id")
    String aquaITFolderId;

    @Parameter(property = "bootstrap-url")
    String bootstrapUrl;

    @Parameter(property = "app")
    String appName;

    @Parameter(property = "update")
    boolean update;

    public void execute() throws MojoExecutionException, MojoFailureException {

        MavenProject parent = project != null ? project.getParent() : null;
        if (parent != null) {
            project = parent;
        }

        String artifactId = project != null ? project.getArtifactId() : null;
        String rootPath = getRootPath();

        if (!update) {

            getLog().info("+----------------------------------+");
            getLog().info("Jenkinsfile already exists and will be not updated: " + artifactId);
            getLog().info("+----------------------------------+");
            return;
        }

        getLog().info("+----------------------------------+");
        getLog().info("Generate Jenkinsfile: " + artifactId + " -> " + rootPath);
        getLog().info("+----------------------------------+");

        String agent = getJenkinsStage("agent");
        String environment = getJenkinsStage("environment");
        String options = getJenkinsStage("options");
        String triggers = buildTriggers();
        String stages = buildStages();
        String pipeline = getJenkinsStage("pipeline");

        pipeline = pipeline.replace("@AGENT@", agent)
                .replace("@ENVIRONMENT@", environment)
                .replace("@OPTIONS@", options)
                .replace("@TRIGGERS@", triggers)
                .replace("@STAGES@", stages);

        try (PrintWriter out = new PrintWriter(rootPath + "/Jenkinsfile")) {
            out.println(pipeline);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    String getRootPath() {

        if (project == null) {
            return ".";
        }

        return project.getBasedir().getPath();
    }

    String buildStages() {

        String test = getTestStageName();

        List<String> stages = Arrays.asList("initialize", "versioning", "compile", test, "sonar",
                "maven-build", "security", "docker-build", "tag", "publish", "deployment", "aqua",
                "promote", "deployment-prod");

        StringBuilder body = new StringBuilder();

        for (String stage : stages) {

            String stageTemplate = getJenkinsStage(stage);

            if (StringUtils.isNotEmpty(stageTemplate)) {
                body.append("\n");
                body.append(paddLine(stageTemplate, 6));
            }
        }

        return body.toString();
    }

    String paddLine(String template, int padding) {

        BufferedReader reader = new BufferedReader(new StringReader(template));
        StringBuilder builder = new StringBuilder();

        String paddingString = "";
        while (padding-- > 0) {
            paddingString = " " + paddingString;
        }

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(paddingString).append(line).append("\n");
            }

        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // ignore this exception
            }
        }

        return builder.toString();
    }

    String getTestStageName() {

        if (StringUtils.isNotEmpty(aquaITFolderId)) {
            return "test";
        }

        return "unit-test";
    }

    String buildTriggers() {

        String template = getJenkinsStage("triggers");

        StringBuilder upstreamProjectsParam = new StringBuilder();

        for (int index = 0; index < upstreamProjects.length; index++) {

            String project = upstreamProjects[index];

            upstreamProjectsParam.append(project);
            upstreamProjectsParam.append("/${env.BRANCH_NAME.replaceAll('/', '%2F')}");

            if (index < upstreamProjects.length - 1) {
                upstreamProjectsParam.append(",");
            }
        }

        return template.replace("@UPSTREAM_PROJECTS@", upstreamProjectsParam.toString());
    }

    String fixSonarStage(String template) {

        if (sonarExcludes.length == 0) {
            return template;
        }

        StringBuilder excludes = new StringBuilder(" -pl ");

        for (String exclude : sonarExcludes) {
            excludes.append("!").append(exclude);
        }

        return template.replaceFirst("sonar:sonar", "sonar:sonar" + excludes);
    }

    String fixupEnvironment(String template) {

        return template.replaceFirst("@BASE_NAMESPACE@", maskEnvironmentVariable(baseNamespace))
                .replaceFirst("@APP@", maskEnvironmentVariable(appName))
                .replaceFirst("@BOOTSTRAP_URL@", maskEnvironmentVariable(bootstrapUrl));
    }

    String fixupAquaStage(String template) {

        return template.replaceFirst("@AQUA_PROJECT_ID@", maskEnvironmentVariable(aquaProjectId))
                .replaceFirst("@AQUA_PRODUCT_ID@", maskEnvironmentVariable(aquaProductId))
                .replaceFirst("@AQUA_RELEASE@", maskEnvironmentVariable(aquaRelease))
                .replaceFirst("@AQUA_LEVEL@", maskEnvironmentVariable(aquaLevel))
                .replaceFirst("@AQUA_JUNIT_TEST_FOLDER_ID@", maskEnvironmentVariable(aquaJunitFolderId))
                .replaceFirst("@AQUA_INTEGRATION_TEST_FOLDER_ID@", maskEnvironmentVariable(aquaITFolderId));

    }

    String fixupDeploymentProd(String template) {

        if (prodStages.length == 0 || !existsDockerfile()) {
            return null;
        }

        StringBuilder stages = new StringBuilder();

        String stageTemplate = getJenkinsStage("deployment-prod-stage");

        for (String stage : prodStages) {
            String stageName = maskEnvironmentVariable(getStageName(stage));
            String namespace = maskEnvironmentVariable(stage);
            stages.append(stageTemplate.replaceFirst("@STAGE_NAME@", stageName).replaceFirst("@STAGE@", namespace));
        }

        return template.replaceFirst("@STAGES@", stages.toString());
    }

    private String getStageName(String stage) {

        String[] tokens = stage.split("-");

        if (tokens.length == 1) {
            return stage.toUpperCase();
        }

        return tokens[1].toUpperCase();
    }

    String getStageOrNull(String template, boolean render) {

        if (render) {

            return template;
        }

        return null;
    }

    boolean existsDockerfile() {

        return new File(getRootPath() + "/Dockerfile").exists();
    }

    boolean existsJenkinsfile() {

        return new File(getRootPath() + "/Jenkinsfile").exists();
    }

    String maskEnvironmentVariable(String value) {

        return "'" + (value != null ? value : "") + "'";
    }

    String getJenkinsStage(String templateName) {

        InputStream inputStream = getClass().getResourceAsStream("/" + templateName + ".Jenkinsfile");

        String template;

        try {
            template = IOUtils.toString(inputStream, Charset.defaultCharset());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        switch (templateName) {
            case "environment":
                return fixupEnvironment(template);
            case "sonar":
                return fixSonarStage(template);

            case "docker-build":
            case "deployment":
                return getStageOrNull(template, existsDockerfile());

            case "publish":
            case "tag":
                return getStageOrNull(template, !existsDockerfile());

            case "aqua":
                return fixupAquaStage(template);
            case "promote":
                return getStageOrNull(template, prodStages.length > 0 && existsDockerfile());
            case "deployment-prod":
                return fixupDeploymentProd(template);
            default:
                return template;
        }
    }
}
