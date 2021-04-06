package org.springframework.batch.item.redis.support;

import com.hybhub.util.concurrent.ConcurrentSetBlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractKeyspaceNotificationItemReader<K, V> extends AbstractPollableItemReader<K> {

    private final K pubSubPattern;
    private final Converter<K, K> keyExtractor;
    private final BlockingQueue<K> queue;

    protected AbstractKeyspaceNotificationItemReader(Duration readTimeout, K pubSubPattern, Converter<K, K> keyExtractor, int queueCapacity) {
        super(readTimeout);
        Assert.notNull(pubSubPattern, "A pub/sub subscription pattern is required.");
        Assert.notNull(keyExtractor, "A key extractor is required.");
        Assert.isTrue(queueCapacity > 0, "Queue capacity must be greater than zero.");
        this.pubSubPattern = pubSubPattern;
        this.keyExtractor = keyExtractor;
        this.queue = new ConcurrentSetBlockingQueue<>(queueCapacity);
    }

    @Override
    public K poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    @Override
    public void open(ExecutionContext executionContext) {
        super.open(executionContext);
        MetricsUtils.createGaugeCollectionSize("reader.notification.queue.size", queue);
        log.debug("Subscribing to pub/sub pattern {}, queue capacity: {}", pubSubPattern, queue.remainingCapacity());
        subscribe(pubSubPattern);
    }

    protected abstract void subscribe(K pattern);

    @Override
    public void close() {
        super.close();
        log.debug("Unsubscribing from pub/sub pattern {}", pubSubPattern);
        unsubscribe(pubSubPattern);
        queue.clear();
    }

    protected abstract void unsubscribe(K pubSubPattern);

    @SuppressWarnings("unused")
    protected void notification(K channel, V message) {
        if (channel == null) {
            return;
        }
        K key = keyExtractor.convert(channel);
        if (key == null) {
            return;
        }
        boolean success = queue.offer(key);
        if (!success) {
            log.debug("Notification queue full");
        }
    }

}
