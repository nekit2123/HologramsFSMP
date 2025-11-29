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
        despawn();
        Location base = getLocation();
        if (base == null) return;
        if (image != null) {
            // image mode: spawn armor stands in grid, grouping horizontally by chunkSize
            int w = image.getWidth();
            int h = image.getHeight();
            if (chunkSize < 1) chunkSize = 1;
            // spacing: approximate character width/height
            double xSpacing = 0.12 * chunkSize; // spacing multiplied by chunk size
            double ySpacing = 0.12;
            // center horizontally
            double xOffsetStart = - ( (w / (double)chunkSize) - 1 ) * xSpacing / 2.0;
            double yOffsetStart = 0; // start at base y and go down

            // For each row, group pixels horizontally by chunkSize
            for (int row = 0; row < h; row++) {
                for (int colGroup = 0; colGroup < w; colGroup += chunkSize) {
                    StringBuilder sb = new StringBuilder();
                    int groupEnd = Math.min(colGroup + chunkSize, w);
                    for (int px = colGroup; px < groupEnd; px++) {
                        int rgb = image.getRGB(px, row);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = (rgb) & 0xFF;
                        String hex = String.format("%02x%02x%02x", r, g, b);
                        String colorCode = toSectionHex(hex);
                        sb.append(colorCode).append('\u2588');
                    }

                    Location loc = base.clone().add(xOffsetStart + (colGroup / (double)chunkSize) * xSpacing, yOffsetStart + row * ySpacing, 0);
                    ArmorStand as = (ArmorStand) base.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
                    as.setInvisible(true);
                    as.setMarker(true);
                    as.setGravity(false);
                    as.setCustomNameVisible(true);
                    String text = sb.toString();
                    try { as.setCustomName(text); } catch (NoSuchMethodError e) { as.setCustomName(text); }
                    spawned.add(as);
                }
            }
            return;
        }

        // default text mode: each line on separate armor stand vertically
        double offset = 0.25;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Location loc = base.clone().add(0, offset * i, 0);
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
