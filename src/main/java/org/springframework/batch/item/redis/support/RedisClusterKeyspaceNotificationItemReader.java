package org.springframework.batch.item.redis.support;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubAdapter;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import lombok.Builder;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

import java.time.Duration;

public class RedisClusterKeyspaceNotificationItemReader<K, V> extends AbstractKeyspaceNotificationItemReader<K, V> {

    private final StatefulRedisClusterPubSubConnection<K, V> connection;
    private final ClusterKeyspaceNotificationListener listener = new ClusterKeyspaceNotificationListener();

    @Builder
    public RedisClusterKeyspaceNotificationItemReader(Duration readTimeout, StatefulRedisClusterPubSubConnection<K, V> connection, K pubSubPattern, Converter<K, K> keyExtractor, int queueCapacity) {
        super(readTimeout, pubSubPattern, keyExtractor, queueCapacity);
        Assert.notNull(connection, "A pub/sub connection is required.");
        this.connection = connection;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void subscribe(K pattern) {
        connection.addListener(listener);
        connection.setNodeMessagePropagation(true);
        connection.sync().upstream().commands().psubscribe(pattern);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void unsubscribe(K pubSubPattern) {
        connection.sync().upstream().commands().punsubscribe(pubSubPattern);
        connection.removeListener(listener);
    }

    private class ClusterKeyspaceNotificationListener extends RedisClusterPubSubAdapter<K, V> {

        @Override
        public void message(RedisClusterNode node, K channel, V message) {
            notification(channel, message);
        }

        @Override
        public void message(RedisClusterNode node, K pattern, K channel, V message) {
            notification(channel, message);
        }
    }

}
