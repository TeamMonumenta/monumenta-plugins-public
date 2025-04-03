package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.itemstats.enchantments.Snowy;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.Optional;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

public class WinterListener implements Listener {
	private static final String SNOWBALLS_HIT_SCORE = "SnowballsHit";

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onProjectileHit(ProjectileHitEvent event) {
		Projectile projectile = event.getEntity();
		if (!(projectile.getShooter() instanceof Player player) || !player.hasPermission("monumenta.event.winterpvp")) {
			return;
		}
		Entity entity = event.getHitEntity();
		if (!(entity instanceof Player target) || player.equals(target) || target.getGameMode() != GameMode.SURVIVAL) {
			return;
		}
		if (target.getScoreboardTags().contains("SQRacer")) {
			return;
		}
		Optional<Integer> modeIndex = Snowy.getProjectileMode(projectile);
		if (modeIndex.isEmpty()) {
			return;
		}
		Snowy.SnowballMode mode = Snowy.getMode(modeIndex.get());
		// disallow in adventure zones
		if (ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.ADVENTURE_MODE)
			|| ZoneUtils.hasZoneProperty(target, ZoneUtils.ZoneProperty.ADVENTURE_MODE)) {
			return;
		}
		// disallow if target does not have Snowy item
		if (!Snowy.hasEnchantInInventory(target)) {
			return;
		}
		mode.applyVelocity(target, projectile.getVelocity());
		player.playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.75f, 1.25f);
		target.playSound(target, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.75f, 2f);
		ScoreboardUtils.addScore(player, SNOWBALLS_HIT_SCORE, 1);
	}
}
