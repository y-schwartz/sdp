package org.yschwartz.sdp.docker.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

@Configuration
public class DockerConfig {

    @Value("${docker.host:localhost}")
    private String host;
    @Value("${docker.port:2375}")
    private int port;

    @Bean
    public DockerClient dockerClient() {
        String uri = "tcp://%s:%d".formatted(host, port);
        var config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(uri)
                .build();
        var client = new ApacheDockerHttpClient.Builder()
                .dockerHost(URI.create(uri))
                .build();
        return DockerClientImpl.getInstance(config, client);
    }
}
