import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.concurrent.Flow.*;
import java.util.*;

class LogPublisherTest {

    private Path tempFolder;

    @BeforeEach
    void setup() throws Exception {
        tempFolder = Files.createTempDirectory("logtest");
        // create a test log file
        Files.write(tempFolder.resolve("test.log"), Arrays.asList("line1", "line2", "line3"));
    }

    @AfterEach
    void cleanup() throws Exception {
        Files.walk(tempFolder)
                .map(Path::toFile)
                .forEach(f -> f.delete());
    }

    @Test
    void testPublishesLinesCorrectly() {
        LogPublisher publisher = new LogPublisher(tempFolder.toString());

        List<String> received = new ArrayList<>();

        Subscriber<String> subscriber = new Subscriber<>() {
            Subscription sub;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.sub = subscription;
                sub.request(2); // request only 2 lines
            }

            @Override
            public void onNext(String item) {
                received.add(item);
            }

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
        };

        publisher.subscribe(subscriber);

        Assertions.assertEquals(2, received.size());
        Assertions.assertTrue(received.contains("line1"));
        Assertions.assertTrue(received.contains("line2"));
    }
}
