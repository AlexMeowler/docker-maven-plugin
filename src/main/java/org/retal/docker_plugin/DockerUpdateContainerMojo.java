package org.retal.docker_plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "docker-update-container", defaultPhase = LifecyclePhase.PACKAGE)
public class DockerUpdateContainerMojo extends AbstractMojo {

    @Parameter(defaultValue = "false")
    private Boolean enableCliOutput;

    @Parameter(defaultValue = "true")
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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(additionalContainerRunParams == null) {
            additionalContainerRunParams = new String[0];
        }
        if(additionalImageBuildParams == null) {
            additionalImageBuildParams = new String[0];
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

    private Integer fetchExternalPort() {
        return null;
    }

    private void tryStopAndRemoveContainer() throws MojoExecutionException {
        getLog().info("Trying to stop and remove container...");
        try {
            String stopContainer = String.format("docker stop %s", containerName);
            ProcessBuilder stopContainerProcess = buildProcessWithCommands(stopContainer);
            stopContainerProcess.start().waitFor();
        } catch (Exception e) {
            failOrLog(e);
        }

        try {
            String deleteContainer = String.format("docker rm %s", containerName);
            ProcessBuilder stopContainerProcess = buildProcessWithCommands(deleteContainer);
            stopContainerProcess.start().waitFor();
        } catch (Exception e) {
            failOrLog(e);
        }
    }

    private void tryUpdateImage() throws MojoExecutionException {
        getLog().info("Updating image...");
        try {
            String deleteImage = String.format("docker image rm %s", imageName);
            ProcessBuilder stopContainerProcess = buildProcessWithCommands(deleteImage);
            stopContainerProcess.start().waitFor();
        } catch (Exception e) {
            failOrLog(e);
        }

        try {
            String updateImage = String.format("docker build -t %s ", imageName);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(updateImage);
            for(String s : additionalImageBuildParams) {
                stringBuilder.append(s + " ");
            }
            stringBuilder.append(".");
            updateImage = stringBuilder.toString();
            ProcessBuilder stopContainerProcess = buildProcessWithCommands(updateImage);
            stopContainerProcess.start().waitFor();
        } catch (Exception e) {
            failOrLog(e);
        }
    }

    private void tryRunContainer() throws MojoExecutionException {
        getLog().info("Starting container...");
        try {
            String ports = externalPort == null || internalPort == null ? "" :
                    String.format("-p %d:%d", externalPort, internalPort);
            String startContainer = String.format("docker run -d %s --name %s ",
                    ports, containerName);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(startContainer);
            for(String s : additionalContainerRunParams) {
                stringBuilder.append(s + " ");
            }
            stringBuilder.append(imageName);
            startContainer = stringBuilder.toString();
            ProcessBuilder stopContainerProcess = buildProcessWithCommands(startContainer);
            stopContainerProcess.start().waitFor();
        } catch (Exception e) {
            failOrLog(e);
        }
    }

    private ProcessBuilder buildProcessWithCommands(String... commands) {
        List<String> commandsList = new ArrayList<>(Arrays.asList("cmd.exe", "/c"));
        commandsList.addAll(Arrays.asList(commands));
        ProcessBuilder pb = new ProcessBuilder(commandsList.toArray(new String[0]))
                .redirectErrorStream(true);
        if(enableCliOutput) {
            pb.inheritIO();
        }
        return pb;
    }

    private void failOrLog(Throwable throwable) throws MojoExecutionException {
        if(failOnError) {
            throw new MojoExecutionException(throwable);
        }
        getLog().error(throwable);
    }
}
