package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

public class SpellShadowRealm extends Spell {

	private int mT = 0;
	private int mRange;
	private Location mCenter;

	public SpellShadowRealm(Location loc, int r) {
		mCenter = loc;
		mRange = r;
	}

	//tp distance = 42
	//shadow realm roof = mCenter Y - 8
	//check distance = 24
	@Override
	public void run() {
		mT--;
		Location shadowloc = mCenter.clone();
		shadowloc.setY(mCenter.getY() - 42);

		if (mT <= 0) {
			mT = 4;
			List<Player> p = Lich.playersInRange(shadowloc, mRange, true);
			p.removeIf(pl -> pl.getLocation().getY() >= mCenter.getY() - 8);
			for (Player player : p) {
				if (player.getLocation().getBlock().getType() != Material.AIR) {
					player.teleport(shadowloc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
				}
				if (player.getLocation().getY() < mCenter.getY() - 8 && player.getLocation().distance(shadowloc) < mRange) {
					AbilityUtils.increaseHealingPlayer(player, 20 * 2, -0.4, "Lich");
				}
				if (!SpellDimensionDoor.getShadowed().contains(player) && player.getLocation().getY() < mCenter.getY() - 8) {
					player.teleport(player.getLocation().add(0, 43, 0), PlayerTeleportEvent.TeleportCause.UNKNOWN);
				}
			}
			List<Player> shadowedplayers = Lich.playersInRange(shadowloc, mRange, true);
			shadowedplayers.removeIf(pl -> pl.getLocation().getY() > Lich.getLichSpawn().getY() - 10);
			if (shadowedplayers.size() == 0) {
				SpellDimensionDoor.clearShadowed();
				List<LivingEntity> mobs = EntityUtils.getNearbyMobs(shadowloc, 55);
				mobs.removeIf(e -> e.getType() == EntityType.MAGMA_CUBE || e.getType() == EntityType.ARMOR_STAND ||
						e.getScoreboardTags().contains("Boss") || e.getLocation().getY() >= mCenter.getY() - 8);
				for (LivingEntity m : mobs) {
					m.setHealth(0);
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
