import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.Flow.*;

public class LogPublisher implements Publisher<String> {

    private final Path folder;

    public LogPublisher(String folderPath) {
        this.folder = Paths.get(folderPath);
    }

    @Override
    public void subscribe(Subscriber<? super String> subscriber) {
        subscriber.onSubscribe(new Subscription() {
            private volatile boolean cancelled = false;

            @Override
            public void request(long n) {
                if (cancelled) return;

                try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.log")) {
                    long sent = 0;
                    for (Path file : stream) {
                        if (sent >= n) break;
                        Files.lines(file).forEach(line -> {
                            if (!cancelled) subscriber.onNext(line);
                        });
                        sent++;
                    }
                } catch (IOException e) {
                    subscriber.onError(e);
                }
            }

            @Override
            public void cancel() {
                cancelled = true;
            }
        });
    }
}
