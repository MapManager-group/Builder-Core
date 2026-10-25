package work.alsace.buildercore.features.cushiontools;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class CushionSession {
    DyeColor color = DyeColor.WHITE;
    float yaw;
    Location first, second;
    Entity dragging;
    Location dragOrigin;
    double dragDistance;
    int lastTick = -1;
    Plan pending;
    boolean running;
    boolean recovering;

    record Plan(UUID world, List<Location> placements, List<UUID> removals, DyeColor color, float yaw) {
        Plan {
            placements = List.copyOf(placements);
            removals = List.copyOf(removals);
        }
        int size() { return placements.size() + removals.size(); }
    }

    static final double STEP = 1.0 / 32.0;
    static final int LIMIT = 1024;
    static final int GROUND_RANGE = 8;

    static List<Integer> groundLevels(int reference, int min, int max) {
        List<Integer> levels = new ArrayList<>();
        for (int distance = 0; distance <= GROUND_RANGE; distance++) {
            int lower = reference - distance, upper = reference + distance;
            if (lower >= min && lower < max) levels.add(lower);
            if (distance != 0 && upper >= min && upper < max) levels.add(upper);
        }
        return levels;
    }

    static boolean withinSelection(Location point, Location first, Location second) {
        return point.getWorld().equals(first.getWorld()) && first.getWorld().equals(second.getWorld())
                && point.getX() >= Math.min(first.getBlockX(), second.getBlockX())
                && point.getX() < Math.max(first.getBlockX(), second.getBlockX()) + 1.0
                && point.getY() >= Math.min(first.getBlockY(), second.getBlockY())
                && point.getY() < Math.max(first.getBlockY(), second.getBlockY()) + 2.0
                && point.getZ() >= Math.min(first.getBlockZ(), second.getBlockZ())
                && point.getZ() < Math.max(first.getBlockZ(), second.getBlockZ()) + 1.0;
    }

    static long area(Location first, Location second) {
        return (Math.abs((long) first.getBlockX() - second.getBlockX()) + 1)
                * (Math.abs((long) first.getBlockZ() - second.getBlockZ()) + 1);
    }
}
