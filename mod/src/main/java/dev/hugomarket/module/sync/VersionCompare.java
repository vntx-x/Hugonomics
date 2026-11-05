package dev.hugomarket.module.sync;

public final class VersionCompare {
    private VersionCompare() {
    }

    public static boolean atLeast(String version, String minimum) {
        int[] a = parse(version);
        int[] b = parse(minimum);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) {
                return x > y;
            }
        }
        return true;
    }

    private static int[] parse(String version) {
        String core = version.split("[+-]")[0];
        String[] parts = core.split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                numbers[i] = Integer.parseInt(parts[i].replaceAll("\\D", ""));
            } catch (NumberFormatException e) {
                numbers[i] = 0;
            }
        }
        return numbers;
    }
}
