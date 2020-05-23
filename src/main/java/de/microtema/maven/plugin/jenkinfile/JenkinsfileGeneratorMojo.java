package de.microtema.maven.plugin.jenkinfile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Parameter(property = "aqua-url")
    String aquaUrl;

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

    @Parameter(property = "environments")
    LinkedHashMap<String, String> environments = new LinkedHashMap<>();

    @Parameter(property = "stages")
    LinkedHashMap<String, String> stages = new LinkedHashMap<>();

    @Parameter(property = "update")
    boolean update;

    public void execute() {

        String rootPath = getRootPath();

        // Skip maven sub modules
        if (!new File(rootPath + "/.git").exists()) {

            getLog().info("+----------------------------------+");
            getLog().info("Skip maven module: " + appName + " since it is not a git repo!");

            return;
        }

        if (!update) {

            getLog().info("+----------------------------------+");
            getLog().info("Jenkinsfile already exists and will be not updated: " + appName);
            getLog().info("+----------------------------------+");

            return;
        }

        getLog().info("+----------------------------------+");
        getLog().info("Generate Jenkinsfile for " + appName + " -> " + rootPath);
        getLog().info("+----------------------------------+");

        initDefaults();

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

    private void initDefaults() {
        if (stages.isEmpty()) {
            stages.put("etu", "develop");
            stages.put("itu", "release-*");
            stages.put("satu", "master");
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

        List<String> stages = Arrays.asList("initialize", "versioning", "compile", "db-migration", test, "maven-build",
                "sonar", "security", "docker-build", "tag", "publish", "deployment", "readiness", "aqua",
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

        if (project.getPackaging().equals("pom")) {
            return null;
        }

        long count = 0;

        if (project != null) {
            count = project.getProperties().entrySet()
                    .stream()
                    .filter(it -> String.valueOf(it.getKey()).startsWith("sonar.")).count();
        }

        if (count == 0) {

            getLog().info("Skip Stage(Sonar) in Jenkinsfile, since there is no sonar properties configured: " + appName);

            return null;
        }

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

        StringBuilder environmentsAsString = new StringBuilder();

        if (existsDockerfile()) {
            environments.putIfAbsent("APP", appName);
            environments.putIfAbsent("BASE_NAMESPACE", baseNamespace);
            environments.putIfAbsent("DEPLOYABLE", "sh(script: 'oc whoami', returnStdout: true).trim().startsWith(\"system:serviceaccount:${env.BASE_NAMESPACE}\")");
        }

        for (Map.Entry<String, String> entry : environments.entrySet()) {
            String value = entry.getValue();

            if (!(value.startsWith("sh") || value.startsWith("'") || value.startsWith("\""))) {
                value = maskEnvironmentVariable(value);
            }

            String line = entry.getKey() + " = " + value;
            environmentsAsString.append(paddLine(line, 8));
        }

        return template.replace("@ENVIRONMENTS@", environmentsAsString.toString());
    }

    String fixupInitializeStage(String template) {

        String bootstrap = StringUtils.EMPTY;

        if (StringUtils.isNotEmpty(bootstrapUrl)) {
            bootstrap = getJenkinsStage("initialize-bootstrap");
        }

        return template.replace("@BOOTSTRAP_URL@", maskEnvironmentVariable(bootstrapUrl)).replace("@BOOTSTRAP@", bootstrap);
    }

    String fixupReadinessStage(String template) {

        if (!existsDockerfile()) {
            return null;
        }

        StringBuilder body = new StringBuilder();

        for (Map.Entry<String, String> stage : stages.entrySet()) {
            body.append("\n");
            String scriptTemplate = getJenkinsStage("readiness-steps");
            String stageTemplate = getJenkinsStage("readiness-stage")
                    .replace("@STAGE_NAME@", maskEnvironmentVariable(stage.getKey().toUpperCase()))
                    .replace("@BRANCH_PATTERN@", maskEnvironmentVariable(stage.getValue()))
                    .replace("@STEPS@", scriptTemplate);
            body.append(paddLine(stageTemplate, 6));
        }

        return template.replace("@STAGES@", body.toString());
    }

    String fixupDbMigrationStage(String template) {

        if (!existsDockerfile() && !existsDbMigrationScripts()) {
            return null;
        }

        StringBuilder body = new StringBuilder();

        for (Map.Entry<String, String> stage : stages.entrySet()) {
            body.append("\n");
            String scriptTemplate = getJenkinsStage("db-migration-steps")
                    .replace("@STAGE_NAME@", stage.getKey().toLowerCase());
            String stageTemplate = getJenkinsStage("db-migration-stage")
                    .replace("@STAGE_NAME@", maskEnvironmentVariable(stage.getKey().toUpperCase()))
                    .replace("@BRANCH_PATTERN@", maskEnvironmentVariable(stage.getValue()))
                    .replace("@STEPS@", scriptTemplate);
            body.append(paddLine(stageTemplate, 6));
        }

        return template.replace("@STAGES@", body.toString());
    }

    String fixupAquaStage(String template) {

        if (project.getPackaging().equals("pom")) {
            return null;
        }

        return template.replaceFirst("@AQUA_PROJECT_ID@", maskEnvironmentVariable(aquaProjectId))
                .replaceFirst("@AQUA_URL@", maskEnvironmentVariable(aquaUrl))
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

    boolean existsDbMigrationScripts() {

        return new File(getRootPath() + "/src/main/resources/db/migration").exists();
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
            case "initialize":
                return fixupInitializeStage(template);
            case "sonar":
                return fixSonarStage(template);
            case "test":
            case "unit-test":
                return getStageOrNull(template, StringUtils.isNotEmpty(aquaJunitFolderId) || StringUtils.isNotEmpty(aquaITFolderId));

            case "docker-build":
            case "deployment":
                return getStageOrNull(template, existsDockerfile());

            case "publish":
            case "tag":
                return getStageOrNull(template, !existsDockerfile());

            case "db-migration":
                return fixupDbMigrationStage(template);
            case "readiness":
                return fixupReadinessStage(template);
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
