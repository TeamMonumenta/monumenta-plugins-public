package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
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
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class CoordinatedAttackOnDeathBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_coordinatedattackondeath";

	public static class Parameters extends BossParameters {
		@BossParam(help = "detection range of the ability")
		public int DETECTION = 40;
		@BossParam(help = "mobs closer than X blocks to the player will not be flung")
		public double PLAYER_RADIUS = 5;
		@BossParam(help = "mobs more than X blocks from the mob will not be flung")
		public double MOB_RADIUS = 40;
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
	// Assuming that anything excluded from Bloodthirsty is equally excluded from Bloodlust
	public final int COOLDOWN_PER_MOB = 6 * 20;

	public CoordinatedAttackOnDeathBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), -1, null);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (event != null) {
			LivingEntity boss = event.getEntity();
			Parameters mParam = Parameters.getParameters(boss, identityTag, new Parameters());
			LivingEntity target = ((Mob) boss).getTarget();
			if (target == null) {
				target = mBoss.getKiller();
			}
			if (target instanceof Player playerTarget) {
				if (playerTarget.isDead() || !playerTarget.isOnline()) {
					return;
				}

				World world = mBoss.getWorld();
				Location targetLoc = target.getLocation();

				List<LivingEntity> mobs;
				mobs = EntityUtils.getNearbyMobs(mBoss.getLocation(), mParam.MOB_RADIUS);
				List<LivingEntity> closemobs;
				closemobs = EntityUtils.getNearbyMobs(targetLoc, mParam.PLAYER_RADIUS);
				mobs.removeIf(mob -> mob.getScoreboardTags().contains(IGNORE_TAG));
				mobs.removeAll(closemobs);
				Collections.shuffle(mobs);

				int i = 0;
				for (LivingEntity le : mobs) {
					if (le instanceof Mob mob && mob.hasLineOfSight(playerTarget) && !AbilityUtils.isStealthed(playerTarget) && !mob.isInsideVehicle()) {
						Set<String> tags = mob.getScoreboardTags();
						if (!tags.contains(DelvesManager.AVOID_MODIFIERS) && !tags.contains(IGNORE_TAG) && !EntityUtils.isBoss(mob)) {
							GlowingManager.startGlowing(mob, NamedTextColor.NAMES.valueOr(mParam.COLOR, NamedTextColor.RED), mParam.WINDUP, GlowingManager.BOSS_SPELL_PRIORITY - 1);

							// make mob immune to other coordinated attacks for a short time
							mob.addScoreboardTag(IGNORE_TAG);
							Bukkit.getScheduler().runTaskLater(mPlugin, () -> mob.removeScoreboardTag(IGNORE_TAG), COOLDOWN_PER_MOB);


							new BukkitRunnable() {
								@Override
								public void run() {
									if (!AbilityUtils.isStealthed(playerTarget)) {
										mob.setTarget(playerTarget);
									}
									Location loc = mob.getLocation();
									double distance = loc.distance(targetLoc);
									Vector velocity = targetLoc.clone().subtract(loc).toVector().multiply(0.19 * mParam.DISTANCE_SCALAR);
									velocity.setY(velocity.getY() * 0.5 + distance * 0.08 * mParam.HEIGHT_SCALAR);

									if (mob instanceof Creeper) {
										velocity.multiply(0.66);
									}

									mob.setVelocity(velocity);

									new PartialParticle(Particle.CLOUD, loc, 10, 0.1, 0.1, 0.1, 0.1).spawnAsEntityActive(mBoss);
									new PartialParticle(Particle.VILLAGER_ANGRY, mob.getEyeLocation(), 8, 0.3, 0.3, 0.3, 0).spawnAsEntityActive(mBoss);
								}
							}.runTaskLater(mPlugin, mParam.WINDUP);

							i++;
							if (i >= mParam.AFFECTED_MOB_CAP) {
								break;
							}
						}
					}
				}

				if (i > 0) {
					// Only show the effects if any mobs were valid to launch
					new PartialParticle(Particle.VILLAGER_ANGRY, mBoss.getEyeLocation(), 8, 0.3, 0.3, 0.3, 0).spawnAsEntityActive(mBoss);

					world.playSound(targetLoc, Sound.EVENT_RAID_HORN, SoundCategory.HOSTILE, 50f, 1.5f);
					new PartialParticle(Particle.VILLAGER_ANGRY, targetLoc, 20, 1, 0, 1, 0).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SPELL_WITCH, targetLoc, 20, 1, 0.5, 1, 0).spawnAsEntityActive(mBoss);
				}
			}
		}
	}
}
