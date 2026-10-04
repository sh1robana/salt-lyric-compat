package local.salt.lyriccompat;

import com.xuncorp.spw.workshop.api.Channel;
import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;
import com.xuncorp.spw.workshop.api.PluginContext;
import org.pf4j.DefaultPluginManager;
import org.pf4j.Plugin;
import org.pf4j.PluginFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipFile;

/** Tests the distributed archive, not the classes on the build classpath. */
public final class PackagingTests {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("salt-compat-packaging-");
        Path unpacked = root.resolve("lyriccompat");
        DefaultPluginManager manager = new DefaultPluginManager(root) {
            @Override protected PluginFactory createPluginFactory() {
                return wrapper -> {
                    try {
                        var cls = wrapper.getPluginClassLoader().loadClass(wrapper.getDescriptor().getPluginClass());
                        var context = new PluginContext(wrapper.getPluginId(), wrapper.getDescriptor().getVersion(),
                                wrapper.getPluginPath().toString(), "1.18.5", Channel.Steam);
                        return (Plugin) cls.getConstructor(PluginContext.class).newInstance(context);
                    } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
                };
            }
        };
        try {
            try (ZipFile zip = new ZipFile(args[0])) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    Path destination = unpacked.resolve(entry.getName()).normalize();
                    if (!destination.startsWith(unpacked)) throw new AssertionError("Unsafe archive entry");
                    if (entry.isDirectory()) { Files.createDirectories(destination); continue; }
                    Files.createDirectories(destination.getParent());
                    try (var input = zip.getInputStream(entry)) { Files.copy(input, destination); }
                }
            }
            String id = manager.loadPlugin(unpacked);
            if (!"local.salt.lyriccompat".equals(id)) throw new AssertionError("Wrong plugin ID");
            var descriptor = manager.getPlugin(id).getDescriptor();
            if (descriptor.getProvider() == null || descriptor.getPluginDescription() == null)
                throw new AssertionError("1.18.5 requires provider and description");
            manager.startPlugin(id);
            var extensions = manager.getExtensions(PlaybackExtensionPoint.class);
            if (extensions.size() != 1) throw new AssertionError("Expected exactly one registered extension");
            var item = new PlaybackExtensionPoint.MediaItem("", "", "", "", root.resolve("missing.flac").toString());
            if (extensions.getFirst().onBeforeLoadLyrics(item) != null)
                throw new AssertionError("Missing file must fall back");
            manager.stopPlugin(id);
            manager.unloadPlugin(id);
            System.out.println("Packaging: descriptor, plugin constructor, extension discovery, callback and stop/unload passed");
        } finally {
            try (var files = Files.walk(root)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
}
