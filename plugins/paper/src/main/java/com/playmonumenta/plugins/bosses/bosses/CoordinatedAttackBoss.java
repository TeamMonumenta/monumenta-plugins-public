package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CoordinatedAttackBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_coordinatedattack";

	public static class Parameters extends BossParameters {
		@BossParam(help = "detection range of the ability")
		public int DETECTION = 40;
		@BossParam(help = "initial delay in ticks before this ability becomes active")
		public int DELAY = 30;
		@BossParam(help = "cooldown in ticks")
		public int COOLDOWN = 12 * 20;
		@BossParam(help = "if false, selects mobs around the target. if true, selects mobs around itself.")
		public boolean NEAR_SELF = true;
		@BossParam(help = "radius in which mobs can be selected from")
		public int TARGET_RADIUS = 20;
		@BossParam(help = "color the selected mobs will glow before jumping")
		public String COLOR = "gold";
		@BossParam(help = "delay before the selected mobs jump")
		public int WINDUP = 20;
		@BossParam(help = "scalar affecting the distance the jumping mobs will go")
		public double DISTANCE_SCALAR = 1;
		@BossParam(help = "scalar affecting the height the jumping mobs will reach")
		public double HEIGHT_SCALAR = 1;
		@BossParam(help = "maximum number of mobs that can be launched")
		public int AFFECTED_MOB_CAP = 4;
	}

	public final String IGNORE_TAG = "boss_coordinatedattack_ignore";

	public CoordinatedAttackBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = Parameters.getParameters(boss, identityTag, new Parameters());
		Spell spell = new Spell() {
			@Override
			public void run() {
				LivingEntity target = ((Mob) boss).getTarget();

				if (target instanceof Player mTarget) {
					if (mTarget.isDead() || !mTarget.isValid() || !mTarget.isOnline()) {
						return;
					}

					World world = mBoss.getWorld();
					Location targetLoc = target.getLocation();

					new PartialParticle(Particle.VILLAGER_ANGRY, mBoss.getEyeLocation(), 8, 0.3, 0.3, 0.3, 0).spawnAsEntityActive(mBoss);

					world.playSound(targetLoc, Sound.EVENT_RAID_HORN, SoundCategory.HOSTILE, 50f, 1.5f);
					new PartialParticle(Particle.VILLAGER_ANGRY, targetLoc, 20, 1, 0, 1, 0).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SPELL_WITCH, targetLoc, 20, 1, 0.5, 1, 0).spawnAsEntityActive(mBoss);

					List<LivingEntity> mobs;
					if (p.NEAR_SELF) {
						mobs = EntityUtils.getNearbyMobs(mBoss.getLocation(), p.TARGET_RADIUS);
					} else {
						mobs = EntityUtils.getNearbyMobs(targetLoc, p.TARGET_RADIUS);
					}
					mobs.removeIf(mob -> mob.getScoreboardTags().contains(IGNORE_TAG));
					Collections.shuffle(mobs);

					int i = 0;
					for (LivingEntity le : mobs) {
						if (le instanceof Mob mob && mob.hasLineOfSight(mTarget) && !AbilityUtils.isStealthed(mTarget)) {
							Set<String> tags = mob.getScoreboardTags();
							if (!tags.contains(identityTag) && !tags.contains(DelvesManager.AVOID_MODIFIERS) && !tags.contains(AbilityUtils.IGNORE_TAG) && !EntityUtils.isBoss(mob)) {
								PotionUtils.applyColoredGlowing(identityTag, mob, NamedTextColor.NAMES.valueOr(p.COLOR, NamedTextColor.RED), p.WINDUP);

								// make mob immune to other coordinated attacks for a short time
								mob.addScoreboardTag(IGNORE_TAG);
								Bukkit.getScheduler().runTaskLater(mPlugin, () -> mob.removeScoreboardTag(IGNORE_TAG), 30);

								new BukkitRunnable() {
									@Override
									public void run() {
										Location locTarget = mTarget.getLocation();

										mob.setTarget(mTarget);
										Location loc = mob.getLocation();
										double distance = loc.distance(locTarget);
										Vector velocity = locTarget.clone().subtract(loc).toVector().multiply(0.19 * p.DISTANCE_SCALAR);
										velocity.setY(velocity.getY() * 0.5 + distance * 0.08 * p.HEIGHT_SCALAR);
										mob.setVelocity(velocity);

										new PartialParticle(Particle.CLOUD, loc, 10, 0.1, 0.1, 0.1, 0.1).spawnAsEntityActive(mBoss);
										new PartialParticle(Particle.VILLAGER_ANGRY, mob.getEyeLocation(), 8, 0.3, 0.3, 0.3, 0).spawnAsEntityActive(mBoss);
									}
								}.runTaskLater(mPlugin, p.WINDUP);

								i++;
								if (i >= p.AFFECTED_MOB_CAP) {
									break;
								}
							}
						}
					}
				}
			}

			@Override
			public int cooldownTicks() {
				return p.COOLDOWN;
			}

			@Override
			public boolean canRun() {
				LivingEntity target = ((Mob) mBoss).getTarget();
				List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mBoss.getLocation(), p.TARGET_RADIUS);
				mobs.removeIf(mob -> mob.getScoreboardTags().contains(IGNORE_TAG));

				return target != null && target.hasLineOfSight(mBoss) && !mobs.isEmpty();
			}

		};

		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}
