package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PotionUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PumpkinPieOverride extends BaseOverride {
	@Override
	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity, ItemStack item) {
		if (player == null
			    || !ServerProperties.getTrickyCreepersEnabled()
			    || !(clickedEntity instanceof Creeper creeper)
			    || EntityUtils.isTrainingDummy(clickedEntity)
			    || !InventoryUtils.testForItemWithName(item, "Creeper's Delight", true)
			    || "plots".equals(ServerProperties.getShardName())
			    || "playerplots".equals(ServerProperties.getShardName())
			    || "guildplots".equals(ServerProperties.getShardName())
			    || (clickedEntity.getScoreboardTags().contains("NoTrickyTransformation") && !clickedEntity.getScoreboardTags().contains("boss_ruten"))
			    || clickedEntity.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
			return true;
		}

		if (!player.hasPermission("monumenta.command.summontrickycreeper")) {
			return true;
		}

        // optionally, remove this block & the and statement for ru'ten above once Creeperween ends (2024 seasonal advancement)
		if (clickedEntity.getScoreboardTags().contains("boss_ruten")) {
			if (DateUtils.getYear() != 2024) { // if not 2024, no advancement, but ru'ten doesn't blow up
				return true;
			}
			item.subtract(1);
			player.sendMessage(Component.text("Ru'Ten takes your candy, but doesn't eat it. Nice try - it doesn't fall for your Tricks.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			if (!AdvancementUtils.checkAdvancement(player, "monumenta:trophies/events/2024/halloween_tricky_ruten")) {
				AdvancementUtils.grantAdvancement(player, "monumenta:trophies/events/2024/halloween_tricky_ruten");
				if (!AdvancementUtils.checkAdvancement(player, "monumenta:trophies/events/2024/root")) {
					AdvancementUtils.grantAdvancement(player, "monumenta:trophies/events/2024/root");
				}
			}
			return true;
		}

		if (player.getScoreboardTags().contains("SQRacer")) {
			player.sendMessage(Component.text("You can't stop to feed the creepers during a race!", NamedTextColor.RED));
			return true;
		}

		try {
			BossManager.createBoss(null, creeper, "boss_halloween_creeper");
		} catch (Exception e) {
			plugin.getLogger().warning("Failed to create Tricky Creeper boss: " + e.getMessage());
		}

		// Consume the item
		item.subtract(1);

		Location loc = clickedEntity.getLocation();
		MMLog.info(player.getName() + " summoned Tricky Creeper at " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());

		return true;
	}

	@Override
	public boolean playerItemConsume(Plugin plugin, Player player, PlayerItemConsumeEvent event) {
		if (player == null || !InventoryUtils.testForItemWithName(event.getItem(), "Creeper's Delight", true)) {
			return true;
		}

		Location loc = player.getLocation();
		loc.getWorld().playSound(loc, Sound.ENTITY_CREEPER_PRIMED, SoundCategory.HOSTILE, 1.0f, 1.0f);
		PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.GLOWING, 4 * 60 * 20, 0));

		return true;
	}
}
