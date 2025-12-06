import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Flow.*;

public class LogSubscriber implements Subscriber<String> {

    private static final int BATCH_SIZE = 20; // how many log lines to buffer before requesting more

    private final Path folder;
    private Subscription subscription;
    private final List<String> buffer = new ArrayList<>(BATCH_SIZE);

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

        // 🔥 Request first batch
        subscription.request(BATCH_SIZE);
        System.out.println("Subscriber: Requested first " + BATCH_SIZE + " items");
    }

    @Override
    public void onNext(String item) {
        buffer.add(item);

        // When buffer is full → flush to file
        if (buffer.size() >= BATCH_SIZE) {
            flushBuffer();
            subscription.request(BATCH_SIZE); // request next batch
            System.out.println("Subscriber: Requested next " + BATCH_SIZE + " items");
        }
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Subscriber error: " + throwable.getMessage());
        flushBuffer(); // flush remaining data to avoid loss
    }

    @Override
    public void onComplete() {
        System.out.println("Subscriber: Completed");
        flushBuffer(); // flush last items
    }

    // ======================
    // 🔥 Flush buffer to disk
    // ======================
    private void flushBuffer() {
        if (buffer.isEmpty()) return;

        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS")
                    .format(new Date());

            Path outputFile = folder.resolve("error-batch-" + timestamp + ".log");

            Files.write(
                    outputFile,
                    buffer,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE
            );

            System.out.println("Flushed " + buffer.size() +
                    " items → " + outputFile.getFileName());

            buffer.clear(); // reset buffer

        } catch (IOException e) {
            subscription.cancel();
            System.err.println("Failed to write batch: " + e.getMessage());
        }
    }
}
