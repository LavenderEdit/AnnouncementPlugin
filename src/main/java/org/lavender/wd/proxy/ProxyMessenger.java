package org.lavender.wd.proxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.lavender.wd.donations.DonationAnnouncement;
import org.lavender.wd.donations.RankPurchase;

/**
 *
 * @author Studios TKOH!
 */
public class ProxyMessenger implements PluginMessageListener {

    private static final String CHANNEL = "BungeeCord";
    private static final String DONATION_CHANNEL = "wd:donation";
    private static final String RANK_CHANNEL = "wd:rank";

    private final JavaPlugin plugin;

    private Consumer<DonationAnnouncement> donationHandler = announcement -> {
    };
    private Consumer<RankPurchase> rankHandler = purchase -> {
    };

    public ProxyMessenger(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    public void unregister() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
    }

    public void setDonationHandler(Consumer<DonationAnnouncement> handler) {
        this.donationHandler = handler == null ? announcement -> {
        } : handler;
    }

    public void setRankHandler(Consumer<RankPurchase> handler) {
        this.rankHandler = handler == null ? purchase -> {
        } : handler;
    }

    public void sendDonation(DonationAnnouncement announcement) {
        sendForward(DONATION_CHANNEL, out -> {
            out.writeUTF(announcement.player());
            out.writeUTF(announcement.item());
            out.writeUTF(announcement.amountOrEmpty());
        });
    }

    public void sendRank(RankPurchase purchase) {
        sendForward(RANK_CHANNEL, out -> {
            out.writeUTF(purchase.player());
            out.writeUTF(purchase.rank());
        });
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String sub = in.readUTF();
            if (!"Forward".equals(sub)) {
                return;
            }
            in.readUTF(); // target server, unused
            String innerChannel = in.readUTF();
            short len = in.readShort();
            byte[] data = new byte[len];
            in.readFully(data);
            try (DataInputStream payload = new DataInputStream(new ByteArrayInputStream(data))) {
                if (DONATION_CHANNEL.equals(innerChannel)) {
                    DonationAnnouncement announcement = new DonationAnnouncement(
                            payload.readUTF(),
                            payload.readUTF(),
                            payload.readUTF());
                    donationHandler.accept(announcement);
                } else if (RANK_CHANNEL.equals(innerChannel)) {
                    RankPurchase purchase = new RankPurchase(
                            payload.readUTF(),
                            payload.readUTF(),
                            "");
                    rankHandler.accept(purchase);
                }
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Error procesando mensaje del proxy: {0}", ex.getMessage());
        }
    }

    private void sendForward(String subChannel, PayloadWriter writer) {
        Player sender = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (sender == null) {
            return;
        }
        try {
            ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
            try (DataOutputStream payload = new DataOutputStream(payloadBytes)) {
                writer.write(payload);
            }
            ByteArrayOutputStream outerBytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(outerBytes)) {
                out.writeUTF("Forward");
                out.writeUTF("ALL");
                out.writeUTF(subChannel);
                byte[] data = payloadBytes.toByteArray();
                out.writeShort(data.length);
                out.write(data);
            }
            sender.sendPluginMessage(plugin, CHANNEL, outerBytes.toByteArray());
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "No se pudo reenviar el mensaje al proxy: {0}", ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface PayloadWriter {

        void write(DataOutputStream out) throws IOException;
    }

}
