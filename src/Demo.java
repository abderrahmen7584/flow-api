import java.util.concurrent.Flow;

public class Demo {
    public static void main(String[] args) {

        Flow.Publisher<String> publisher = new LogPublisher("D:/Riot Games/League of Legends/Logs/LeagueClient Logs");

        ErrorProcessor processor = new ErrorProcessor();

        Flow.Subscriber<String> subscriber = new LogSubscriber("logs/errors");

        // IMPORTANT: connect chain from subscriber → backward
        processor.subscribe(subscriber);
        publisher.subscribe(processor);
    }
}
