package com.fsmpgame.hologrammfsmp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Hologram {
    private final String id;
    private String worldName;
    private double x, y, z;
    private final List<String> lines = new ArrayList<>();

    // runtime
    private final List<ArmorStand> spawned = new ArrayList<>();

    // optional image mode (transient runtime only)
    private transient BufferedImage image = null;
    // stored image filename (persisted)
    private String imageFileName = null;

    public Hologram(String id, Location loc) {
        this.id = id;
        setLocation(loc);
    }

    public String getId() { return id; }

    public void setLocation(Location loc) {
        if (loc.getWorld() != null) this.worldName = loc.getWorld().getName();
        this.x = loc.getX(); this.y = loc.getY(); this.z = loc.getZ();
    }

    public Location getLocation() {
        World w = Bukkit.getWorld(worldName);
        if (w == null) return null;
        return new Location(w, x, y, z);
    }

    public List<String> getLines() { return lines; }

    public void addLine(String line) { lines.add(line); }

    public void setLine(int index, String line) {
        if (index < 0 || index >= lines.size()) return;
        lines.set(index, line);
    }

    public void removeLine(int index) {
        if (index < 0 || index >= lines.size()) return;
        lines.remove(index);
    }

    public void setImage(BufferedImage img) {
        this.image = img;
    }

    public BufferedImage getImage() { return image; }

    public void setImageFileName(String fileName) { this.imageFileName = fileName; }
    public String getImageFileName() { return imageFileName; }

    /**
     * Spawn with default chunk size (1)
     */
    public void spawn() { spawn(1); }

    /**
     * Spawn with specified horizontal chunk size: how many pixels are combined into one ArmorStand horizontally.
     */
    public void spawn(int chunkSize) {
        // Deprecated: keep for backward compatibility (calls spawnImage when image present)
        if (image != null) {
            // compute a reasonable overall height (default 4.0 blocks)
            double overallHeight = 4.0;
            if (!lines.isEmpty()) {
                overallHeight = lines.size() * 0.25;
            }
            spawnImage(chunkSize, overallHeight);
            return;
        }
        // default text mode: each line on separate armor stand vertically
        despawn();
        Location base = getLocation();
        if (base == null) return;
        double offset = 0.25;
        // place line 0 at the top and subsequent lines below it
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Location loc = base.clone().add(0, -offset * i, 0);
            ArmorStand as = (ArmorStand) base.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            as.setInvisible(true);
            as.setMarker(true);
            as.setGravity(false);
            as.setCustomNameVisible(true);
            try {
                as.setCustomName(line);
            } catch (NoSuchMethodError e) {
                as.setCustomName(line);
            }
            spawned.add(as);
        }
    }

    /**
     * Spawn image using automatic spacing. chunkSize = number of horizontal pixels grouped
     * into a single armor stand. overallHeight is total height (in blocks) that the image should occupy.
     */
    public void spawnImage(int chunkSize, double overallHeight) {
        despawn();
        Location base = getLocation();
        if (base == null) return;
        if (image == null) return;

        int w = image.getWidth();
        int h = image.getHeight();
        if (chunkSize < 1) chunkSize = 1;

        // Number of columns after grouping
        int cols = (w + chunkSize - 1) / chunkSize;

        // per-pixel spacing to fit the image into overallHeight
        double perPixel = overallHeight / (double) h;
        // make horizontal spacing proportional and square
        double xSpacing = perPixel * chunkSize;
        double ySpacing = perPixel;

        // center offsets
        double xOffsetStart = -((cols - 1) * xSpacing) / 2.0;
        // place top row at base Y and go downward (row 0 = topmost)
        double yStart = 0.0; // base location is the top edge

        for (int row = 0; row < h; row++) {
            // compute Y: row 0 at top, increasing rows go downwards
            double y = yStart - row * ySpacing;
            for (int colGroup = 0; colGroup < w; colGroup += chunkSize) {
                // average color for the group
                int groupEnd = Math.min(colGroup + chunkSize, w);
                long rSum = 0, gSum = 0, bSum = 0, aSum = 0;
                int count = 0;
                for (int px = colGroup; px < groupEnd; px++) {
                    int rgb = image.getRGB(px, row);
                    int a = (rgb >> 24) & 0xFF;
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = (rgb) & 0xFF;
                    aSum += a; rSum += r; gSum += g; bSum += b; count++;
                }
                if (count == 0) continue;
                int aAvg = (int)(aSum / count);
                // skip mostly-transparent groups
                if (aAvg < 16) continue;
                int rAvg = (int)(rSum / count);
                int gAvg = (int)(gSum / count);
                int bAvg = (int)(bSum / count);
                String hex = String.format("%02x%02x%02x", rAvg, gAvg, bAvg);
                String colorCode = toSectionHex(hex);

                int groupIndex = colGroup / chunkSize;
                double x = xOffsetStart + groupIndex * xSpacing;
                Location loc = base.clone().add(x, y, 0);

                ArmorStand as = (ArmorStand) base.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
                as.setInvisible(true);
                as.setMarker(true);
                as.setGravity(false);
                as.setCustomNameVisible(true);
                String text = colorCode + '\u2588';
                try { as.setCustomName(text); } catch (NoSuchMethodError e) { as.setCustomName(text); }
                spawned.add(as);
            }
        }
    }

    private String toSectionHex(String hex) {
        StringBuilder s = new StringBuilder();
        char section = '\u00A7';
        s.append(section).append('x');
        for (char c : hex.toCharArray()) { s.append(section).append(c); }
        return s.toString();
    }

    public void despawn() {
        for (ArmorStand as : spawned) {
            if (!as.isDead()) as.remove();
        }
        spawned.clear();
    }
}
