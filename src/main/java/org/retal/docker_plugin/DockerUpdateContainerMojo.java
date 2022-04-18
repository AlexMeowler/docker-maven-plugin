package org.retal.docker_plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.retal.docker_plugin.validation.ExtraParamsValidator;
import org.retal.plugin.test.exception.ExceptionUtil;
import org.retal.plugin.test.logging.AsyncOutputRedirector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Mojo(name = "docker-update-container")
public class DockerUpdateContainerMojo extends AbstractMojo {

    @Parameter(defaultValue = "false")
    private Boolean enableCliOutput;

    @Parameter(defaultValue = "false")
    private Boolean failOnError;

    @Parameter(required = true)
    private String containerName;

    @Parameter(required = true)
    private String imageName;

    @Parameter
    private String[] additionalImageBuildParams;

    @Parameter
    private String[] additionalContainerRunParams;

    @Parameter
    private Integer externalPort;

    @Parameter
    private Integer internalPort;

    @Parameter(defaultValue = ".")
    private String dockerfileDirectory;

    private ExceptionUtil exceptionUtil;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        exceptionUtil = new ExceptionUtil(failOnError, getLog());
        ExtraParamsValidator extraParamsValidator = new ExtraParamsValidator(exceptionUtil);

        if(additionalContainerRunParams == null) {
            additionalContainerRunParams = new String[0];
        }
        if(additionalImageBuildParams == null) {
            additionalImageBuildParams = new String[0];
        }
        extraParamsValidator.validateContainerParams(additionalContainerRunParams);
        extraParamsValidator.validateImageParams(additionalImageBuildParams);

        if(internalPort == null) {
            getLog().warn("Container internal port is not specified");
            internalPort = fetchInternalPort();
        }
        if(externalPort == null) {
            getLog().warn("Container external port is not specified");
            externalPort = fetchExternalPort();
        }

        tryStopAndRemoveContainer();
        tryUpdateImage();
        tryRunContainer();
        getLog().info("Container run successful");
    }

    //TODO fetching ports from existing containers
    private Integer fetchExternalPort() {
        throw new UnsupportedOperationException();
    }

    private Integer fetchInternalPort() {
        throw new UnsupportedOperationException();
    }

    private void tryStopAndRemoveContainer() throws MojoExecutionException {
        getLog().info("Trying to stop and remove container...");
        try {
            String stopContainer = String.format("docker stop %s", containerName);
            getLog().info(stopContainer);
            ProcessBuilder stopContainerProcess = buildDockerProcessWithCommands(stopContainer);
            runProcess(stopContainerProcess);
        } catch (Exception e) {
            exceptionUtil.failOrLog(e);
        }

        try {
            String removeContainer = String.format("docker rm %s", containerName);
            getLog().info(removeContainer);
            ProcessBuilder removeContainerProcess = buildDockerProcessWithCommands(removeContainer);
            runProcess(removeContainerProcess);
        } catch (Exception e) {
            exceptionUtil.failOrLog(e);
        }
    }

    private void tryUpdateImage() throws MojoExecutionException {
        getLog().info("Updating image...");
        try {
            String deleteImage = String.format("docker image rm %s", imageName);
            getLog().info(deleteImage);
            ProcessBuilder deleteImageProcess = buildDockerProcessWithCommands(deleteImage);
            runProcess(deleteImageProcess);
        } catch (Exception e) {
            exceptionUtil.failOrLog(e);
        }

        try {
            String buildImage = String.format("docker build -t %s ", imageName);
            buildImage = attachAdditionalCommands(buildImage, additionalImageBuildParams, dockerfileDirectory);
            getLog().info(buildImage);
            ProcessBuilder buildImageProcess = buildDockerProcessWithCommands(buildImage);
            runProcess(buildImageProcess);
        } catch (Exception e) {
            exceptionUtil.failOrLog(e);
        }
    }

    private void tryRunContainer() throws MojoExecutionException {
        getLog().info("Starting container...");
        try {
            String ports = externalPort == null || internalPort == null ? "" :
                    String.format("-p %d:%d", externalPort, internalPort);
            String startContainer = String.format("docker run -d %s --name %s ",
                    ports, containerName);
            startContainer = attachAdditionalCommands(startContainer, additionalContainerRunParams, imageName);
            getLog().info(startContainer);
            ProcessBuilder startContainerProcess = buildDockerProcessWithCommands(startContainer);
            runProcess(startContainerProcess);
        } catch (Exception e) {
            exceptionUtil.failOrLog(e);
        }
    }

    private ProcessBuilder buildDockerProcessWithCommands(String... commands) throws URISyntaxException {
        String[] cmds = System.getProperty("os.name").startsWith("Windows") ?
                new String[]{"cmd.exe", "/c"} : new String[]{"/bin/bash", "-c"};

        List<String> commandsList = new ArrayList<>(Arrays.asList(cmds));
        commandsList.addAll(Arrays.asList(commands));
        ProcessBuilder pb = new ProcessBuilder(commandsList.toArray(new String[0]))
                .directory(new File(System.getProperty("user.dir")));
        pb.environment().put("DOCKER_BUILDKIT", "0");
        return pb;
    }

    private void runProcess(ProcessBuilder processBuilder) throws InterruptedException, IOException, MojoExecutionException {
        Process process = processBuilder.start();
        InputStream inputStream = InputStream.nullInputStream();
        InputStream errorStream = InputStream.nullInputStream();
        if(enableCliOutput) {
            inputStream = process.getInputStream();
            errorStream  = process.getErrorStream();
        }
        boolean[] hasErrors = {false};
        Throwable[] throwable = {null};
        AsyncOutputRedirector asyncOutputRedirector = new AsyncOutputRedirector(getLog(), inputStream, errorStream, exceptionUtil);
        asyncOutputRedirector.setUncaughtExceptionHandler((thread, exception) -> {
            hasErrors[0] = true;
            throwable[0] = exception;
        });
        asyncOutputRedirector.start();
        process.waitFor();
        asyncOutputRedirector.interrupt();
        if(hasErrors[0]) {
            throw new MojoExecutionException(throwable[0].getCause());
        }
    }

    private String attachAdditionalCommands(String startCommand, String[] cmds, String appendix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(startCommand);
        for(String s : cmds) {
            stringBuilder.append(s + " ");
        }
        if(Objects.isNull(appendix)) {
            appendix = "";
        }
        stringBuilder.append(appendix);
        return stringBuilder.toString();
    }

}
