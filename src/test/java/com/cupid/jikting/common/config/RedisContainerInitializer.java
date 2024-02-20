package com.cupid.jikting.common.config;


import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class RedisContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final GenericContainer redisContainer =
            new GenericContainer(DockerImageName.parse("redis").withTag("5.0.3-alpine"))
                    .withExposedPorts(6379);

    static {
        redisContainer.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String host = redisContainer.getHost();
        Integer port = redisContainer.getMappedPort(6379);
        String redisUrl = "redis://" + host + ":" + port;
        applicationContext.getEnvironment().getSystemProperties().put("spring.redis.host", host);
        applicationContext.getEnvironment().getSystemProperties().put("spring.redis.port", port.toString());
        applicationContext.getEnvironment().getSystemProperties().put("spring.redis.url", redisUrl);
    }
}
