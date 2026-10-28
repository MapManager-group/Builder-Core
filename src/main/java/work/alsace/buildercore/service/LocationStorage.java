package work.alsace.buildercore.service;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/** 点号为名称的一部分；写入成功后才替换原文件。 */
final class LocationStorage {
    private LocationStorage() {}

    static YamlConfiguration load(File file) {
        YamlConfiguration config = new YamlConfiguration();
        config.options().pathSeparator('\0');
        if (!file.exists()) return config;
        try {
            config.load(file);
            return config;
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("无法读取位置数据: " + file, exception);
        }
    }

    static void save(File file, YamlConfiguration config) {
        Path temporary = null;
        try {
            Path destination = file.toPath();
            Files.createDirectories(destination.getParent());
            temporary = Files.createTempFile(destination.getParent(), "locations-", ".tmp");
            Files.writeString(temporary, config.saveToString());
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("无法保存位置数据: " + file, exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }
}
