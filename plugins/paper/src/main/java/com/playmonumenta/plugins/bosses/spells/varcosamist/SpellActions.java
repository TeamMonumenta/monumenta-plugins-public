package com.playmonumenta.plugins.bosses.spells.varcosamist;

import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.SpellPlayerAction;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;

import net.md_5.bungee.api.ChatColor;

public class SpellActions {
	//Not technically a spell - just putting the repeated actions in a seperate class
	public static SpellPlayerAction getTooLowAction(LivingEntity boss, Location center) {
		return new SpellPlayerAction(boss, 50, (player, tick) -> {
			if (player.getLocation().getBlock().isLiquid() || player.getLocation().getBlockY() <= center.getY() - 3 && tick % 20 == 0) {
				Vector velocity = player.getVelocity();
				BossUtils.bossDamagePercent(boss, player, 0.025);
				player.setVelocity(velocity);
				// Only show the message every 30s
				if (!MetadataUtils.happenedInRecentTicks(player, "PlayerIntoDepthsVarcosaMetakey", 600)) {
					MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, "PlayerIntoDepthsVarcosaMetakey"); // Mark this tick
					player.sendMessage(ChatColor.RED + "Into the depths with ye! The deep gods take yer soul!");
				}
			}
		});
	}

	public static SpellPlayerAction getTooHighAction(LivingEntity boss, Location center) {
		return new SpellPlayerAction(boss, 50, (player, tick) -> {
			if (player.getLocation().getBlockY() >= center.getY() + 3 && tick % 20 == 0) {
				Vector velocity = player.getVelocity();
				BossUtils.bossDamagePercent(boss, player, 0.025);
				player.setVelocity(velocity);
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
					if (!(e instanceof Player) && (e.getLocation().getBlock().getType() == Material.WATER  || e.getLocation().getBlockY() <= center.getY() - 3)) {
						e.teleport(center);
					}
				}
			}
		};
	}
}
