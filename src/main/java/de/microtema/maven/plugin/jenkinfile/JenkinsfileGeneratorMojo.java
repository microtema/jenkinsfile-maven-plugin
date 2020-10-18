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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.COMPILE)
public class JenkinsfileGeneratorMojo extends AbstractMojo {

    static final String STAGES_TAG = "@STAGES@";
    static final String CLOSURE_TAG = "@CLOSURE@";
    static final String FUNCTIONS_TAG = "@FUNCTIONS@";
    static final String STAGE_NAME = "@STAGE_NAME@";
    static final String ENDPOINT = "@ENDPOINT@";
    static final String STAGE_DISPLAY_NAME = "@STAGE_DISPLAY_NAME@";
    static final String MAVEN_PROFILE = "@MAVEN_PROFILE@";
    static final String JOB_NAME = "@JOB_NAME@";
    static final String BRANCH_PATTERN = "@BRANCH_PATTERN@";
    static final String AGENT = "@AGENT@";
    static final String ENVIRONMENT = "@ENVIRONMENT@";
    static final String OPTIONS = "@OPTIONS@";
    static final String TRIGGERS = "@TRIGGERS@";
    static final String BOOTSTRAP_URL = "@BOOTSTRAP_URL@";
    static final String STAGE = "@STAGE@";
    static final String SONAR_TOKEN = "@SONAR_TOKEN@";
    static final String TIMEOUT_TAG = "@TIMEOUT@";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "upstream-projects")
    List<String> upstreamProjects = new ArrayList<>();

    @Parameter(property = "downstream-projects")
    List<String> downstreamProjects = new ArrayList<>();

    @Parameter(property = "prod-stages")
    List<String> prodStages = new ArrayList<>();

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

    @Parameter(property = "app-name")
    String appName;

    @Parameter(property = "base-namespace")
    String baseNamespace;

    @Parameter(property = "environments")
    LinkedHashMap<String, String> environments = new LinkedHashMap<>();

    @Parameter(property = "stages")
    LinkedHashMap<String, String> stages = new LinkedHashMap<>();

    @Parameter(property = "clusters")
    LinkedHashMap<String, String> clusters = new LinkedHashMap<>();

    @Parameter(property = "update")
    boolean update = true;

    @Parameter(property = "timeout")
    int timeout = 60; // in minutes

    @Parameter(property = "sonar")
    boolean sonar = true;

    @Parameter(property = "sonar-quality-gate")
    boolean sonarQualityGate = true;

    @Parameter(property = "readiness")
    boolean readiness = true;

    @Parameter(property = "readiness-endpoint")
    String readinessEndpoint;

    @Parameter(property = "readiness-closure")
    String readinessClosure;

    @Parameter(property = "performance-test")
    boolean performanceTest = true;

    JenkinsfileGeneratorService service = new JenkinsfileGeneratorService();

    List<String> functionTemplates = new ArrayList<>();

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
        String options = getJenkinsStage("options").replace(TIMEOUT_TAG, String.valueOf(timeout));
        String triggers = buildTriggers();
        String stagesTemplate = buildStages();
        String pipeline = getJenkinsStage("pipeline");

        pipeline = pipeline.replace(AGENT, paddLine(agent, 4))
                .replace(ENVIRONMENT, paddLine(environment, 4))
                .replace(OPTIONS, paddLine(options, 4))
                .replace(TRIGGERS, paddLine(triggers, 4))
                .replace(STAGES_TAG, paddLine(stagesTemplate, 4))
                .replace(FUNCTIONS_TAG, getFunctions())
                .replaceFirst("\\n$", "")
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
            stages.put("itu", "release-*,hotfix-*,master");
            stages.put("satu", "master");
        }

        if (clusters.isEmpty()) {
            clusters.put("etu", "test-ekad-caas1.rz.bankenit.de");
            clusters.put("itu", "test-ekad-caas1.rz.bankenit.de");
            clusters.put("satu", "satu-ekad-caas1.rz.bankenit.de");
        }

        if (readiness && StringUtils.isEmpty(readinessClosure)) {
            readinessEndpoint = Optional.ofNullable(readinessEndpoint).orElse("/" + appName + "/actuator/info/git");
        }

        if (StringUtils.isNoneEmpty(readinessEndpoint)) {
            if (!readinessEndpoint.startsWith("http")) {
                readinessEndpoint = "https://$BASE_NAMESPACE-$STAGE_NAME.$CLUSTER_URL" + (readinessEndpoint.startsWith("/") ? "" : "/") + readinessEndpoint;
            }
        }

        // Prevent recursive upstreams
        upstreamProjects.removeIf(it -> StringUtils.equalsIgnoreCase(it, project.getArtifactId()));
    }

    String buildStages() {

        String test = getTestStageName();

        List<String> stageNames = new ArrayList<>(Arrays.asList("initialize", "versioning", "compile", test, "maven-build"));

        if (sonar) {
            stageNames.add("sonar");
        }

        stageNames.add("security");

        if (sonarQualityGate && sonar) {
            stageNames.add("sonar-quality-gate");
        }

        stageNames.addAll(Arrays.asList("db-migration", "docker-build", "tag", "publish", "deployment"));

        stageNames.add("aqua");

        if (readiness) {
            stageNames.add("readiness");
        }

        stageNames.add("regression-test");

        if (performanceTest) {
            stageNames.add("performance-test");
        }

        stageNames.addAll(Arrays.asList("promote", "deployment-prod"));

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

    String getFunctions() {

        if (functionTemplates.isEmpty()) {
            return StringUtils.EMPTY;
        }

        StringBuilder template = new StringBuilder();

        for (String function : functionTemplates) {
            template.append("\n");
            template.append(function);
        }

        return template.toString();
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

    String fixSonarQualityGateStage(String template) {

        if (!service.hasSourceCode(project)) {

            return null;
        }

        if (!service.hasSonarProperties(project)) {

            getLog().info("Skip Stage(Sonar Quality Gates) in Jenkinsfile, since there is no sonar properties configured: " + appName);

            return null;
        }

        String functionTemplate = getJenkinsStage("sonar-quality-gate-function");

        functionTemplates.add(functionTemplate);

        return template.replaceFirst(SONAR_TOKEN, maskEnvironmentVariable(project.getProperties().getProperty("sonar.login")));
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

        String functionTemplate = getJenkinsStage("readiness-functions");
        String templateName = "readiness-stage";

        if (StringUtils.isEmpty(readinessEndpoint)) {
            functionTemplate = getJenkinsStage("readiness-functions-oc");
            templateName = "readiness-stage-oc";
        }

        functionTemplates.add(functionTemplate);

        StringBuilder body = new StringBuilder();

        for (Map.Entry<String, String> stage : stages.entrySet()) {

            String stageName = stage.getKey();
            String clusterUrl = clusters.getOrDefault(stageName, stageName + ".de");

            for (String branchPatter : getBranches(stage.getValue())) {

                body.append("\n");

                String endpoint = StringUtils.trimToEmpty(readinessEndpoint)
                        .replace("$BASE_NAMESPACE", baseNamespace)
                        .replace("$STAGE_NAME", stageName.toLowerCase())
                        .replace("$CLUSTER_URL", clusterUrl);

                String stageTemplate = getJenkinsStage(templateName)
                        .replaceAll(STAGE_DISPLAY_NAME, getStageDisplayName(stageName, branchPatter))
                        .replaceAll(STAGE_NAME, maskEnvironmentVariable(stageName.toLowerCase()))
                        .replaceAll(ENDPOINT, maskEnvironmentVariable(endpoint))
                        .replaceAll(CLOSURE_TAG, Optional.ofNullable(readinessClosure).orElse("{ it.commitId == env.GIT_COMMIT }"))
                        .replace(BRANCH_PATTERN, maskEnvironmentVariable(branchPatter));
                body.append(paddLine(stageTemplate, 8));

                body.append("\n");
            }

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
                        .replaceAll(STAGE_NAME, maskEnvironmentVariable(stageName))
                        .replaceAll(STAGE_DISPLAY_NAME, getStageDisplayName(stageName, branchPatter))
                        .replace("@DEPLOY_FUNCTION@", getDeployFunction(branchPatter))
                        .replace(BRANCH_PATTERN, maskEnvironmentVariable(branchPatter));
                body.append(paddLine(stageTemplate, 8));

                body.append("\n");
            }
        }

        return template.replace(STAGES_TAG, body.toString());
    }

    private String getDeployFunction(String branchPatter) {

        if (branchPatter.equalsIgnoreCase("feature-*")) {
            return "createBranchIfNotExistsAndCommitChanges()";
        }

        return "createAndMergeOpsRepoMergeRequest()";
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

            for (String branchPatter : getBranches(stage.getValue())) {

                body.append("\n");

                String stageTemplate = getJenkinsStage("db-migration-stage")
                        .replace(STAGE_DISPLAY_NAME, getStageDisplayName(stageName, branchPatter))
                        .replace(STAGE_NAME, maskEnvironmentVariable(stageName.toLowerCase()))
                        .replace(MAVEN_PROFILE, maskEnvironmentVariable(stageName.toLowerCase()))
                        .replace(BRANCH_PATTERN, maskEnvironmentVariable(branchPatter));
                body.append(paddLine(stageTemplate, 6));

                body.append("\n");
            }
        }

        return template.replace(STAGES_TAG, body.toString());
    }

    String fixupPerformanceTestStage(String template) {

        if (!service.existsJmeterFile(project)) {
            return null;
        }

        functionTemplates.add(getJenkinsStage("performance-test-function"));

        StringBuilder body = new StringBuilder();

        for (Map.Entry<String, String> stage : stages.entrySet()) {

            String stageName = stage.getKey();

            for (String branchPatter : getBranches(stage.getValue())) {

                body.append("\n");

                String stageTemplate = getJenkinsStage("performance-test-stage")
                        .replace(STAGE_DISPLAY_NAME, getStageDisplayName(stageName, branchPatter))
                        .replace(STAGE_NAME, maskEnvironmentVariable(stageName.toLowerCase()))
                        .replace(MAVEN_PROFILE, maskEnvironmentVariable(stageName.toLowerCase()))
                        .replace(BRANCH_PATTERN, maskEnvironmentVariable(branchPatter));
                body.append(paddLine(stageTemplate, 8));

                body.append("\n");
            }
        }

        return template.replace(STAGES_TAG, body.toString());
    }

    String fixupRegressionTestStage(String template) {

        if (downstreamProjects.isEmpty()) {
            return null;
        }

        functionTemplates.add(getJenkinsStage("regression-test-function"));

        StringBuilder body = new StringBuilder();

        int index = 0;
        boolean multiple = downstreamProjects.size() > 1;

        for (String downstreamProject : downstreamProjects) {

            String stageDisplayName = "Regression Tests" + (multiple ? " (" + (index + 1) + ")" : "");

            body.append(fixupRegressionTestStageImpl(template, downstreamProject, stageDisplayName));

            if (multiple) {
                body.append("\n");
            }

            index++;
        }

        return body.toString();
    }

    String fixupRegressionTestStageImpl(String template, String downstreamProject, String stageDisplayName) {

        StringBuilder body = new StringBuilder();

        for (Map.Entry<String, String> stage : stages.entrySet()) {

            String stageName = stage.getKey();

            for (String branchPatter : getBranches(stage.getValue())) {

                body.append("\n");

                String stageTemplate = getJenkinsStage("regression-test-stage")
                        .replace(STAGE_DISPLAY_NAME, getStageDisplayName(stageName, downstreamProject + "/" + branchPatter))
                        .replace(STAGE_NAME, maskEnvironmentVariable(stageName.toLowerCase()))
                        .replace(JOB_NAME, maskEnvironmentVariable(downstreamProject.toLowerCase()))
                        .replace(BRANCH_PATTERN, maskEnvironmentVariable(branchPatter));
                body.append(paddLine(stageTemplate, 8));

                body.append("\n");
            }
        }

        return template.replace(STAGE_NAME, maskEnvironmentVariable(stageDisplayName)).replace(STAGES_TAG, body.toString());
    }

    List<String> getBranches(String branches) {

        return Arrays.asList(branches.split(","));
    }

    String fixupAquaStage(String template) {

        if (!service.hasSourceCode(project)) {
            return null;
        }

        functionTemplates.add(getJenkinsStage("aqua-function"));

        StringBuilder stringBuilder = new StringBuilder();

        String stageTemplate = getJenkinsStage("aqua-stage");

        List<Map<String, String>> tests = new ArrayList<>();

        if (StringUtils.isNotEmpty(aquaJunitFolderId)) {
            Map<String, String> properties = new HashMap<>();
            properties.put("STAGE_NAME", "Unit Tests");
            properties.put("AQUA_FILE_FILTER", "**/*Test.xml");
            properties.put("AQUA_FOLDER_ID", aquaJunitFolderId);
            properties.put("AQUA_TEST_TYPE", "Komponententest");

            tests.add(properties);
        }

        if (StringUtils.isNotEmpty(aquaITFolderId)) {
            Map<String, String> properties = new HashMap<>();
            properties.put("STAGE_NAME", "Integration Tests");
            properties.put("AQUA_FILE_FILTER", "**/*IT.xml");
            properties.put("AQUA_FOLDER_ID", aquaITFolderId);
            properties.put("AQUA_TEST_TYPE", "Integrationstest");

            tests.add(properties);
        }

        for (Map<String, String> properties : tests) {

            String stageName = maskEnvironmentVariable(properties.get("STAGE_NAME"));

            stringBuilder.append("\n");

            stringBuilder.append(paddLine(stageTemplate, 8).replace(STAGE_NAME, stageName)
                    .replace("@AQUA_FILE_FILTER@", maskEnvironmentVariable(properties.get("AQUA_FILE_FILTER")))
                    .replace("@AQUA_TEST_TYPE@", maskEnvironmentVariable(properties.get("AQUA_TEST_TYPE")))
                    .replace("@AQUA_FOLDER_ID@", maskEnvironmentVariable(properties.get("AQUA_FOLDER_ID"))));

            stringBuilder.append("\n");
        }

        return template.replaceFirst("@AQUA_PROJECT_ID@", maskEnvironmentVariable(aquaProjectId))
                .replace("@AQUA_URL@", maskEnvironmentVariable(aquaUrl))
                .replace("@AQUA_PRODUCT_ID@", maskEnvironmentVariable(aquaProductId))
                .replace("@AQUA_RELEASE@", maskEnvironmentVariable(aquaRelease))
                .replace("@AQUA_LEVEL@", maskEnvironmentVariable(aquaLevel))
                .replace("@AQUA_JUNIT_TEST_FOLDER_ID@", maskEnvironmentVariable(aquaJunitFolderId))
                .replace("@AQUA_INTEGRATION_TEST_FOLDER_ID@", maskEnvironmentVariable(aquaITFolderId))
                .replace(STAGES_TAG, stringBuilder.toString());
    }

    private String fixupPromoteStage(String template) {

        if (prodStages.isEmpty() || !service.existsDockerfile(project)) {
            return null;
        }

        return template.replace(TIMEOUT_TAG, String.valueOf(timeout / 2));
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

        return tokens[tokens.length - 1].toUpperCase();
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
            case "sonar-quality-gate":
                return fixSonarQualityGateStage(template);

            case "docker-build":
                return getStageOrNull(template, service.existsDockerfile(project));

            case "deployment":
                return fixupDeploymentStage(template);

            case "versioning":
                return fixupVersioningStage(template);

            case "publish":
            case "tag":
                return getStageOrNull(template, !service.existsDockerfile(project));

            case "db-migration":
                return fixupDbMigrationStage(template);
            case "readiness":
                return fixupReadinessStage(template);

            case "regression-test":
                return fixupRegressionTestStage(template);
            case "performance-test":
                return fixupPerformanceTestStage(template);
            case "aqua":
                return fixupAquaStage(template);
            case "promote":
                return fixupPromoteStage(template);
            case "deployment-prod":
                return fixupDeploymentProd(template);
            default:
                return template;
        }
    }

    private String fixupVersioningStage(String template) {

        if (service.existsDockerfile(project)) {
            return null;
        }

        functionTemplates.add(getJenkinsStage("versioning-function"));

        return template;
    }

    void logMessage(String message) {
        getLog().info("+----------------------------------+");
        getLog().info(message);
        getLog().info("+----------------------------------+");
    }
}
