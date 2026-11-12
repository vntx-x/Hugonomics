package dev.hugomarket.module.spawners;

import dev.hugomarket.store.SpawnerPrices;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class SpawnerRows {
    record Row(String type, long unitCents) {
    }

    private SpawnerRows() {
    }

    static List<Row> build(SpawnerPrices prices) {
        List<Row> rows = new ArrayList<>(prices.unitCentsByType().size());
        for (Map.Entry<String, Long> entry : prices.unitCentsByType().entrySet()) {
            rows.add(new Row(entry.getKey(), entry.getValue()));
        }
        rows.sort(Comparator.comparingLong(Row::unitCents).reversed().thenComparing(Row::type));
        return rows;
    }
}
