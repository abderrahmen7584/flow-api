import java.util.concurrent.Flow.*;

public class ErrorProcessor implements Processor<String, String> {

    private Subscriber<? super String> subscriber;

    @Override
    public void subscribe(Subscriber<? super String> s) {
        this.subscriber = s;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscriber.onSubscribe(subscription);
        subscription.request(Long.MAX_VALUE); // request everything
    }

    @Override
    public void onNext(String item) {
        if (item.contains("ERROR")) {
            subscriber.onNext(item);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }
}
