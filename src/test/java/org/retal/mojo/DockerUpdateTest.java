package org.retal.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.retal.docker_plugin.DockerUpdateContainerMojo;
import org.retal.util.annotation.InjectMojo;
import org.retal.util.annotation.processor.MojoInjector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MojoInjector.class)
public class DockerUpdateTest {

    @InjectMojo("test-config.xml")
    private DockerUpdateContainerMojo mojo;

    @InjectMojo("test-config-invalid.xml")
    private DockerUpdateContainerMojo invalidMojo;

    @Test
    public void executeDefaultConfig() throws Exception {
        mojo.execute();

        ProcessBuilder processBuilder = buildDockerProcessWithCommands("docker ps");
        Process process = processBuilder.start();
        InputStream inputStream = process.getInputStream();
        process.waitFor();
        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            long imageCount = bufferedReader.lines()
                    .filter(line -> line.split(" +")[1].equals("springboot"))
                    .count();
            Assertions.assertEquals(1, imageCount);
        } catch (IOException e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void invalidXmlFail() throws MojoExecutionException, MojoFailureException {
        Assertions.assertThrows(MojoExecutionException.class, () -> invalidMojo.execute(), "Should fail on \"docker stop\"");
    }

    private ProcessBuilder buildDockerProcessWithCommands(String... commands) {
        String[] cmds = System.getProperty("os.name").startsWith("Windows") ?
                new String[]{"cmd.exe", "/c"} : new String[]{"/bin/bash", "-c"};

        List<String> commandsList = new ArrayList<>(Arrays.asList(cmds));
        commandsList.addAll(Arrays.asList(commands));
        ProcessBuilder pb = new ProcessBuilder(commandsList.toArray(new String[0]))
                .redirectErrorStream(true);
        return pb;
    }

}
