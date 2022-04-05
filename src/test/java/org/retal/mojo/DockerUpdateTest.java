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

@ExtendWith(MockitoExtension.class)
@ExtendWith(MojoInjector.class)
public class DockerUpdateTest {

    @InjectMojo("test-config.xml")
    private DockerUpdateContainerMojo mojo;

    @Test
    public void test() throws MojoExecutionException, MojoFailureException {
        Assertions.assertEquals(80, mojo.getExternalPort());
        mojo.execute();
    }

}
