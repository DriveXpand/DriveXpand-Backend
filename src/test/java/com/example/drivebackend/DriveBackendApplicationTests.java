package com.example.drivebackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "app.auth.username=testuser",
    "app.auth.password=testpass",
    "app.auth.secret=testsecretkey"
})
class DriveBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
