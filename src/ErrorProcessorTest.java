import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.Flow.*;

class ErrorProcessorTest {

    @Test
    void testFiltersErrorLines() {
        ErrorProcessor processor = new ErrorProcessor();

        List<String> received = new ArrayList<>();
        Subscriber<String> subscriber = new Subscriber<>() {
            @Override public void onSubscribe(Subscription subscription) {}
            @Override public void onNext(String item) { received.add(item); }
            @Override public void onError(Throwable throwable) {}
            @Override public void onComplete() {}
        };

        processor.subscribe(subscriber);

        processor.onNext("INFO line");
        processor.onNext("ERROR line 1");
        processor.onNext("ERROR line 2");

        Assertions.assertEquals(2, received.size());
        Assertions.assertTrue(received.contains("ERROR line 1"));
        Assertions.assertTrue(received.contains("ERROR line 2"));
    }
}
