package dev.hugomarket.store;

public final class StatusBoard {
    private long todayNetCents;
    private boolean todayKnown;
    private long netWorthCents;
    private boolean netWorthKnown;
    private int version;

    public void setToday(long netCents) {
        if (!todayKnown || todayNetCents != netCents) {
            todayNetCents = netCents;
            todayKnown = true;
            version++;
        }
    }

    public void setNetWorth(long cents) {
        if (!netWorthKnown || netWorthCents != cents) {
            netWorthCents = cents;
            netWorthKnown = true;
            version++;
        }
    }

    public long todayNetCents() {
        return todayNetCents;
    }

    public boolean todayKnown() {
        return todayKnown;
    }

    public long netWorthCents() {
        return netWorthCents;
    }

    public boolean netWorthKnown() {
        return netWorthKnown;
    }

    public int version() {
        return version;
    }
}
