package dev.hugomarket.module.spawners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class DealEvents {
    enum Kind {
        NEW_REQUEST, COUNTER, ACCEPTED, DECLINED
    }

    record Event(Kind kind, ExchangeDeal deal) {
    }

    private final Map<String, String> seen = new HashMap<>();
    private boolean primed;

    List<Event> update(List<ExchangeDeal> deals) {
        List<Event> events = new ArrayList<>();
        Map<String, String> next = new HashMap<>();
        for (ExchangeDeal deal : deals) {
            String signature = deal.signature();
            next.put(deal.id(), signature);
            String before = seen.get(deal.id());
            if (!primed || signature.equals(before)) {
                continue;
            }
            Kind kind = kindOf(deal, before == null);
            if (kind != null) {
                events.add(new Event(kind, deal));
            }
        }
        seen.clear();
        seen.putAll(next);
        primed = true;
        return events;
    }

    void acknowledge(ExchangeDeal deal) {
        seen.put(deal.id(), deal.signature());
    }

    void reset() {
        seen.clear();
        primed = false;
    }

    private static Kind kindOf(ExchangeDeal deal, boolean fresh) {
        return switch (deal.status()) {
            case ACCEPTED -> Kind.ACCEPTED;
            case DECLINED -> Kind.DECLINED;
            case OPEN -> !deal.myTurn() ? null : fresh && deal.mineIsOffer() ? Kind.NEW_REQUEST : Kind.COUNTER;
        };
    }
}
