package org.retal.docker_plugin.validation;

import org.apache.maven.plugin.MojoExecutionException;
import org.retal.plugin.test.exception.ExceptionUtil;

import java.util.Arrays;
import java.util.List;

public class ExtraParamsValidator {

    private final ExceptionUtil exceptionUtil;

    private final List<String> RESERVED_IMAGE_PARAMS = Arrays.asList("-t", "--tag");

    private final List<String> RESERVED_CONTAINER_PARAMS = Arrays.asList("-d", "--detach", "-p", "--publish-all", "--name");

    public ExtraParamsValidator(ExceptionUtil exceptionUtil) {
        this.exceptionUtil = exceptionUtil;
    }

    public void validateImageParams(String... params) throws MojoExecutionException {
        for(String param : params) {
            String[] splittedParam = param.split(" ");
            String paramName = splittedParam.length != 0 ? splittedParam[0] : param;
            if(RESERVED_IMAGE_PARAMS.indexOf(paramName) != -1) {
                exceptionUtil.failOrLog(String.format("%s is specified by default!", paramName));
            }
        }
    }

    public void validateContainerParams(String... params) throws MojoExecutionException {
        for(String param : params) {
            String[] splittedParam = param.split(" ");
            String paramName = splittedParam.length != 0 ? splittedParam[0] : param;
            if(RESERVED_CONTAINER_PARAMS.indexOf(paramName) != -1) {
                exceptionUtil.failOrLog(String.format("%s is specified by default!", paramName));
            }
        }
    }
}
