package dev.j3fftw.soundmuffler;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.energy.ChargableBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public class SoundMufflerListener extends PacketAdapter implements Listener {

    public SoundMufflerListener(Plugin plugin) {
        super(plugin, ListenerPriority.NORMAL,
            PacketType.Play.Server.NAMED_SOUND_EFFECT, PacketType.Play.Server.ENTITY_SOUND
        );
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.NAMED_SOUND_EFFECT
            || event.getPacketType() == PacketType.Play.Server.ENTITY_SOUND
        ) {
            Location loc;
            if (event.getPacketType() == PacketType.Play.Server.NAMED_SOUND_EFFECT){
                int x = event.getPacket().getIntegers().read(0) >> 3;
                int y = event.getPacket().getIntegers().read(1) >> 3;
                int z = event.getPacket().getIntegers().read(2) >> 3;
                loc = new Location(event.getPlayer().getWorld(), x, y, z);
            } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_SOUND) {
                loc = event.getPlayer().getWorld().getEntities().stream()
                    .filter(e -> e.getEntityId() == event.getPacket().getIntegers().read(0))
                    .map(Entity::getLocation)
                    .findAny().orElse(null);
            } else return;

            if (loc == null)
                return;

            final Block soundMuff = findSoundMuffler(loc);
            if (soundMuff != null
                && BlockStorage.getLocationInfo(soundMuff.getLocation(), "enabled") != null
                && BlockStorage.getLocationInfo(soundMuff.getLocation(), "enabled").equals("true")
                && ChargableBlock.getCharge(soundMuff) > 8
            ) {

                int volume = Integer.parseInt(BlockStorage.getLocationInfo(soundMuff.getLocation(), "volume"));
                if (volume == 0) {
                    event.setCancelled(true);
                } else {
                    event.getPacket().getFloat().write(0, (float) volume / 100.0f);
                }
            }
        }
    }

    private Block findSoundMuffler(Location loc) {
        final int dis = SoundMufflerMachine.DISTANCE;
        for (int x = loc.getBlockX() - dis; x < loc.getBlockX() + dis; x++) {
            for (int y = loc.getBlockY() - dis; y < loc.getBlockY() + dis; y++) {
                for (int z = loc.getBlockZ() - dis; z < loc.getBlockZ() + dis; z++) {
                    if (!Objects.requireNonNull(loc.getWorld()).isChunkLoaded(x >> 4, z >> 4))
                        continue;
                    Block b = loc.getWorld().getBlockAt(x, y, z);
                    if (b.getType() == Material.WHITE_CONCRETE && BlockStorage.hasBlockInfo(b)) {
                        SlimefunItem item = BlockStorage.check(b);
                        if (item.getID().equals("SOUND_MUFFLER")) {
                            return b;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void start() {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }
}
