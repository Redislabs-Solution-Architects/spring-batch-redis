package org.springframework.batch.item.redis;

import org.springframework.batch.item.redis.support.AbstractKeyValue;
import org.springframework.batch.item.redis.support.AbstractRedisItemWriter;
import org.springframework.batch.item.redis.support.KeyDump;
import org.springframework.batch.item.redis.support.RedisItemWriterBuilder;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.RestoreArgs;
import io.lettuce.core.api.async.BaseRedisAsyncCommands;
import io.lettuce.core.api.async.RedisKeyAsyncCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

public class RedisKeyDumpItemWriter<K, V> extends AbstractRedisItemWriter<K, V, KeyDump<K>> {

	@Override
	@SuppressWarnings("unchecked")
	protected RedisFuture<?> write(BaseRedisAsyncCommands<K, V> commands, KeyDump<K> item) {
		RedisKeyAsyncCommands<K, V> keyCommands = (RedisKeyAsyncCommands<K, V>) commands;
		if (item.getValue() == null || item.getTtl() == AbstractKeyValue.TTL_NOT_EXISTS) {
			return keyCommands.del(item.getKey());
		}
		if (item.getTtl() >= 0) {
			long ttl = item.getTtl() * 1000;
			return keyCommands.restore(item.getKey(), item.getValue(), new RestoreArgs().ttl(ttl).replace(replace));
		}
		return keyCommands.restore(item.getKey(), item.getValue(), new RestoreArgs().replace(replace));
	}

	private final boolean replace;

	public RedisKeyDumpItemWriter(boolean replace) {
		this.replace = replace;
	}

	public static RedisKeyDumpItemWriterBuilder<String, String> builder() {
		return new RedisKeyDumpItemWriterBuilder<>(StringCodec.UTF8);
	}

	public static class RedisKeyDumpItemWriterBuilder<K, V>
			extends RedisItemWriterBuilder<K, V, RedisKeyDumpItemWriterBuilder<K, V>> {

		public RedisKeyDumpItemWriterBuilder(RedisCodec<K, V> codec) {
			super(codec);
		}

		private boolean replace;

		public RedisKeyDumpItemWriterBuilder<K, V> replace(boolean replace) {
			this.replace = replace;
			return this;
		}

		public RedisKeyDumpItemWriter<K, V> build() {
			return configure(new RedisKeyDumpItemWriter<>(replace));
		}

	}

}
