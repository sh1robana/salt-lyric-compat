package local.salt.lyriccompat;

import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;
import com.xuncorp.spw.workshop.api.PluginContext;
import com.xuncorp.spw.workshop.api.Channel;
import com.xuncorp.spw.workshop.api.SpwPlugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.zip.ZipFile;
import java.net.URLClassLoader;

public final class CompatTests {
    private static int assertions;
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
    private static void same(String input, double duration, String reason) {
        var result = ColonTimestampNormalizer.normalize(input, duration);
        check(result.replacements() == 0 && result.text().equals(input), reason);
    }
    private static byte[] little(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }
    private static byte[] flac(String lyric) throws Exception {
        byte[] stream = new byte[34];
        long packed = ((long) 44100 << 44) | ((long) 1 << 41) | ((long) 15 << 36) | 44100L * 200;
        ByteBuffer.wrap(stream, 10, 8).putLong(packed);
        byte[] field = ("LYRICS=" + lyric).getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream comments = new ByteArrayOutputStream();
        comments.write(little(0)); comments.write(little(1));
        comments.write(little(field.length)); comments.write(field);
        byte[] vorbis = comments.toByteArray();
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        file.write("fLaC".getBytes(StandardCharsets.US_ASCII));
        file.write(new byte[]{0,0,0,34}); file.write(stream);
        file.write(new byte[]{(byte) 132, (byte)(vorbis.length >> 16), (byte)(vorbis.length >> 8), (byte)vorbis.length});
        file.write(vorbis);
        return file.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        String malformed = "[00:00.00]title\r\n[00:20:60]日本語\r\n[01:02:061]中文\r\n";
        var result = ColonTimestampNormalizer.normalize(malformed, 200);
        check(result.replacements() == 2, "two/three fractional digits");
        check(result.text().equals("[00:00.00]title\r\n[00:20.60]日本語\r\n[01:02.061]中文\r\n"), "exact text and newline preservation");
        same(result.text(), 200, "idempotent");
        same("[00:00.599]normal\n[00:03.467]line", 200, "valid LRC unchanged");
        same("[00:00:20]ambiguous\n[00:01:30]could be hours", 200, "real hh:mm:ss retained");
        same("[01:02:03]hour track", 4000, "long-track hours retained");
        same("[00:00:00]instrumental", 200, "zero-only ambiguous retained");
        same("[09:30:80]past end", 200, "outside duration retained");
        same("[00:20:60]unknown", 0, "unknown duration retained");
        same("text [00:20:60]literal\n[00:99:80]invalid", 200, "body and invalid seconds retained");
        String multiple = "\ufeff [00:20:60][00:22:30]repeat\n[00:01.000]text <00:20:60>word";
        var multi = ColonTimestampNormalizer.normalize(multiple, 200);
        check(multi.replacements() == 2 && multi.text().contains("<00:20:60>"), "multi tags and word text");
        var lowFractions = ColonTimestampNormalizer.normalize("[00:20:30]fraction below 60", 200);
        check(lowFractions.replacements() == 1, "duration disambiguates short track");

        Path dir = Files.createTempDirectory("salt-lyric-compat-test-");
        try {
            Path audio = dir.resolve("test.flac");
            byte[] original = flac(malformed);
            Files.write(audio, original);
            var read = FlacLyricsReader.read(audio);
            check(read.text().equals(malformed) && read.durationSeconds() == 200, "FLAC metadata decoding");
            var extension = new LyricCompatExtension();
            var item = new PlaybackExtensionPoint.MediaItem("", "", "", "", audio.toString());
            check(extension.onBeforeLoadLyrics(item).equals(result.text()), "host callback repaired");
            check(java.util.Arrays.equals(original, Files.readAllBytes(audio)), "original file byte-identical");
            Path sidecar = dir.resolve("test.lrc");
            Files.writeString(sidecar, "[00:01.00]explicit sidecar");
            check(extension.onBeforeLoadLyrics(item) == null, "sidecar preserves host priority");
            Files.delete(sidecar);
            Files.write(audio, new byte[]{1,2,3});
            check(extension.onBeforeLoadLyrics(item) == null, "corrupt file falls back");
            Files.delete(audio);
            check(extension.onBeforeLoadLyrics(item) == null, "missing file falls back");
            check(extension.onBeforeLoadLyrics(null) == null, "null input falls back");
        } finally {
            try (var files = Files.walk(dir)) {
                for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        if (args.length > 0) {
            int scanned = 0, fixed = 0, stamps = 0, ambiguous = 0;
            for (String path : Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8)) {
                if (path.isBlank()) continue;
                var read = FlacLyricsReader.read(Path.of(path));
                var converted = ColonTimestampNormalizer.normalize(read.text(), read.durationSeconds());
                var item = new PlaybackExtensionPoint.MediaItem("", "", "", "", path);
                String callback = new LyricCompatExtension().onBeforeLoadLyrics(item);
                check((converted.replacements() > 0 && converted.text().equals(callback))
                        || (converted.replacements() == 0 && callback == null), "corpus callback agreement #" + scanned);
                scanned++;
                if (converted.replacements() > 0) { fixed++; stamps += converted.replacements(); }
                if (converted.reason().equals("ambiguous-hours")) ambiguous++;
            }
            System.out.printf("Corpus: scanned=%d, repaired=%d, timestamps=%d, ambiguous=%d%n", scanned, fixed, stamps, ambiguous);
        }
        System.out.println("Passed " + assertions + " assertions");
    }
}
