import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Flow.*;

public class LogSubscriber implements Subscriber<String> {

    private final Path folder;
    private Subscription subscription;

    public LogSubscriber(String folderPath) {
        this.folder = Paths.get(folderPath);

        // 🔥 Create the folder if it doesn't exist
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
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(String item) {
        try {
            // Timestamped file name
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS")
                    .format(new Date());

            Path outputFile = folder.resolve("error-" + timestamp + ".log");

            Files.writeString(
                    outputFile,
                    item + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE
            );

            System.out.println("Saved error to: " + outputFile.getFileName());

        } catch (IOException e) {
            subscription.cancel();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Subscriber error: " + throwable.getMessage());
    }

    @Override
    public void onComplete() {
        System.out.println("Finished receiving logs.");
    }
}
