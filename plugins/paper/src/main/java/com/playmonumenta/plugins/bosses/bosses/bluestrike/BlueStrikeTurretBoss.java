package com.playmonumenta.plugins.bosses.bosses.bluestrike;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Shulker;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class BlueStrikeTurretBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_bluestriketurretboss";
	public static final int CHARGE_DURATION = 7 * 20; // 7 seconds charging
	public @Nullable LivingEntity mTarget;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BlueStrikeTurretBoss(plugin, boss);
	}

	public BlueStrikeTurretBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Team redTeam = ScoreboardUtils.getExistingTeamOrCreate("Red", NamedTextColor.RED);
		redTeam.addEntry(boss.getUniqueId().toString());
		mBoss.setGlowing(true);

		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mBoss.getLocation(), 100, EnumSet.of(EntityType.VILLAGER));
		Collections.shuffle(mobs);

		// Search for npc target that is the closest.
		double minDistance = 9999;
		for (LivingEntity mob : mobs) {
			if (mob.getScoreboardTags().contains(BlueStrikeDaggerCraftingBoss.identityTag)) {
				double distance = mBoss.getLocation().distance(mob.getLocation());
				if (distance < minDistance) {
					mTarget = mob;
					minDistance = distance;
				}
			}
		}

		if (mTarget == null) {
			MMLog.warning("TurretNPCBoss: Didn't find NPC! (Likely bug)");
			return;
		}

		List<Spell> passiveList = List.of();

		mBoss.setAI(false);
		((Shulker) mBoss).setPeek(1.0f);
		((Shulker) mBoss).setAttachedFace(BlockFace.DOWN);
		super.constructBoss(SpellManager.EMPTY, passiveList, 100, null);
		chargeLaser();
	}

	public void chargeLaser() {
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTarget == null) {
					this.cancel();
					return;
				}

				// Shoot Laser!
				Location startLocation = mBoss.getEyeLocation();
				Location endLocation = mTarget.getLocation().add(0, mTarget.getEyeHeight() * 3 / 5, 0);

				World world = mBoss.getWorld();
				BoundingBox movingLaserBox = BoundingBox.of(startLocation, 0.5, 0.5, 0.5);
				Vector vector = new Vector(
					endLocation.getX() - startLocation.getX(),
					endLocation.getY() - startLocation.getY(),
					endLocation.getZ() - startLocation.getZ()
				);

				// Playsound
				if (mTicks % 8 == 0) {
					world.playSound(mTarget.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (mTicks / 100f) * 1.5f);
				} else if (mTicks % 8 == 2) {
					world.playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (mTicks / 100f) * 1.5f);
				} else if (mTicks % 8 == 4) {
					world.playSound(mTarget.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.HOSTILE, 2, 0.5f + (mTicks / 100f) * 1.5f);
				} else if (mTicks % 8 == 6) {
					world.playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (mTicks / 100f) * 1.5f);
				}

				// Particles
				LocationUtils.travelTillMaxDistance(
					world,
					movingLaserBox,
					startLocation.distance(endLocation),
					vector,
					0.2,
					(Location loc) -> {
						new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, .02, .02, .02, 0).spawnAsEnemy();
						new PartialParticle(Particle.SMOKE_LARGE, loc, 1, .02, .02, .02, 0).spawnAsEnemy();
						new PartialParticle(Particle.SPELL_MOB, loc, 1, .02, .02, .02, 1).spawnAsEnemy();
					},
					1,
					6
				);

				if (!mBoss.isValid() || mBoss.isDead()) {
					this.cancel();
				}

				// Damage NPC
				if (mTicks >= CHARGE_DURATION) {
					// When Laser actually lands.
					BlueStrikeDaggerCraftingBoss targetAbility = BossUtils.getBossOfClass(mTarget, BlueStrikeDaggerCraftingBoss.class);
					if (targetAbility != null) {
						new PartialParticle(Particle.EXPLOSION_NORMAL, mTarget.getLocation(), 10, 1, 1, 1).spawnAsEnemy();
						mBoss.getWorld().playSound(mTarget.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0.8f);
						targetAbility.takeDamage();
					}
					mBoss.remove();
					this.cancel();
				}

				// Increment Ticks
				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}
}
