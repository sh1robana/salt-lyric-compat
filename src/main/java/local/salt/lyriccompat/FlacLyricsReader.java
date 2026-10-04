package local.salt.lyriccompat;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** 仅读取 FLAC 的时长信息和 Vorbis 标签，始终以只读方式打开文件。 */
public final class FlacLyricsReader {
    private static final long MAX_METADATA = 64L * 1024 * 1024;
    private static final int MAX_COMMENTS = 100_000;
    private static final int MAX_LYRIC_CHARS = 2 * 1024 * 1024;
    public record Lyrics(String text, double durationSeconds) {}

    private FlacLyricsReader() {}

    public static Lyrics read(Path path) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            if (file.readInt() != 0x664c6143) throw new IOException("Not a native FLAC file");
            double duration = 0;
            String lyrics = null;
            long total = 0;
            boolean last;
            do {
                int header = file.readUnsignedByte();
                last = (header & 128) != 0;
                int type = header & 127;
                int length = (file.readUnsignedByte() << 16)
                        | (file.readUnsignedByte() << 8) | file.readUnsignedByte();
                total += 4L + length;
                if (total > MAX_METADATA || length > file.length() - file.getFilePointer())
                    throw new IOException("Invalid or oversized FLAC metadata");
                if (type == 0) {
                    if (length != 34) throw new IOException("Invalid STREAMINFO");
                    byte[] stream = new byte[34];
                    file.readFully(stream);
                    long packed = ByteBuffer.wrap(stream, 10, 8).getLong();
                    int rate = (int) ((packed >>> 44) & 0xfffff);
                    long samples = packed & 0xfffffffffL;
                    if (rate > 0) duration = samples / (double) rate;
                } else if (type == 4 && lyrics == null) {
                    byte[] comments = new byte[length];
                    file.readFully(comments);
                    lyrics = parseComments(comments);
                } else {
                    file.seek(file.getFilePointer() + length);
                }
            } while (!last);
            return new Lyrics(lyrics, duration);
        }
    }

    private static String parseComments(byte[] bytes) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int vendorLength = length(buffer);
        buffer.position(buffer.position() + vendorLength);
        if (buffer.remaining() < 4) throw new IOException("Truncated comment count");
        long count = Integer.toUnsignedLong(buffer.getInt());
        if (count > MAX_COMMENTS) throw new IOException("Too many comments");
        String fallback = null;
        String primary = null;
        for (long i = 0; i < count; i++) {
            int size = length(buffer);
            ByteBuffer field = buffer.slice();
            field.limit(size);
            buffer.position(buffer.position() + size);
            String comment;
            try {
                comment = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(field).toString();
            } catch (CharacterCodingException error) {
                throw new IOException("Invalid UTF-8 comment", error);
            }
            int equals = comment.indexOf('=');
            if (equals < 0) continue;
            String name = comment.substring(0, equals);
            if (name.equalsIgnoreCase("LYRICS") && primary == null)
                primary = comment.substring(equals + 1);
            else if (name.equalsIgnoreCase("UNSYNCEDLYRICS") && fallback == null)
                fallback = comment.substring(equals + 1);
        }
        String result = primary != null ? primary : fallback;
        if (result != null && result.length() > MAX_LYRIC_CHARS)
            throw new IOException("Lyrics too large");
        return result;
    }

    private static int length(ByteBuffer buffer) throws IOException {
        if (buffer.remaining() < 4) throw new IOException("Truncated comment length");
        long length = Integer.toUnsignedLong(buffer.getInt());
        if (length > buffer.remaining()) throw new IOException("Truncated comment value");
        return (int) length;
    }
}
