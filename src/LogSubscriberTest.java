import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.*;

class LogSubscriberTest {

    private Path tempFolder;

    @BeforeEach
    void setup() throws Exception {
        tempFolder = Files.createTempDirectory("subscribertest");
    }

    @AfterEach
    void cleanup() throws Exception {
        Files.walk(tempFolder)
                .map(Path::toFile)
                .forEach(f -> f.delete());
    }

    @Test
    void testFlushBufferCreatesFile() throws Exception {
        LogSubscriber subscriber = new LogSubscriber(tempFolder.toString());

        Flow.Subscription sub = new Flow.Subscription() {
            @Override public void request(long n) {}
            @Override public void cancel() {}
        };
        subscriber.onSubscribe(sub);

        // simulate receiving BATCH_SIZE lines
        for (int i = 0; i < 10; i++) subscriber.onNext("ERROR line " + i);

        // wait for async flush
        Thread.sleep(1000);

        Assertions.assertTrue(Files.list(tempFolder).findAny().isPresent());
    }
}
