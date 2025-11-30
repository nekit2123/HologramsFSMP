package com.fsmpgame.hologrammfsmp.commands;

import com.fsmpgame.hologrammfsmp.Hologram;
import com.fsmpgame.hologrammfsmp.HologramManager;
import com.fsmpgame.hologrammfsmp.HologramPlugin;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HDCommand implements CommandExecutor, TabCompleter {
    private final HologramPlugin plugin;

    public HDCommand(HologramPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        HologramManager mgr = plugin.getManager();
        String sub = args[0].toLowerCase();

        try {
            switch (sub) {
                case "create":
                    if (!(sender instanceof Player)) { sender.sendMessage("Только игрок может создавать голограммы"); return true; }
                    if (args.length < 2) { sender.sendMessage("Использование: /hd create <название>"); return true; }
                    Player p = (Player) sender;
                    String id = args[1];
                    Hologram created = mgr.create(id, p.getLocation());
                    if (created == null) {
                        sender.sendMessage(ChatColor.RED + "Голограмма с таким именем уже существует");
                    } else {
                        // replace default first line with friendly text
                        created.getLines().clear();
                        created.addLine(ChatColor.translateAlternateColorCodes('&', "&fХоло " + id + " - &7Первая линия"));
                        created.spawn();
                        sender.sendMessage(ChatColor.GREEN + "Создана голограмма: " + ChatColor.YELLOW + id + ChatColor.GRAY + " — Первая линия добавлена");
                    }
                    return true;

                case "delete":
                    if (args.length < 2) { sender.sendMessage("Usage: /hd delete <id>"); return true; }
                    if (mgr.delete(args[1])) sender.sendMessage("Deleted " + args[1]); else sender.sendMessage("Not found");
                    return true;
                    case "yaw":
                    case "y":
                        // /hd yaw <id> <deg|+rel|-rel>
                        if (args.length < 3) { sender.sendMessage("Usage: /hd yaw <id> <deg|+rel|-rel>"); return true; }
                        Hologram hy = mgr.get(args[1]);
                        if (hy == null) { sender.sendMessage(ChatColor.RED + "Hologram not found"); return true; }
                        String degStr = args[2];
                        double yawVal;
                        try {
                            if (degStr.startsWith("+") || degStr.startsWith("-")) {
                                double delta = Double.parseDouble(degStr);
                                yawVal = hy.getImageYawDegrees() + delta;
                            } else {
                                yawVal = Double.parseDouble(degStr);
                            }
                        } catch (NumberFormatException nfe) { sender.sendMessage("Invalid yaw value"); return true; }
                        hy.setImageYawDegrees(yawVal);
                        hy.despawn();
                        if (hy.getImage() != null) {
                            int maxPerLine = plugin.getConfig().getInt("max-stands-per-line", 64);
                            int chunk = Math.max(1, (int)Math.ceil((double)hy.getImage().getWidth() / (double)maxPerLine));
                            double overallHeight = plugin.getConfig().getDouble("image-overall-height", 4.0);
                            if (!hy.getLines().isEmpty()) overallHeight = hy.getLines().size() * plugin.getConfig().getDouble("line-spacing", 0.25);
                            double minPixel = plugin.getConfig().getDouble("min-pixel-block-size", 0.12);
                            overallHeight = Math.max(overallHeight, minPixel * hy.getImage().getHeight());
                            hy.spawnImage(chunk, overallHeight);
                        } else {
                            hy.spawn();
                        }
                        mgr.saveAll();
                        sender.sendMessage(ChatColor.GREEN + "Set hologram yaw to " + hy.getImageYawDegrees() + " for " + args[1]);
                        return true;

                    case "face":
                        // /hd face <id>
                        if (!(sender instanceof Player)) { sender.sendMessage("Only players can use /hd face <id>"); return true; }
                        if (args.length < 2) { sender.sendMessage("Usage: /hd face <id>"); return true; }
                        Hologram hf2 = mgr.get(args[1]);
                        if (hf2 == null) { sender.sendMessage(ChatColor.RED + "Hologram not found"); return true; }
                        Player caller = (Player) sender;
                        if (hf2.getLocation() == null) { sender.sendMessage("Hologram has no valid location"); return true; }
                        org.bukkit.Location base = hf2.getLocation();
                        org.bukkit.Location playerLoc = caller.getLocation();
                        org.bukkit.util.Vector dir = playerLoc.toVector().subtract(base.toVector());
                        if (dir.lengthSquared() < 0.0001) { sender.sendMessage("You are too close to determine facing"); return true; }
                        org.bukkit.Location tmp = base.clone();
                        tmp.setDirection(dir);
                        double yawToPlayer = tmp.getYaw();
                        hf2.setImageYawDegrees(yawToPlayer);
                        hf2.despawn();
                        if (hf2.getImage() != null) {
                            int maxPerLine = plugin.getConfig().getInt("max-stands-per-line", 64);
                            int chunk = Math.max(1, (int)Math.ceil((double)hf2.getImage().getWidth() / (double)maxPerLine));
                            double overallHeight = plugin.getConfig().getDouble("image-overall-height", 4.0);
                            if (!hf2.getLines().isEmpty()) overallHeight = hf2.getLines().size() * plugin.getConfig().getDouble("line-spacing", 0.25);
                            double minPixel = plugin.getConfig().getDouble("min-pixel-block-size", 0.12);
                            overallHeight = Math.max(overallHeight, minPixel * hf2.getImage().getHeight());
                            hf2.spawnImage(chunk, overallHeight);
                        } else {
                            hf2.spawn();
                        }
                        mgr.saveAll();
                        sender.sendMessage(ChatColor.GREEN + "Hologram " + args[1] + " now faces you (yaw=" + hf2.getImageYawDegrees() + ")");
                        return true;

                case "list":
                    sender.sendMessage("Holograms:");
                    for (Hologram h : mgr.list()) sender.sendMessage(" - " + h.getId());
                    return true;

                case "teleport":
                    if (!(sender instanceof Player)) { sender.sendMessage("Only players"); return true; }
                    if (args.length < 2) { sender.sendMessage("Usage: /hd teleport <id>"); return true; }
                    Hologram t = mgr.get(args[1]);
                    if (t == null) { sender.sendMessage("Not found"); return true; }
                    Player pl = (Player) sender;
                    pl.teleport(t.getLocation().add(0,1,0));
                    sender.sendMessage("Teleported to hologram " + args[1]);
                    return true;

                case "movehere":
                    if (!(sender instanceof Player)) { sender.sendMessage("Only players"); return true; }
                    if (args.length < 2) { sender.sendMessage("Usage: /hd movehere <id>"); return true; }
                    Hologram mh = mgr.get(args[1]);
                    if (mh == null) { sender.sendMessage("Not found"); return true; }
                    mh.setLocation(((Player)sender).getLocation());
                    mh.spawn();
                    sender.sendMessage("Moved hologram " + args[1]);
                    return true;

                case "addline":
                    // new signature: /hd addline <id> <index> <text>
                    if (args.length < 4) { sender.sendMessage("Использование: /hd addline <название> <номер_строки> <текст>"); return true; }
                    Hologram al = mgr.get(args[1]);
                    if (al == null) { sender.sendMessage(ChatColor.RED + "Голограмма не найдена"); return true; }
                    int insertIndex;
                    try { insertIndex = Integer.parseInt(args[2]); } catch (NumberFormatException nfe) { sender.sendMessage("Неверный номер строки"); return true; }
                    String insertText = ChatColor.translateAlternateColorCodes('&', join(args, 3));
                    // insert at index (if index > size -> append)
                    if (insertIndex < 0) insertIndex = 0;
                    if (insertIndex >= al.getLines().size()) {
                        al.addLine(insertText);
                    } else {
                        al.getLines().add(insertIndex, insertText);
                    }
                    al.spawn();
                    sender.sendMessage(ChatColor.GREEN + "Добавлена строка в голограмму " + ChatColor.YELLOW + args[1] + ChatColor.GRAY + " на позицию " + insertIndex);
                    return true;

                case "removeline":
                    if (args.length < 3) { sender.sendMessage("Использование: /hd removeline <название> <номер_строки>"); return true; }
                    Hologram rl = mgr.get(args[1]);
                    if (rl == null) { sender.sendMessage(ChatColor.RED + "Голограмма не найдена"); return true; }
                    int ridx;
                    try { ridx = Integer.parseInt(args[2]); } catch (NumberFormatException nfe) { sender.sendMessage("Неверный номер строки"); return true; }
                    rl.removeLine(ridx);
                    rl.spawn();
                    sender.sendMessage(ChatColor.GREEN + "Удалена строка " + ridx + " из " + args[1]);
                    return true;

                case "setline":
                    if (args.length < 4) { sender.sendMessage("Использование: /hd setline <название> <номер_строки> <текст>"); return true; }
                    Hologram sl = mgr.get(args[1]);
                    if (sl == null) { sender.sendMessage(ChatColor.RED + "Голограмма не найдена"); return true; }
                    int sidx;
                    try { sidx = Integer.parseInt(args[2]); } catch (NumberFormatException nfe) { sender.sendMessage("Неверный номер строки"); return true; }
                    String stext = ChatColor.translateAlternateColorCodes('&', join(args, 3));
                    sl.setLine(sidx, stext);
                    sl.spawn();
                    sender.sendMessage(ChatColor.GREEN + "Установлена строка " + sidx + " для " + args[1]);
                    return true;

                case "readtext":
                    if (args.length < 4) { sender.sendMessage("Использование: /hd readtext <название> <номер_строки> <текст>"); return true; }
                    Hologram rt = mgr.get(args[1]);
                    if (rt == null) { sender.sendMessage(ChatColor.RED + "Голограмма не найдена"); return true; }
                    int rid;
                    try { rid = Integer.parseInt(args[2]); } catch (NumberFormatException nfe) { sender.sendMessage("Неверный номер строки"); return true; }
                    String rtext = ChatColor.translateAlternateColorCodes('&', join(args, 3));
                    rt.setLine(rid, rtext);
                    rt.spawn();
                    sender.sendMessage(ChatColor.GREEN + "Текст строки " + rid + " обновлён");
                    return true;

                case "readimage":
                    // readimage <id> <image.png> [<width>x<height>]
                    if (args.length < 3) { sender.sendMessage("Usage: /hd readimage <id> <image.png> [<width>x<height>]"); return true; }
                    Hologram hi = mgr.get(args[1]);
                    if (hi == null) { sender.sendMessage("Not found"); return true; }
                    String imageName = args[2];
                    File f = new File(plugin.getDataFolder(), "images" + File.separator + imageName);
                    if (!f.exists()) { sender.sendMessage("Image not found in plugin/images/"); return true; }
                    try {
                        BufferedImage img = ImageIO.read(f);
                        if (img == null) { sender.sendMessage("Failed to read image"); return true; }

                        // optional size argument like 64x64
                        int targetW = img.getWidth();
                        int targetH = img.getHeight();
                        if (args.length >= 4) {
                            String sz = args[3].toLowerCase();
                            if (sz.matches("\\d+x\\d+")) {
                                String[] parts = sz.split("x");
                                try {
                                    targetW = Integer.parseInt(parts[0]);
                                    targetH = Integer.parseInt(parts[1]);
                                } catch (NumberFormatException nfe) {
                                    sender.sendMessage("Invalid size parameter, using original image size.");
                                }
                            } else {
                                sender.sendMessage("Size must be like 64x64. Using original image size.");
                            }
                        }

                        // high-quality scaling to requested size (if changed)
                        if (img.getWidth() != targetW || img.getHeight() != targetH) {
                            BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
                            java.awt.Graphics2D g = scaled.createGraphics();
                            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                            g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                            g.drawImage(img, 0, 0, targetW, targetH, null);
                            g.dispose();
                            img = scaled;
                        }

                        // compute chunk automatically so stands per line <= max-stands-per-line
                        int maxPerLine = plugin.getConfig().getInt("max-stands-per-line", 64);
                        int chunk = Math.max(1, (int)Math.ceil((double)img.getWidth() / (double)maxPerLine));

                        // compute overall height: use existing text height if present, otherwise config default
                        double overallHeight = plugin.getConfig().getDouble("image-overall-height", 4.0);
                        if (!hi.getLines().isEmpty()) {
                            overallHeight = hi.getLines().size() * plugin.getConfig().getDouble("line-spacing", 0.25);
                        }
                        double minPixel = plugin.getConfig().getDouble("min-pixel-block-size", 0.12);
                        overallHeight = Math.max(overallHeight, minPixel * img.getHeight());

                        // set image on hologram (transient runtime), persist filename and spawn grid
                        hi.setImage(img);
                        hi.setImageFileName(imageName);
                        hi.spawnImage(chunk, overallHeight);
                        sender.sendMessage(ChatColor.GREEN + "Image displayed in hologram " + args[1] + ". chunk=" + chunk + ", size=" + img.getWidth() + "x" + img.getHeight());
                    } catch (IOException ex) {
                        sender.sendMessage(ChatColor.RED + "Error reading image: " + ex.getMessage());
                    }
                    return true;

                case "imetext":
                    // imetext <holoId> <filename.png> <text...> [<width>x<height>]
                    if (args.length < 4) { sender.sendMessage("Usage: /hd imetext <holoId> <file.png> <text...> [<width>x<height>]"); return true; }
                    String holoId = args[1];
                    String outName = args[2];
                    Hologram target = mgr.get(holoId);
                    if (target == null) { sender.sendMessage("Hologram not found: " + holoId); return true; }
                    // collect text (args[3..n]) until optional size param
                    String possibleSize = null;
                    String text;
                    if (args[args.length - 1].toLowerCase().matches("\\d+x\\d+")) {
                        possibleSize = args[args.length - 1];
                        text = join(args, 3, args.length - 1);
                    } else {
                        text = join(args, 3);
                    }

                    int tW = 128, tH = 64;
                    if (possibleSize != null) {
                        String[] sizeParts = possibleSize.split("x");
                        try { tW = Integer.parseInt(sizeParts[0]); tH = Integer.parseInt(sizeParts[1]); } catch (NumberFormatException ignored) {}
                    }

                    // create images directory if missing
                    java.io.File imagesDir = new java.io.File(plugin.getDataFolder(), "images");
                    if (!imagesDir.exists()) imagesDir.mkdirs();
                    java.io.File outFile = new java.io.File(imagesDir, outName);

                    try {
                        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(tW, tH, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                        java.awt.Graphics2D g = img.createGraphics();
                        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                        // background transparent
                        g.setComposite(java.awt.AlphaComposite.Clear);
                        g.fillRect(0,0,tW,tH);
                        g.setComposite(java.awt.AlphaComposite.SrcOver);

                        // gradient paint from blue to gold
                        java.awt.Color blue = new java.awt.Color(0x1E90FF);
                        java.awt.Color gold = new java.awt.Color(0xFFD700);
                        java.awt.GradientPaint gp = new java.awt.GradientPaint(0,0,blue,tW,0,gold);
                        g.setPaint(gp);

                        // choose font size to fit
                        int fontSize = Math.max(12, tH - 8);
                        java.awt.Font font = new java.awt.Font("SansSerif", java.awt.Font.BOLD, fontSize);
                        g.setFont(font);

                        // measure and adjust font to fit width
                        java.awt.FontMetrics fm = g.getFontMetrics();
                        int textWidth = fm.stringWidth(text);
                        while (textWidth > tW - 8 && fontSize > 8) {
                            fontSize -= 2;
                            font = font.deriveFont((float)fontSize);
                            g.setFont(font);
                            fm = g.getFontMetrics();
                            textWidth = fm.stringWidth(text);
                        }

                        int x = (tW - textWidth) / 2;
                        int y = (tH - fm.getHeight()) / 2 + fm.getAscent();

                        // draw text with gradient
                        g.drawString(text, x, y);
                        g.dispose();

                        javax.imageio.ImageIO.write(img, "PNG", outFile);
                        sender.sendMessage("Generated image " + outName + " (" + tW + "x" + tH + ")");

                        // now call readimage logic by loading the file and rendering
                        BufferedImage read = javax.imageio.ImageIO.read(outFile);
                        if (read == null) { sender.sendMessage("Failed to generate image"); return true; }

                        // reuse existing logic: compute chunk and overall height
                        int maxPerLine = plugin.getConfig().getInt("max-stands-per-line", 64);
                        int chunk = Math.max(1, (int)Math.ceil((double)read.getWidth() / (double)maxPerLine));
                        double overallHeight = plugin.getConfig().getDouble("image-overall-height", 4.0);
                        if (!target.getLines().isEmpty()) overallHeight = target.getLines().size() * plugin.getConfig().getDouble("line-spacing", 0.25);
                        double minPixel = plugin.getConfig().getDouble("min-pixel-block-size", 0.12);
                        overallHeight = Math.max(overallHeight, minPixel * read.getHeight());

                        target.setImage(read);
                        target.setImageFileName(outName);
                        target.spawnImage(chunk, overallHeight);
                        sender.sendMessage("Rendered generated text image into hologram " + holoId + ". chunk=" + chunk);
                    } catch (Exception ex) {
                        sender.sendMessage("Error generating image: " + ex.getMessage());
                    }
                    return true;

                case "rotate":
                case "r":
                    // /hd rotate <id> <0|90|180|270>
                    if (args.length < 3) { sender.sendMessage("Usage: /hd rotate <id> <0|90|180|270>"); return true; }
                    Hologram hr = mgr.get(args[1]);
                    if (hr == null) { sender.sendMessage(ChatColor.RED + "Hologram not found"); return true; }
                    int deg;
                    try { deg = Integer.parseInt(args[2]); } catch (NumberFormatException nfe) { sender.sendMessage("Invalid degrees"); return true; }
                    deg = ((deg % 360) + 360) % 360;
                    if (deg != 0 && deg != 90 && deg != 180 && deg != 270) { sender.sendMessage("Rotation must be one of 0,90,180,270"); return true; }
                    hr.setImageRotationDegrees(deg);
                    // respawn appropriately
                    hr.despawn();
                    if (hr.getImage() != null) {
                        int maxPerLine = plugin.getConfig().getInt("max-stands-per-line", 64);
                        int chunk = Math.max(1, (int)Math.ceil((double)hr.getImage().getWidth() / (double)maxPerLine));
                        double overallHeight = plugin.getConfig().getDouble("image-overall-height", 4.0);
                        if (!hr.getLines().isEmpty()) overallHeight = hr.getLines().size() * plugin.getConfig().getDouble("line-spacing", 0.25);
                        double minPixel = plugin.getConfig().getDouble("min-pixel-block-size", 0.12);
                        overallHeight = Math.max(overallHeight, minPixel * hr.getImage().getHeight());
                        hr.spawnImage(chunk, overallHeight);
                    } else {
                        hr.spawn();
                    }
                    mgr.saveAll();
                    sender.sendMessage(ChatColor.GREEN + "Set rotation " + deg + " for " + args[1]);
                    return true;

                case "flip":
                case "f":
                    // /hd flip <id> <h|v|none>
                    if (args.length < 3) { sender.sendMessage("Usage: /hd flip <id> <h|v|none>"); return true; }
                    Hologram hf = mgr.get(args[1]);
                    if (hf == null) { sender.sendMessage(ChatColor.RED + "Hologram not found"); return true; }
                    String which = args[2].toLowerCase();
                    if (which.equals("h")) { hf.setImageFlipH(true); hf.setImageFlipV(false); }
                    else if (which.equals("v")) { hf.setImageFlipV(true); hf.setImageFlipH(false); }
                    else if (which.equals("none")) { hf.setImageFlipH(false); hf.setImageFlipV(false); }
                    else { sender.sendMessage("Invalid flip option. Use h, v or none"); return true; }
                    hf.despawn();
                    if (hf.getImage() != null) {
                        int maxPerLine = plugin.getConfig().getInt("max-stands-per-line", 64);
                        int chunk = Math.max(1, (int)Math.ceil((double)hf.getImage().getWidth() / (double)maxPerLine));
                        double overallHeight = plugin.getConfig().getDouble("image-overall-height", 4.0);
                        if (!hf.getLines().isEmpty()) overallHeight = hf.getLines().size() * plugin.getConfig().getDouble("line-spacing", 0.25);
                        double minPixel = plugin.getConfig().getDouble("min-pixel-block-size", 0.12);
                        overallHeight = Math.max(overallHeight, minPixel * hf.getImage().getHeight());
                        hf.spawnImage(chunk, overallHeight);
                    } else {
                        hf.spawn();
                    }
                    mgr.saveAll();
                    sender.sendMessage(ChatColor.GREEN + "Set flip for " + args[1] + " to " + which);
                    return true;

                case "fix":
                    if (args.length < 2) { sender.sendMessage("Использование: /hd fix <название>"); return true; }
                    Hologram fx = mgr.get(args[1]);
                    if (fx == null) { sender.sendMessage(ChatColor.RED + "Голограмма не найдена"); return true; }
                    fx.despawn(); fx.spawn();
                    sender.sendMessage(ChatColor.GREEN + "Голограмма " + args[1] + " обновлена");
                    return true;

                case "save":
                    mgr.saveAll(); sender.sendMessage(ChatColor.GREEN + "Все голограммы сохранены"); return true;

                case "reload":
                    plugin.reloadConfig();
                    for (Hologram h : mgr.list()) h.despawn();
                    mgr.loadAll();
                    sender.sendMessage(ChatColor.GREEN + "Плагин перезагружен, голограммы загружены");
                    return true;

                default:
                    sender.sendMessage(ChatColor.RED + "Неизвестная команда. Используй /hd для списка команд.");
                    return true;
            }
        } catch (Exception ex) {
            sender.sendMessage("Error: " + ex.getMessage());
            ex.printStackTrace();
            return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- HologramsFSMP Help ---");
        sender.sendMessage(ChatColor.YELLOW + "/hd create <название>" + ChatColor.GRAY + " - Создать голограмму под вами");
        sender.sendMessage(ChatColor.YELLOW + "/hd delete <название>" + ChatColor.GRAY + " - Удалить голограмму");
        sender.sendMessage(ChatColor.YELLOW + "/hd list" + ChatColor.GRAY + " - Список голограмм");
        sender.sendMessage(ChatColor.YELLOW + "/hd teleport <название>" + ChatColor.GRAY + " - Телепорт к голограмме");
        sender.sendMessage(ChatColor.YELLOW + "/hd movehere <название>" + ChatColor.GRAY + " - Переместить голограмму к вам");
        sender.sendMessage(ChatColor.YELLOW + "/hd addline <название> <номер> <текст>" + ChatColor.GRAY + " - Добавить/вставить строку");
        sender.sendMessage(ChatColor.YELLOW + "/hd removeline <название> <номер>" + ChatColor.GRAY + " - Удалить строку");
        sender.sendMessage(ChatColor.YELLOW + "/hd setline <название> <номер> <текст>" + ChatColor.GRAY + " - Установить строку");
        sender.sendMessage(ChatColor.YELLOW + "/hd readtext <название> <номер> <текст>" + ChatColor.GRAY + " - Редактировать строку");
        sender.sendMessage(ChatColor.YELLOW + "/hd readimage <название> <файл.png>" + ChatColor.GRAY + " - Загрузить изображение из plugins/HologramsFSMP/images/");
        sender.sendMessage(ChatColor.YELLOW + "/hd rotate|r <название> <0|90|180|270>" + ChatColor.GRAY + " - Повернуть изображение голограммы (содержимое)");
        sender.sendMessage(ChatColor.YELLOW + "/hd flip|f <название> <h|v|none>" + ChatColor.GRAY + " - Отразить изображение по горизонтали/вертикали (содержимое)");
        sender.sendMessage(ChatColor.YELLOW + "/hd yaw|y <название> <deg|+rel|-rel>" + ChatColor.GRAY + " - Повернуть плоскость голограммы (ось Y)");
        sender.sendMessage(ChatColor.YELLOW + "/hd face <название>" + ChatColor.GRAY + " - Повернуть плоскость так, чтобы она смотрела на вас");
        sender.sendMessage(ChatColor.YELLOW + "/hd fix <название>" + ChatColor.GRAY + " - Пересоздать отображение голограммы");
        sender.sendMessage(ChatColor.YELLOW + "/hd save" + ChatColor.GRAY + " - Сохранить все голограммы");
        sender.sendMessage(ChatColor.YELLOW + "/hd reload" + ChatColor.GRAY + " - Перезагрузить плагин и голограммы");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        HologramManager mgr = plugin.getManager();
        if (args.length == 1) {
            String[] subs = new String[]{"create","delete","list","teleport","movehere","addline","removeline","setline","readtext","readimage","imetext","rotate","r","flip","f","yaw","y","face","fix","save","reload"};
            for (String s : subs) if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            return completions;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("create")) return completions; // name free text
            // suggest existing hologram ids
            for (Hologram h : mgr.list()) {
                if (h.getId().toLowerCase().startsWith(args[1].toLowerCase())) completions.add(h.getId());
            }
            return completions;
        }
        // for index arguments suggest numbers
        if (args.length == 3) {
            try {
                for (int i = 0; i < 20; i++) completions.add(String.valueOf(i));
            } catch (Exception ignored) {}
            return completions;
        }
        return completions;
    }

    private String join(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i != start) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private String join(String[] args, int start, int endExclusive) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < endExclusive && i < args.length; i++) {
            if (i != start) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private List<String> imageToLines(BufferedImage img) {
        List<String> lines = new ArrayList<>();
        for (int y = 0; y < img.getHeight(); y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb) & 0xFF;
                String hex = String.format("%02x%02x%02x", r, g, b);
                // build §x§R§R§G§G§B§B sequence
                String colorCode = toSectionHex(hex);
                sb.append(colorCode).append('\u2588'); // full block
            }
            lines.add(sb.toString());
        }
        return lines;
    }

    private String toSectionHex(String hex) {
        // hex like rrggbb
        StringBuilder s = new StringBuilder();
        char section = '\u00A7';
        s.append(section).append('x');
        for (char c : hex.toCharArray()) {
            s.append(section).append(c);
        }
        return s.toString();
    }
}
