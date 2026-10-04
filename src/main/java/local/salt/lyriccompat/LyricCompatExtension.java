package local.salt.lyriccompat;

import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;
import org.pf4j.Extension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Extension
public final class LyricCompatExtension implements PlaybackExtensionPoint {
    @Override
    public String onBeforeLoadLyrics(MediaItem mediaItem) {
        if (mediaItem == null) return null;
        try {
            Path path = Path.of(mediaItem.getPath());
            String name = path.getFileName().toString();
            if (!name.toLowerCase(Locale.ROOT).endsWith(".flac") || !Files.isRegularFile(path))
                return null;
            // An explicit sidecar is user intent; let the host choose and load it.
            String stem = name.substring(0, name.length() - 5);
            for (String suffix : new String[]{".lrc", ".ttml", ".srt", ".qrc", ".yrc"}) {
                if (Files.exists(path.resolveSibling(stem + suffix))
                        || Files.exists(path.resolveSibling(name + suffix))) return null;
            }
            FlacLyricsReader.Lyrics lyrics = FlacLyricsReader.read(path);
            var result = ColonTimestampNormalizer.normalize(lyrics.text(), lyrics.durationSeconds());
            return result.replacements() > 0 ? result.text() : null;
        } catch (Exception | LinkageError error) {
            // File failures never prevent playback or the host's default lyric handling.
            return null;
        }
    }
}
