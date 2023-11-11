package com.playmonumenta.plugins.bosses.spells.exalted;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellFlamingHavoc extends Spell {

	private static final String SPELL_NAME = "Flaming Havoc";
	private static final String TARGET_TAG = "HavocTarget";

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;

	public SpellFlamingHavoc(Plugin plugin, LivingEntity boss, int range) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
	}

	@Override public void run() {
		World world = mBoss.getWorld();
		// get valid targets (totem mobs)
		List<LivingEntity> mobList = EntityUtils.getNearbyMobs(mBoss.getLocation(), mRange);
		mobList.removeIf(le -> !le.getScoreboardTags().contains(TARGET_TAG));

		// draw vector line per mob
		for (LivingEntity target : mobList) {
			Vector dir = LocationUtils.getDirectionTo(target.getLocation(), mBoss.getLocation()).multiply(0.25);
			Location startLoc = mBoss.getLocation().add(0, 0.125, 0);

			BukkitRunnable drawLine = new BukkitRunnable() {
				final double mMaxActionLimit = 500.0;
				final double mStepCount = mMaxActionLimit / mobList.size();
				int mTotalAction = 0;
				final BoundingBox mBox = BoundingBox.of(startLoc, 0.4, 0.4, 0.4);
				@Override public void run() {
					for (int action = 0; action < mStepCount; action++) {
						mTotalAction++;
						mBox.shift(dir.clone());
						Location bLoc = mBox.getCenter().toLocation(world);
						new PartialParticle(Particle.FLAME, bLoc, 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
						for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), mRange, true)) {
							if (player.getBoundingBox().overlaps(mBox) && player.getNoDamageTicks() <= 0) {
								world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.HOSTILE, 1f, 1f);
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, 36, null, false, true, SPELL_NAME);
								MovementUtils.knockAway(bLoc, player, 0.3f, 0.1f);
								player.setFireTicks(20 * 5);
							}
						}

						double distance = mBoss.getLocation().distance(target.getLocation());
						if (mTotalAction / 4.0 >= distance) {
							this.cancel();
							break;
						}
					}
					world.playSound(mBoss.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.HOSTILE, 3f, 1f);
				}
			};
			mActiveRunnables.add(drawLine);
			drawLine.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override public int cooldownTicks() {
		return 1;
	}
}
