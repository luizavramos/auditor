package br.com.motorfraude.auditor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TopicDrainTest {

    private static final String TOPIC = "fraud-decisions.v1";

    @Test
    @DisplayName("lê o tópico inteiro, de todas as partições, até alcançar o fim e ficar ocioso")
    void leTudo() {
        var consumer = new MockConsumer<String, String>("earliest");
        var p0 = new TopicPartition(TOPIC, 0);
        var p1 = new TopicPartition(TOPIC, 1);
        consumer.updateBeginningOffsets(Map.of(p0, 0L, p1, 0L));
        consumer.updateEndOffsets(Map.of(p0, 2L, p1, 1L));
        consumer.updatePartitions(TOPIC, java.util.List.of(new org.apache.kafka.common.PartitionInfo(TOPIC, 0, null, null, null),
                new org.apache.kafka.common.PartitionInfo(TOPIC, 1, null, null, null)));
        consumer.assign(java.util.List.of(p0, p1));
        consumer.addRecord(new ConsumerRecord<>(TOPIC, 0, 0, "k", "a"));
        consumer.addRecord(new ConsumerRecord<>(TOPIC, 0, 1, "k", "b"));
        consumer.addRecord(new ConsumerRecord<>(TOPIC, 1, 0, "k", "c"));

        var values = TopicDrain.drain(consumer, TOPIC, Duration.ofMillis(100), Duration.ofSeconds(5));

        assertThat(values).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    @DisplayName("tópico vazio devolve lista vazia sem esperar o prazo todo")
    void vazio() {
        var consumer = new MockConsumer<String, String>("earliest");
        var p0 = new TopicPartition(TOPIC, 0);
        consumer.updateBeginningOffsets(Map.of(p0, 0L));
        consumer.updateEndOffsets(Map.of(p0, 0L));
        consumer.updatePartitions(TOPIC, java.util.List.of(new org.apache.kafka.common.PartitionInfo(TOPIC, 0, null, null, null)));

        long start = System.nanoTime();
        var values = TopicDrain.drain(consumer, TOPIC, Duration.ofMillis(100), Duration.ofSeconds(30));

        assertThat(values).isEmpty();
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(10));
    }
}
