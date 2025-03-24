package com.playmonumenta.plugins.bosses.spells.varcosamist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.SpellPlayerAction;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;
import java.util.ArrayList;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellActions {
	// Not technically a spell - just putting the repeated actions in a seperate class
	public static SpellPlayerAction getTooLowAction(LivingEntity boss, Location center) {
		return new SpellPlayerAction(boss, 50, (player, tick) -> {
			if (tick % 10 == 0 && (player.getLocation().getBlock().isLiquid() || player.getLocation().getBlockY() <= center.getY() - 3)) {
				damagePlayer(boss, player);
			}
		});
	}

	public static SpellPlayerAction getTooHighAction(LivingEntity boss, Location center) {
		double arenaSurfaceY = center.getY(); // This should be 52.0 based on the armour stand placement
		return new SpellPlayerAction(boss, 50, (player, tick) -> {
			double playerY = player.getLocation().getY();

			// Assume player is standing on ground/cheese pillar or a climbable block
			int yThreshold = 2;

			// If truly in the air (not on ground and not climbing - like the Thunder Step requirement),
			// use a relaxed 5-block threshold
			if (PlayerUtils.isFreeFalling(player)) {
				yThreshold = 5;
			}

			if (tick % 10 == 0 && playerY >= arenaSurfaceY + yThreshold) {
				damagePlayer(boss, player);
			}
		});
	}

	public static BukkitRunnable getTeleportEntityRunnable(LivingEntity boss, Location center) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				if (boss.isDead() || !boss.isValid()) {
					this.cancel();
				}
				Collection<LivingEntity> entities = center.getNearbyLivingEntities(25, 20);
				for (LivingEntity e : entities) {
					if (!(e instanceof Player) && (e.getLocation().getBlock().getType() == Material.WATER || e.getLocation().getBlockY() <= center.getY() - 3)) {
						e.teleport(center);
					}
				}
			}
		};
	}

	private static void damagePlayer(LivingEntity boss, Player player) {
		final String ANTIHEAL_SRC = "VarcosaOOBAntiHeal";

		// Deals flat and percent damage to prevent players from surviving for long periods
		Plugin.getInstance().mEffectManager.addEffect(player, ANTIHEAL_SRC, new PercentHeal(3 * 20, -1.0));
		BossUtils.bossDamagePercent(boss, player, 0.1, null, false, null, false, new ArrayList<>());
		BossUtils.bossDamagePercent(boss, player, 2.0, null, true, null, false, new ArrayList<>());
		if (!MetadataUtils.happenedInRecentTicks(player, "PlayerIntoDepthsVarcosaMetakey", 600)) {
			MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, "PlayerIntoDepthsVarcosaMetakey"); // Mark this tick
			player.sendMessage(Component.text("Into the depths with ye! The deep gods take yer soul!", NamedTextColor.RED));
		}
	}
}
