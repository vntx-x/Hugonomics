package dev.hugomarket.store;

public final class FlipMath {
    public record Flip(long buyUnitCents, long sellUnitCents, long marginUnitCents, long units, long totalMarginCents) {
        public double marginPercent() {
            return buyUnitCents == 0 ? 0 : marginUnitCents * 100.0 / buyUnitCents;
        }
    }

    private FlipMath() {
    }

    public static Flip ahToOrder(PriceStats ahStats, OrderBook.BestOrder order, int maxUnits) {
        if (ahStats == null || order == null) {
            return null;
        }
        long units = Math.min(order.openUnits(), Math.max(1, maxUnits));
        long margin = order.unitPriceCents() - ahStats.lowestUnitCents();
        return new Flip(ahStats.lowestUnitCents(), order.unitPriceCents(), margin, units, margin * units);
    }

    public static Flip orderToAh(OrderBook.BestOrder order, PriceStats ahStats, Fees fees, int units) {
        if (ahStats == null || order == null) {
            return null;
        }
        long sellStack = ahStats.lowestUnitCents() * units;
        long net = fees.netAfterSale(sellStack) - fees.listingFee(sellStack);
        long netUnit = Math.floorDiv(net, units);
        long margin = netUnit - order.unitPriceCents();
        return new Flip(order.unitPriceCents(), netUnit, margin, units, margin * units);
    }
}
