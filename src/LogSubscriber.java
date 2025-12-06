import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow.*;

public class LogSubscriber implements Subscriber<String> {

    private static final int BATCH_SIZE = 50; // how many log lines to buffer before requesting more

    private final Path folder;
    private Subscription subscription;
    private final List<String> buffer = new ArrayList<>(BATCH_SIZE);

    // Executor for async flush
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LogSubscriber(String folderPath) {
        this.folder = Paths.get(folderPath);

        try {
            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
                System.out.println("Created folder: " + folder.toAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create folder: " + folderPath, e);
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(BATCH_SIZE); // request first batch
        System.out.println("Subscriber: Requested first " + BATCH_SIZE + " items");
    }

    @Override
    public void onNext(String item) {
        synchronized (buffer) {
            buffer.add(item);

            if (buffer.size() >= BATCH_SIZE) {
                List<String> itemsToFlush = new ArrayList<>(buffer);
                buffer.clear(); // clear immediately to prevent memory spike

                // Flush asynchronously
                executor.submit(() -> flushBuffer(itemsToFlush));

                subscription.request(BATCH_SIZE); // request next batch
                System.out.println("Subscriber: Requested next " + BATCH_SIZE + " items");
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println("Subscriber error: " + throwable.getMessage());
        flushBufferAsync();
        executor.shutdown();
    }

    @Override
    public void onComplete() {
        System.out.println("Subscriber: Completed");
        flushBufferAsync();
        executor.shutdown();
    }

    // ======================
    // Async flush helper
    // ======================
    private void flushBufferAsync() {
        List<String> itemsToFlush;
        synchronized (buffer) {
            if (buffer.isEmpty()) return;
            itemsToFlush = new ArrayList<>(buffer);
            buffer.clear();
        }
        executor.submit(() -> flushBuffer(itemsToFlush));
    }

    private void flushBuffer(List<String> items) {
        if (items.isEmpty()) return;

        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS")
                    .format(new Date());

            Path outputFile = folder.resolve("error-batch-" + timestamp + ".log");

            Files.write(
                    outputFile,
                    items,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE
            );

            System.out.println("Flushed " + items.size() +
                    " items → " + outputFile.getFileName());

        } catch (IOException e) {
            if (subscription != null) subscription.cancel();
            System.err.println("Failed to write batch: " + e.getMessage());
        }
    }
}
