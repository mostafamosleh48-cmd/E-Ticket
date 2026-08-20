package com.mostafa.eticket.observability;

import com.mostafa.eticket.service.AspectTestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class ServiceLoggingAspectTest {

    @Autowired
    private AspectTestService aspectTestService;

    @Test
    void logsMethodNameDurationAndSuccessOutcome(CapturedOutput output) {
        aspectTestService.doWork("hello");

        assertThat(output)
                .contains("AspectTestService.doWork")
                .contains("returned successfully")
                .contains("ms");
    }

    @Test
    void logsExceptionClassOnFailure(CapturedOutput output) {
        assertThatThrownBy(() -> aspectTestService.failWork())
                .isInstanceOf(IllegalStateException.class);

        assertThat(output)
                .contains("AspectTestService.failWork")
                .contains("threw IllegalStateException");
    }

    @Test
    void neverLogsArgumentValues(CapturedOutput output) {
        String secret = "s3cr3t-t0ken-123";

        aspectTestService.doWork(secret);

        assertThat(output).doesNotContain(secret);
    }
}