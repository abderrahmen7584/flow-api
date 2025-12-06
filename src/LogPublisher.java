import java.io.IOException;
import java.nio.file.*;
import java.util.Iterator;
import java.util.concurrent.Flow.*;
import java.util.stream.Stream;

public class LogPublisher implements Publisher<String> {

    private final Path folder;
    private final PathMatcher matcher;

    public LogPublisher(String folderPath) {
        this.folder = Paths.get(folderPath);
        this.matcher = FileSystems.getDefault().getPathMatcher("glob:*.log");
    }

    @Override
    public void subscribe(Subscriber<? super String> subscriber) {
        subscriber.onSubscribe(new Subscription() {
            private volatile boolean cancelled = false;
            private long requested = 0;
            private Iterator<Path> fileIterator;
            private Iterator<String> lineIterator;
            private Stream<String> currentStream;

            @Override
            public synchronized void request(long n) {
                if (cancelled) return;
                requested += n;

                try {
                    if (fileIterator == null) {
                        fileIterator = Files.newDirectoryStream(folder, entry -> matcher.matches(entry.getFileName())).iterator();
                    }

                    while (requested > 0 && !cancelled) {
                        if (lineIterator == null || !lineIterator.hasNext()) {
                            if (currentStream != null) {
                                currentStream.close();
                                currentStream = null;
                            }
                            if (!fileIterator.hasNext()) break;
                            Path nextFile = fileIterator.next();
                            currentStream = Files.lines(nextFile);
                            lineIterator = currentStream.iterator();
                        }

                        if (lineIterator.hasNext()) {
                            subscriber.onNext(lineIterator.next());
                            requested--;
                        }
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
