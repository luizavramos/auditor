package br.com.motorfraude.auditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public record AuditReport(int accepted, int decided, int extras, List<String> missing, List<String> duplicatedDecisions,
        List<String> duplicatedAlerts, List<String> alertsMissing) {

    public record Decision(String transactionId, String severity) {
    }

    public record Alert(String transactionId, String alertId) {
    }

    private static final int LISTED = 10;

    public static AuditReport compute(Set<String> accepted, List<Decision> decisions, List<Alert> alerts) {
        var decisionsPerTx = new HashMap<String, Integer>();
        var severityPerTx = new HashMap<String, String>();
        for (var decision : decisions) {
            decisionsPerTx.merge(decision.transactionId(), 1, Integer::sum);
            severityPerTx.put(decision.transactionId(), decision.severity());
        }
        var alertsPerTx = new HashMap<String, Integer>();
        for (var alert : alerts) {
            alertsPerTx.merge(alert.transactionId(), 1, Integer::sum);
        }

        var missing = new TreeSet<String>();
        var decidedAccepted = new HashSet<String>();
        for (var id : accepted) {
            if (decisionsPerTx.containsKey(id)) {
                decidedAccepted.add(id);
            } else {
                missing.add(id);
            }
        }
        var duplicatedDecisions = new TreeSet<String>();
        decisionsPerTx.forEach((id, count) -> {
            if (count > 1) {
                duplicatedDecisions.add(id);
            }
        });
        var duplicatedAlerts = new TreeSet<String>();
        alertsPerTx.forEach((id, count) -> {
            if (count > 1) {
                duplicatedAlerts.add(id);
            }
        });
        var alertsMissing = new TreeSet<String>();
        severityPerTx.forEach((id, severity) -> {
            if (!"NONE".equals(severity) && !alertsPerTx.containsKey(id)) {
                alertsMissing.add(id);
            }
        });
        int extras = (int) decisionsPerTx.keySet().stream().filter(id -> !accepted.contains(id)).count();
        return new AuditReport(accepted.size(), decidedAccepted.size(), extras, List.copyOf(missing), List.copyOf(duplicatedDecisions),
                List.copyOf(duplicatedAlerts), List.copyOf(alertsMissing));
    }

    public boolean ok() {
        return missing.isEmpty() && duplicatedDecisions.isEmpty() && duplicatedAlerts.isEmpty() && alertsMissing.isEmpty();
    }

    public String summary() {
        var text = new StringBuilder();
        text.append("aceitas: ").append(accepted).append('\n');
        text.append("com decisão: ").append(decided).append('\n');
        text.append("faltantes: ").append(missing.size()).append(sample(missing)).append('\n');
        text.append("decisões duplicadas: ").append(duplicatedDecisions.size()).append(sample(duplicatedDecisions)).append('\n');
        text.append("alertas duplicados: ").append(duplicatedAlerts.size()).append(sample(duplicatedAlerts)).append('\n');
        text.append("MEDIUM/HIGH sem alerta: ").append(alertsMissing.size()).append(sample(alertsMissing)).append('\n');
        text.append("decisões fora do log de aceitas (informativo): ").append(extras).append('\n');
        text.append(ok() ? "APROVADO" : "REPROVADO");
        return text.toString();
    }

    private static String sample(List<String> ids) {
        if (ids.isEmpty()) {
            return "";
        }
        var shown = new ArrayList<>(ids.subList(0, Math.min(LISTED, ids.size())));
        return " " + shown + (ids.size() > LISTED ? " ..." : "");
    }
}
