package br.com.motorfraude.auditor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;

public final class TopicDrain {

    private static final Duration POLL = Duration.ofMillis(200);

    private TopicDrain() {
    }

    public static List<String> drain(Consumer<String, String> consumer, String topic, Duration idle, Duration timeout) {
        var partitions = consumer.partitionsFor(topic).stream().map(info -> new TopicPartition(topic, info.partition()))
                .collect(Collectors.toList());
        consumer.assign(partitions);
        consumer.seekToBeginning(partitions);
        var values = new ArrayList<String>();
        long deadline = System.nanoTime() + timeout.toNanos();
        long lastData = System.nanoTime();
        while (System.nanoTime() < deadline) {
            var records = consumer.poll(POLL);
            for (var record : records) {
                if (record.value() != null) {
                    values.add(record.value());
                }
            }
            if (!records.isEmpty()) {
                lastData = System.nanoTime();
            } else if (System.nanoTime() - lastData >= idle.toNanos() && caughtUp(consumer, partitions)) {
                break;
            }
        }
        return values;
    }

    private static boolean caughtUp(Consumer<String, String> consumer, List<TopicPartition> partitions) {
        var ends = consumer.endOffsets(partitions);
        return partitions.stream().allMatch(p -> consumer.position(p) >= ends.getOrDefault(p, 0L));
    }
}
