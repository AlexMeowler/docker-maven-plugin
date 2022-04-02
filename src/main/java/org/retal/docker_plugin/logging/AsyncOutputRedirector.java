package org.retal.docker_plugin.logging;

import org.apache.maven.plugin.logging.Log;

import java.io.InputStream;

public class AsyncOutputRedirector extends Thread {

    private final Log log;

    private final InputStream inputStream;

    public AsyncOutputRedirector(Log log, InputStream inputStream) {
        super();
        this.log = log;
        this.inputStream = inputStream;
    }

    @Override
    public void run() {

    }
}
