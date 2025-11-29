package com.fsmpgame.hologrammfsmp;

import org.bukkit.plugin.java.JavaPlugin;
import com.fsmpgame.hologrammfsmp.commands.HDCommand;

public final class HologramPlugin extends JavaPlugin {
    private HologramManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.manager = new HologramManager(this);
        this.manager.loadAll();

        // register command + tab completer
        HDCommand hdCmd = new HDCommand(this);
        if (this.getCommand("hd") != null) {
            this.getCommand("hd").setExecutor(hdCmd);
            this.getCommand("hd").setTabCompleter(hdCmd);
        }

        // ensure images folder exists
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        java.io.File images = new java.io.File(getDataFolder(), "images");
        if (!images.exists()) images.mkdirs();
    }

    @Override
    public void onDisable() {
        if (this.manager != null) this.manager.saveAll();
    }

    public HologramManager getManager() {
        return manager;
    }
}
