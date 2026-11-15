package br.com.motorfraude.auditor;

import br.com.motorfraude.contracts.FraudAlert;
import br.com.motorfraude.contracts.FraudDecision;
import br.com.motorfraude.contracts.JsonCodec;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Cli.Config config;
        try {
            if (args.length == 1 && "--help".equals(args[0])) {
                System.out.println(Cli.HELP);
                return;
            }
            config = Cli.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("erro: " + e.getMessage() + "\n\n" + Cli.HELP);
            System.exit(2);
            return;
        }
        var accepted = AcceptedLog.read(config.acceptedLog());
        System.out.println("lendo " + config.decisionsTopic() + " e " + config.alertsTopic() + " (read_committed)...");
        var decisions = drain(config, config.decisionsTopic()).stream().map(Main::decision).toList();
        var alerts = drain(config, config.alertsTopic()).stream().map(Main::alert).toList();
        var report = AuditReport.compute(accepted, decisions, alerts);
        System.out.println(report.summary());
        System.exit(report.ok() ? 0 : 1);
    }

    private static List<String> drain(Cli.Config config, String topic) {
        try (var consumer = new KafkaConsumer<String, String>(Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers(),
                ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            return TopicDrain.drain(consumer, topic, config.idle(), config.timeout());
        }
    }

    private static AuditReport.Decision decision(String json) {
        var decision = JsonCodec.fromJson(json, FraudDecision.class);
        return new AuditReport.Decision(decision.transactionId(), decision.severity().name());
    }

    private static AuditReport.Alert alert(String json) {
        var alert = JsonCodec.fromJson(json, FraudAlert.class);
        return new AuditReport.Alert(alert.transactionId(), alert.alertId());
    }
}
