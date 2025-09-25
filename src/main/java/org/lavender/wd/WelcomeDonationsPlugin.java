package org.lavender.wd;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;

/**
 *
 * @authors Lavender & VaCris
 */
public class WelcomeDonationsPlugin extends JavaPlugin implements Listener, PluginMessageListener {

    public enum ServerRole {
        AUTH, SPAWN, SURVIVAL
    }

    private ServerRole role;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String roleStr = getConfig().getString("server-role", "SPAWN").toUpperCase();
        try {
            role = ServerRole.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            role = ServerRole.SPAWN;
        }

        // Registrar canales de mensajería con proxy (opcional)
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

        if (role == ServerRole.AUTH) {
            getLogger().info("Plugin deshabilitado en AUTH.");
            getServer().getPluginManager().registerEvents(this, this); // Solo para mostrar msg si hace falta
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(color(getConfig().getString("messages.disabled-on-auth"))));
            // Desactivar lógicamente: no registrar listeners de bienvenida ni comandos de donación
            return; // Sin disablePlugin() para permitir mensajes de aviso si lo deseas
        }

        // Listeners y comandos activos solo en SPAWN/SURVIVAL
        getServer().getPluginManager().registerEvents(this, this);

        var cmd = this.getCommand("donatebroadcast");
        if (cmd != null) {
            var exec = new org.lavender.wd.commands.DonateBroadcastCommand(this);
            cmd.setExecutor(exec);
            cmd.setTabCompleter(exec);
        } else {
            this.getLogger().severe("El comando \'donatebroadcast\' no está definido en plugin.yml");
        }

        getLogger().log(Level.INFO, "WelcomeDonations habilitado en rol: {0}", role);
    }

    @Override
    public void onDisable() {
        // Limpieza si fuese necesaria
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!getConfig().getBoolean("welcome.enabled", true)) {
            return;
        }
        Player p = e.getPlayer();
        boolean first = !p.hasPlayedBefore();

        if (first) {
            String chat = getConfig().getString("welcome.first-join.chat");
            sendChat(chat, p.getName());
            sendTitleFromConfig("welcome.first-join", p);
            playSoundFromConfig("welcome.first-join.sound", p);
        } else {
            String chat = getConfig().getString("welcome.rejoin.chat");
            sendChat(chat, p.getName());
            if (getConfig().getBoolean("welcome.rejoin.title.enabled", false)) {
                sendTitleFromConfig("welcome.rejoin", p);
            }
            playSoundFromConfig("welcome.rejoin.sound", p);
        }
    }

    private void sendChat(String template, String playerName) {
        if (template == null || template.isEmpty()) {
            return;
        }
        String msg = template.replace("%player%", playerName);
        Bukkit.getServer().broadcastMessage(color(msg));
    }

    private void sendTitleFromConfig(String basePath, Player p) {
        boolean enabled = getConfig().getBoolean(basePath + ".title.enabled", false);
        if (!enabled) {
            return;
        }
        String main = color(getConfig().getString(basePath + ".title.main", ""));
        String sub = color(getConfig().getString(basePath + ".title.sub", ""));
        int fi = getConfig().getInt(basePath + ".title.fade-in", 10);
        int st = getConfig().getInt(basePath + ".title.stay", 50);
        int fo = getConfig().getInt(basePath + ".title.fade-out", 10);
        p.sendTitle(main, sub, fi, st, fo);
    }

    private void playSoundFromConfig(String path, Player p) {
        String s = getConfig().getString(path, "");
        if (s == null || s.isEmpty()) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(s);
            p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {
            getLogger().log(Level.WARNING, "Sonido inv\u00e1lido en config: {0}", s);
        }
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    // ==== Donaciones ====
    public void announceDonationLocal(String player, String item, String amount) {
        if (!getConfig().getBoolean("donations.enabled", true)) {
            return;
        }
        String fmt = getConfig().getString("donations.format", "&6[Donación] &e%player% &fapoyó con &b%item% &f%amount%");
        String amountText = (amount == null || amount.isEmpty()) ? "" : amount;
        String msg = fmt.replace("%player%", player).replace("%item%", item).replace("%monto%", amountText).replace("%amount%", amountText);
        Bukkit.broadcastMessage(color(msg));
        // Sonido global
        String s = getConfig().getString("donations.sound", "");
        if (s != null && !s.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(s);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), sound, 1f, 1f);
                }
            } catch (IllegalArgumentException ignored) {
                getLogger().log(Level.WARNING, "Sonido de donaci\u00f3n inv\u00e1lido: {0}", s);
            }
        }
    }

    public void announceRankPurchase(String player, String rank) {
        if (!getConfig().getBoolean("ranks.enabled", true)) {
            return;
        }
        String fmt = getConfig().getString("ranks.format", "&6[Rango] &e%player% &fcompró &b%rank% ¡Bienvenido!");
        Bukkit.broadcastMessage(color(fmt.replace("%player%", player).replace("%rank%", rank)));

        String soundName = getConfig().getString("ranks.sound", "ENTITY_VILLAGER_CELEBRATE");
        if (soundName != null && !soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), sound, 1f, 1f);
                }
            } catch (IllegalArgumentException ignored) {
                getLogger().warning("Sonido inválido: " + soundName);
            }
        }
    }

    // ==== Mensajería con Proxy (BungeeCord/Velocity) ====
    public void forwardDonationViaProxy(String player, String item, String amount) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            // Subcanal "Forward"
            out.writeUTF("Forward");
            // Server target, usa "ALL" o uno en particular
            out.writeUTF("ALL");
            // Canal propio del plugin
            out.writeUTF("wd:donation");

            // Carga útil
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream msgOut = new DataOutputStream(msgBytes);
            msgOut.writeUTF(player);
            msgOut.writeUTF(item);
            msgOut.writeUTF(amount == null ? "" : amount);

            byte[] data = msgBytes.toByteArray();
            out.writeShort(data.length);
            out.write(data);

            // Enviar a través de cualquier jugador online
            Player any = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
            if (any != null) {
                any.sendPluginMessage(this, "BungeeCord", b.toByteArray());
            }
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Error reenviando donaci\u00f3n: {0}", e.getMessage());
        }
    }

    public void forwardRankViaProxy(String player, String rank) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Forward");
            out.writeUTF("ALL");
            out.writeUTF("wd:rank");

            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream msgOut = new DataOutputStream(msgBytes);
            msgOut.writeUTF(player);
            msgOut.writeUTF(rank);

            byte[] data = msgBytes.toByteArray();
            out.writeShort(data.length);
            out.write(data);

            Player any = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
            if (any != null) {
                any.sendPluginMessage(this, "BungeeCord", b.toByteArray());
            }
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Error reenviando rango: {0}", e.getMessage());
        }
    }

    public void broadcastDonation(String player, String item, String amount) {
        this.announceDonationLocal(player, item, amount);
        if (this.getConfig().getBoolean("donations.proxy-forward.enabled", false)) {
            this.forwardDonationViaProxy(player, item, amount);
        }
    }

    public void broadcastRank(String player, String rank) {
        this.announceRankPurchase(player, rank);
        if (this.getConfig().getBoolean("ranks.proxy-forward.enabled", false)) {
            this.forwardRankViaProxy(player, rank);
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String sub = in.readUTF();
            if ("Forward".equals(sub)) {
                String server = in.readUTF();
                String thisChannel = in.readUTF();

                if ("wd:donation".equals(thisChannel)) {
                    short len = in.readShort();
                    byte[] data = new byte[len];
                    in.readFully(data);
                    DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(data));
                    announceDonationLocal(msgIn.readUTF(), msgIn.readUTF(), msgIn.readUTF());
                }

                if ("wd:rank".equals(thisChannel)) {
                    short len = in.readShort();
                    byte[] data = new byte[len];
                    in.readFully(data);
                    DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(data));
                    announceRankPurchase(msgIn.readUTF(), msgIn.readUTF());
                }
            }
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Error recibiendo mensaje de proxy: {0}", e.getMessage());
        }
    }
}
