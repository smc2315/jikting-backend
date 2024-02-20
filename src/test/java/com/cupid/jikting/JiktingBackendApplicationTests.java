package com.cupid.jikting;

import com.cupid.jikting.common.config.RedisContainerInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(initializers = RedisContainerInitializer.class)
@SpringBootTest
class JiktingBackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
