package local.salt.lyriccompat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative, line-prefix-only conversion; no timing or lyric text changes. */
public final class ColonTimestampNormalizer {
    private static final Pattern PREFIX = Pattern.compile(
            "(?m)^[\\uFEFF \\t]*(?:\\[\\d{2}:[0-5]\\d[.:]\\d{2,3}\\])+"
    );
    private static final Pattern BAD = Pattern.compile("\\[(\\d{2}):([0-5]\\d):(\\d{2,3})\\]");

    public record Result(String text, int replacements, String reason) {}
    private record Edit(int index) {}

    private ColonTimestampNormalizer() {}

    public static Result normalize(String text, double durationSeconds) {
        if (text == null || text.isEmpty()) return new Result(text, 0, "no-lyrics");
        if (!Double.isFinite(durationSeconds) || durationSeconds <= 0)
            return new Result(text, 0, "unknown-duration");
        List<Edit> edits = new ArrayList<>();
        boolean unambiguous = false;
        Matcher prefixes = PREFIX.matcher(text);
        while (prefixes.find()) {
            Matcher bad = BAD.matcher(text).region(prefixes.start(), prefixes.end());
            while (bad.find()) {
                int minutes = Integer.parseInt(bad.group(1));
                int seconds = Integer.parseInt(bad.group(2));
                String fraction = bad.group(3);
                int value = Integer.parseInt(fraction);
                double repaired = minutes * 60 + seconds + value / Math.pow(10, fraction.length());
                if (repaired > durationSeconds + 5)
                    return new Result(text, 0, "outside-track-duration");
                // Real hh:mm:ss stays intact if it could fit within this track.
                double asHours = minutes * 3600.0 + seconds * 60.0 + value;
                if (fraction.length() == 3 || value > 59 || asHours > durationSeconds + 5)
                    unambiguous = true;
                edits.add(new Edit(bad.start() + 6));
            }
        }
        if (edits.isEmpty()) return new Result(text, 0, "unchanged");
        if (!unambiguous) return new Result(text, 0, "ambiguous-hours");
        char[] fixed = text.toCharArray();
        for (Edit edit : edits) fixed[edit.index()] = '.';
        return new Result(new String(fixed), edits.size(), "converted");
    }
}
