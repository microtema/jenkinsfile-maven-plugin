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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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

    public void execute() throws MojoExecutionException, MojoFailureException {

        String artifactId = project != null ? project.getArtifactId() : null;
        MavenProject parent = project.getParent();
        if (parent != null) {
            project = parent;
        }

        String rootPath = getRootPath();

        getLog().info("+----------------------------------+");
        getLog().info("Generate Jenkinsfile: " + artifactId + " -> " + rootPath);
        getLog().info("+----------------------------------+");

        String agent = getJenkinsStage("agent");
        String environment = getJenkinsStage("environment");
        String options = getJenkinsStage("options");
        String triggers = buildTriggers();
        String stages = buildStages();
        String post = getJenkinsStage("post");
        String pipeline = generateClosure("pipeline", null, agent, environment, options, triggers, stages, post);

        try (PrintWriter out = new PrintWriter(rootPath + "/Jenkinsfile")) {
            out.println(pipeline);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    String getRootPath() {

        return project.getBasedir().getPath();
    }

    String buildStages() {

        String test = getTestStageName();

        List<String> stages = Arrays.asList("initialize", "versioning", "compile", test, "sonar",
                "maven-build", "security", "docker-build", "tag", "publish", "deployment", "aqua", "promote", "deployment-prod");

        String[] objects = new String[stages.size()];

        for (int index = 0; index < stages.size(); index++) {
            objects[index] = getJenkinsStage(stages.get(index));
        }

        return generateClosure("stages", null, objects);
    }

    String getTestStageName() {

        if (StringUtils.isNotEmpty(aquaITFolderId)) {
            return "test";
        }

        return "unit-test";
    }

    String buildTriggers() {

        if (upstreamProjects.length == 0) {
            return null;
        }

        StringBuilder upstreamProjectsParam = new StringBuilder();

        for (int index = 0; index < this.upstreamProjects.length; index++) {

            String project = this.upstreamProjects[index];

            upstreamProjectsParam.append(project);
            upstreamProjectsParam.append("/${env.BRANCH_NAME.replaceAll('/', '%2F')}");

            if (index < this.upstreamProjects.length - 1) {
                upstreamProjectsParam.append(",");
            }
        }

        String body = "upstream(\n" +
                "  upstreamProjects: \"" + upstreamProjectsParam + "\",\n" +
                "  threshold: hudson.model.Result.SUCCESS)\n";

        return generateClosure("triggers", "", body);
    }

    String generateClosure(String tag, String name, String... bodies) {

        StringBuilder builder = new StringBuilder();

        builder.append(tag);
        if (StringUtils.isNotEmpty(name)) {
            builder.append("('" + name + "')");
        }
        builder.append(" {").append("\n");

        for (String body : bodies) {

            if (StringUtils.isNotEmpty(body)) {
                builder.append(body);
                builder.append("\n");
            }
        }

        builder.append("}");

        return builder.toString();
    }

    String fixSonarStage(String template) {

        if (this.sonarExcludes.length == 0) {
            return template;
        }

        StringBuilder excludes = new StringBuilder(" -pl ");

        for (String exclude : sonarExcludes) {
            excludes.append("!").append(exclude);
        }

        return template.replaceFirst("sonar:sonar", "sonar:sonar" + excludes);
    }

    String fixupInitializeStage(String template) {

        return template.replaceFirst("@BOOTSTRAP_URL@", maskEnvironmentVariable(bootstrapUrl));
    }

    String fixupEnvironment(String template) {

        return template.replaceFirst("@BASE_NAMESPACE@", maskEnvironmentVariable(baseNamespace))
                .replaceFirst("@APP@", maskEnvironmentVariable(appName));
    }

    String fixupAquaStage(String template) {

        return template.replaceFirst("@AQUA_PROJECT_ID@", maskEnvironmentVariable(aquaProjectId))
                .replaceFirst("@AQUA_PRODUCT_ID@", maskEnvironmentVariable(aquaProductId))
                .replaceFirst("@AQUA_RELEASE@", maskEnvironmentVariable(aquaRelease))
                .replaceFirst("@AQUA_LEVEL@", maskEnvironmentVariable(aquaLevel))
                .replaceFirst("@INTEGRATION_TEST_AQUA_FOLDER_ID@", maskEnvironmentVariable(aquaITFolderId))
                .replaceFirst("@JUNIT_TEST_AQUA_FOLDER_ID@", maskEnvironmentVariable(aquaJunitFolderId));
    }

    String fixupDeploymentProd(String template) {

        if (prodStages.length == 0 || !existsDocker()) {
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

    boolean existsDocker() {

        return new File(getRootPath() + "/Dockerfile").exists();
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
            case "initialize":
                return fixupInitializeStage(template);

            case "docker-build":
            case "deployment":
                return getStageOrNull(template, existsDocker());

            case "publish":
            case "tag":
                return getStageOrNull(template, !existsDocker());

            case "aqua":
                return fixupAquaStage(template);
            case "promote":
                return getStageOrNull(template, prodStages.length > 0 && existsDocker());
            case "deployment-prod":
                return fixupDeploymentProd(template);
            default:
                return template;
        }
    }
}
