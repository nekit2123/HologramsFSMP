package com.fsmpgame.hologrammfsmp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HologramManager {
    private final HologramPlugin plugin;
    private final Map<String, Hologram> holograms = new LinkedHashMap<>();
    private final File dataFile;

    public HologramManager(HologramPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "holograms.yml");
    }

    public Hologram create(String id, Location loc) {
        if (holograms.containsKey(id)) return null;
        Hologram h = new Hologram(id, loc);
        h.addLine("&7New hologram: " + id);
        holograms.put(id, h);
        h.spawn();
        return h;
    }

    public boolean delete(String id) {
        Hologram h = holograms.remove(id);
        if (h == null) return false;
        h.despawn();
        return true;
    }

    public Hologram get(String id) { return holograms.get(id); }

    public Collection<Hologram> list() { return holograms.values(); }

    public void saveAll() {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        cfg.set("holograms", null);
        for (Map.Entry<String, Hologram> e : holograms.entrySet()) {
            String id = e.getKey();
            Hologram h = e.getValue();
            if (h.getLocation() != null) {
                cfg.set("holograms." + id + ".world", h.getLocation().getWorld().getName());
                cfg.set("holograms." + id + ".x", h.getLocation().getX());
                cfg.set("holograms." + id + ".y", h.getLocation().getY());
                cfg.set("holograms." + id + ".z", h.getLocation().getZ());
            }
            cfg.set("holograms." + id + ".lines", h.getLines());
            // persist image filename if set
            if (h.getImageFileName() != null) cfg.set("holograms." + id + ".image", h.getImageFileName()); else cfg.set("holograms." + id + ".image", null);
            // persist image orientation flags
            cfg.set("holograms." + id + ".imageRotationDegrees", h.getImageRotationDegrees());
            cfg.set("holograms." + id + ".imageFlipH", h.isImageFlipH());
            cfg.set("holograms." + id + ".imageFlipV", h.isImageFlipV());
        }
        try {
            cfg.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save holograms: " + ex.getMessage());
        }
    }

    public void loadAll() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        if (!cfg.isConfigurationSection("holograms")) return;
        for (String id : cfg.getConfigurationSection("holograms").getKeys(false)) {
            String path = "holograms." + id + ".";
            String world = cfg.getString(path + "world");
            double x = cfg.getDouble(path + "x");
            double y = cfg.getDouble(path + "y");
            double z = cfg.getDouble(path + "z");
            Location loc = new Location(Bukkit.getWorld(world), x, y, z);
            Hologram h = new Hologram(id, loc);
            List<String> lines = cfg.getStringList(path + "lines");
            for (String line : lines) h.addLine(line);
            // if image filename present, load image and set on hologram
            String imageName = cfg.getString(path + "image", null);
            if (imageName != null) {
                java.io.File f = new java.io.File(plugin.getDataFolder(), "images" + java.io.File.separator + imageName);
                if (f.exists()) {
                    try {
                        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(f);
                        if (img != null) h.setImage(img);
                        h.setImageFileName(imageName);
                        // load persisted orientation flags (if present)
                        int rot = cfg.getInt(path + "imageRotationDegrees", 0);
                        boolean flipH = cfg.getBoolean(path + "imageFlipH", false);
                        boolean flipV = cfg.getBoolean(path + "imageFlipV", false);
                        h.setImageRotationDegrees(rot);
                        h.setImageFlipH(flipH);
                        h.setImageFlipV(flipV);
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Failed to load image for hologram " + id + ": " + ex.getMessage());
                    }
                } else {
                    plugin.getLogger().warning("Image file for hologram " + id + " not found: " + imageName);
                }
            }
            holograms.put(id, h);
            // spawn image with configured chunking and overall height
            int maxPerLine = plugin.getConfig().getInt("max-stands-per-line", 64);
            int chunk = 1;
            if (h.getImageFileName() != null && h.getImage() != null) {
                int w = h.getImage().getWidth();
                chunk = Math.max(1, (int)Math.ceil((double)w / (double)maxPerLine));
                double overallHeight = plugin.getConfig().getDouble("image-overall-height", 4.0);
                if (!h.getLines().isEmpty()) {
                    overallHeight = h.getLines().size() * plugin.getConfig().getDouble("line-spacing", 0.25);
                }
                // Ensure minimum per-pixel block size so pixels are visible
                double minPixel = plugin.getConfig().getDouble("min-pixel-block-size", 0.12);
                overallHeight = Math.max(overallHeight, minPixel * h.getImage().getHeight());
                h.spawnImage(chunk, overallHeight);
            } else {
                // text-mode spawn
                h.spawn();
            }
        }
    }
}
