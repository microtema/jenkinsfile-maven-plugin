package de.microtema.maven.plugin.jenkinfile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.COMPILE)
public class JenkinsfileGeneratorMojo extends AbstractMojo {

    static final String STAGES_TAG = "@STAGES@";
    static final String STAGE_NAME = "@STAGE_NAME@";
    static final String ENV_STAGE_NAME = "@ENV_STAGE_NAME@";
    static final String MAVEN_PROFILE = "@MAVEN_PROFILE@";
    static final String BRANCH_PATTERN = "@BRANCH_PATTERN@";
    static final String AGENT = "@AGENT@";
    static final String ENVIRONMENT = "@ENVIRONMENT@";
    static final String OPTIONS = "@OPTIONS@";
    static final String TRIGGERS = "@TRIGGERS@";
    static final String BOOTSTRAP_URL = "@BOOTSTRAP_URL@";
    static final String STEPS = "@STEPS@";
    static final String STAGE = "@STAGE@";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "upstream-projects")
    List<String> upstreamProjects = new ArrayList<>();

    @Parameter(property = "prod-stages")
    List<String> prodStages = new ArrayList<>();

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

    JenkinsfileGeneratorService service = new JenkinsfileGeneratorService();

    public void execute() {

        // Skip maven sub modules
        if (!service.isGitRepo(project)) {

            logMessage("Skip maven module: " + appName + " since it is not a git repo!");

            return;
        }

        if (!update) {

            logMessage("Jenkinsfile already exists and will be not updated: " + appName);

            return;
        }

        String rootPath = service.getRootPath(project);

        logMessage("Generate Jenkinsfile for " + appName + " -> " + rootPath);

        initDefaults();

        String agent = getJenkinsStage("agent");
        String environment = getJenkinsStage("environment");
        String options = getJenkinsStage("options");
        String triggers = buildTriggers();
        String stagesTemplate = buildStages();
        String pipeline = getJenkinsStage("pipeline");

        pipeline = pipeline.replace(AGENT, paddLine(agent, 4))
                .replace(ENVIRONMENT, paddLine(environment, 4))
                .replace(OPTIONS, paddLine(options, 4))
                .replace(TRIGGERS, paddLine(triggers, 4))
                .replace(STAGES_TAG, paddLine(stagesTemplate, 4))
                .replaceFirst("\\n$", "");

        try (PrintWriter out = new PrintWriter(rootPath + "/Jenkinsfile")) {
            out.println(pipeline);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    void initDefaults() {

        /*
         * NOTE: keys should match DB instances
         */
        if (stages.isEmpty()) {
            stages.put("etu", "develop,feature-*");
            stages.put("itu", "release-*,hotfix-*");
            stages.put("satu", "master");
        }

        // Prevent recursive upstreams
        upstreamProjects.removeIf(it -> StringUtils.equalsIgnoreCase(it, project.getArtifactId()));
    }

    String buildStages() {

        String test = getTestStageName();

        List<String> stageNames = Arrays.asList("initialize", "versioning", "compile", "db-migration", test, "maven-build",
                "sonar", "security", "docker-build", "tag", "publish", "deployment", "readiness", "aqua",
                "promote", "deployment-prod");

        StringBuilder body = new StringBuilder();

        for (String stageName : stageNames) {

            String stageTemplate = getJenkinsStage(stageName);

            if (StringUtils.isNotEmpty(stageTemplate)) {
                body.append("\n");
                body.append(paddLine(stageTemplate, 4));
                body.append("\n");
            }
        }

        return body.toString();
    }

    String paddLine(String template, int padding) {

        BufferedReader reader = new BufferedReader(new StringReader(template));
        StringBuilder builder = new StringBuilder();

        List<String> spaces = new ArrayList<>();
        while (padding-- > 0) {
            spaces.add(" ");
        }

        String paddingString = String.join("", spaces);

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

        return builder.toString().replaceFirst("\\n$", "");
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

        for (int index = 0; index < upstreamProjects.size(); index++) {

            String upstreamProject = upstreamProjects.get(index);

            upstreamProjectsParam.append(upstreamProject);
            upstreamProjectsParam.append("/${env.BRANCH_NAME.replaceAll('/', '%2F')}");

            if (index < upstreamProjects.size() - 1) {
                upstreamProjectsParam.append(",");
            }
        }

        return template.replace("@UPSTREAM_PROJECTS@", upstreamProjectsParam.toString());
    }


    String fixSonarStage(String template) {

        if (!service.hasSourceCode(project)) {

            return null;
        }

        if (!service.hasSonarProperties(project)) {

            getLog().info("Skip Stage(Sonar) in Jenkinsfile, since there is no sonar properties configured: " + appName);

            return null;
        }

        List<String> sonarExcludes = service.getSonarExcludes(project);

        if (sonarExcludes.isEmpty()) {
            return template;
        }

        StringBuilder excludes = new StringBuilder(" -pl ");

        for (int index = 0; index < sonarExcludes.size(); index++) {

            String exclude = sonarExcludes.get(index);

            excludes.append("!").append(exclude);

            if (index < sonarExcludes.size() - 1) {
                excludes.append(" ");
            }
        }

        return template.replaceFirst("sonar:sonar", "sonar:sonar" + excludes);
    }

    String fixupEnvironment(String template) {

        StringBuilder environmentsAsString = new StringBuilder();

        if (service.existsDockerfile(project)) {
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
            environmentsAsString.append(paddLine(line, 4)).append("\n");
        }

        return template.replace("@ENVIRONMENTS@", environmentsAsString.toString().replaceFirst("\\n$", ""));
    }

    String fixupInitializeStage(String template) {

        String bootstrap = StringUtils.EMPTY;

        if (StringUtils.isNotEmpty(bootstrapUrl)) {
            bootstrap = paddLine(getJenkinsStage("initialize-bootstrap"), 8);
        }

        return template.replace(BOOTSTRAP_URL, maskEnvironmentVariable(bootstrapUrl)).replace("@BOOTSTRAP@", bootstrap);
    }

    String fixupReadinessStage(String template) {

        if (!service.existsDockerfile(project)) {
            return null;
        }

        StringBuilder body = new StringBuilder();

        for (Map.Entry<String, String> stage : stages.entrySet()) {

            String stageName = stage.getKey();
            String branchPatter = getBranches(stage.getValue()).get(0);

            body.append("\n");

            String stageTemplate = getJenkinsStage("readiness-stage")
                    .replaceAll(STAGE_NAME, maskEnvironmentVariable(stageName.toUpperCase()))
                    .replaceAll(ENV_STAGE_NAME, maskEnvironmentVariable(stageName.toLowerCase()))
                    .replace(BRANCH_PATTERN, maskEnvironmentVariable(branchPatter));
            body.append(paddLine(stageTemplate, 8));

            body.append("\n");

        }

        return template.replace(STAGES_TAG, body.toString());
    }

    String fixupDeploymentStage(String template) {

        if (!service.existsDockerfile(project)) {

            return null;
        }

        StringBuilder body = new StringBuilder();

        for (Map.Entry<String, String> stage : stages.entrySet()) {

            String stageName = stage.getKey();

            for (String branchPatter : getBranches(stage.getValue())) {

                body.append("\n");

                String stageTemplate = getJenkinsStage("deployment-stage")
                        .replaceAll(STAGE_NAME, getStageDisplayName(stageName, branchPatter))
                        .replaceAll(ENV_STAGE_NAME, maskEnvironmentVariable(stageName.toLowerCase()))
                        .replace(BRANCH_PATTERN, maskEnvironmentVariable(branchPatter));
                body.append(paddLine(stageTemplate, 8));

                body.append("\n");
            }
        }

        return template.replace(STAGES_TAG, body.toString());
    }

    String getStageDisplayName(String stageName, String branchPatter) {

        return maskEnvironmentVariable(stageName.toUpperCase() + " (" + branchPatter + ")");
    }

    String fixupDbMigrationStage(String template) {

        if (!service.existsDbMigrationScripts(project)) {
            return null;
        }

        StringBuilder body = new StringBuilder();

        for (Map.Entry<String, String> stage : stages.entrySet()) {

            String stageName = stage.getKey();
            String branchPatter = getBranches(stage.getValue()).get(0);

            body.append("\n");

            String stageTemplate = getJenkinsStage("db-migration-stage")
                    .replace(STAGE_NAME, maskEnvironmentVariable(stageName.toUpperCase()))
                    .replace(MAVEN_PROFILE, maskEnvironmentVariable(stageName.toLowerCase()))
                    .replace(BRANCH_PATTERN, maskEnvironmentVariable(branchPatter));
            body.append(paddLine(stageTemplate, 6));

            body.append("\n");
        }

        return template.replace(STAGES_TAG, body.toString());
    }

    List<String> getBranches(String branches) {

        return Arrays.asList(branches.split(","));
    }

    String fixupAquaStage(String template) {

        if (!service.hasSourceCode(project)) {
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

        if (prodStages.isEmpty() || !service.existsDockerfile(project)) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();

        String stageTemplate = getJenkinsStage("deployment-prod-stage");

        for (String stage : prodStages) {

            String stageName = maskEnvironmentVariable(getStageName(stage));
            String namespace = maskEnvironmentVariable(stage);

            stringBuilder.append("\n");

            stringBuilder.append(paddLine(stageTemplate, 8).replaceFirst(STAGE_NAME, stageName).replaceFirst(STAGE, namespace));

            stringBuilder.append("\n");
        }

        return template.replaceFirst(STAGES_TAG, stringBuilder.toString());
    }

    String getStageName(String stage) {

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

            case "test":
            case "unit-test":
                return getStageOrNull(template, service.hasSourceCode(project));

            case "sonar":
                return fixSonarStage(template);

            case "docker-build":
                return getStageOrNull(template, service.existsDockerfile(project));

            case "deployment":
                return fixupDeploymentStage(template);

            case "versioning":
            case "publish":
            case "tag":
                return getStageOrNull(template, !service.existsDockerfile(project));

            case "db-migration":
                return fixupDbMigrationStage(template);
            case "readiness":
                return fixupReadinessStage(template);
            case "aqua":
                return fixupAquaStage(template);
            case "promote":
                return getStageOrNull(template, !prodStages.isEmpty() && service.existsDockerfile(project));
            case "deployment-prod":
                return fixupDeploymentProd(template);
            default:
                return template;
        }
    }

    void logMessage(String message) {
        getLog().info("+----------------------------------+");
        getLog().info(message);
        getLog().info("+----------------------------------+");
    }
}
