package com.playmonumenta.plugins.protocollib;

import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutEntityMetadataHandle;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.commands.GlowingCommand;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.DivineJusticeCS;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

/**
 * Packet listener for selectively disabling the glowing effect
 */
public class GlowingReplacer extends PacketAdapter {

	private static final byte GLOWING_BIT = 0b01000000;

	public GlowingReplacer(Plugin plugin) {
		super(plugin, ListenerPriority.NORMAL,
			PacketType.Play.Server.ENTITY_METADATA);
	}

	@Override
	public void onPacketSending(PacketEvent event) {

		PacketContainer packet = event.getPacket();

		// Entity flags is a byte and is at index 0, see https://wiki.vg/Entity_metadata#Entity
		PacketPlayOutEntityMetadataHandle handle = PacketPlayOutEntityMetadataHandle.createHandle(packet.getHandle());
		if (handle.getMetadataItems().isEmpty() || !(handle.getMetadataItems().get(0).value() instanceof Byte data) || (data & GLOWING_BIT) == 0) {
			// No glowing bit is set, so there's nothing to do
			return;
		}

		Player player = event.getPlayer();
		DivineJusticeCS cosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new DivineJusticeCS());
		int playerSettings = ScoreboardUtils.getScoreboardValue(player, GlowingCommand.SCOREBOARD_OBJECTIVE).orElse(0);

		// Check if glowing is disabled for the entity's type.
		if (playerSettings != 0xFFFFFFFF) { // If all glowing is disabled, this check can be skipped.
			Entity entity = packet.getEntityModifier(event).read(0); // NB: this is the first int, not (just) the first entity in the packet.
			if (entity == null || (GlowingCommand.isGlowingEnabled(player, playerSettings, entity)
				// Purified Ash can only be picked up by clerics, so it doesn't make sense to highlight them for other players
				&& !(entity instanceof Item item && item.getItemStack().getType() == cosmetic.justiceAsh()
				&& cosmetic.justiceAshName().equals(ItemUtils.getPlainNameIfExists(item.getItemStack())) && !DivineJustice.canPickUpAsh(player)))) {
				return;
			}
		}

		// Finally, unset the glowing bits
		// We need to clone the packet to not affect other players (as the packet is shared between players)
		try {
			packet = packet.deepClone();
		} catch (RuntimeException e) {
			// sometimes, cloning just fails for some reason?
			if (e.getMessage() != null && e.getMessage().startsWith("Unable to clone")) {
				MMLog.warning("Failed to clone packet of type " + packet.getType());
				return;
			}
			throw e;
		}
		handle = PacketPlayOutEntityMetadataHandle.createHandle(packet.getHandle());
		handle.getMetadataItems().set(0, handle.getMetadataItems().get(0).cloneWithValue((byte) (data & ~GLOWING_BIT)));
		event.setPacket(packet);
	}

}
