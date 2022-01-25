package com.playmonumenta.plugins.abilities.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.ShadeParticleBoss;
import com.playmonumenta.plugins.bosses.bosses.ShadePossessedBoss;
import com.playmonumenta.plugins.bosses.bosses.TwistedEventBoss;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Flying;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Witch;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Twisted extends DelveModifier {

	private static final double EVENT_ATTEMPT_CHANCE = 0.2;
	private static final int EFFECT_RANGE = 16;

	private static final String AFFECTED_MOB_TAG = "ShadeAffectedMob";

	private static final String SHADE_OF_WRATH = "ShadeofWrath";
	private static final String SHADE_OF_GREED = "ShadeofGreed";
	private static final String SHADE_OF_DESPAIR = "ShadeofDespair";
	private static final String SHADE_OF_CORRUPTION_TAG = "ShadeofCorruption";

	private static final String PERCENT_SPEED_MODIFIER_NAME = "TwistedCorruptedPercentSpeedModifier";
	private static final double PERCENT_SPEED_MODIFIER = 0.25;
	private static final double CORRUPTION_DAMAGE_DEALT_MULTIPLIER = 1.5;
	private static final double CORRUPTION_DAMAGE_RECEIVED_MULTIPLIER = 0.5;

	public static final String DESCRIPTION = "Something, everything is wrong...";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Worth 5 Depth Points.",
				"Cannot be randomly assigned by Entropy.",
				"",
				ChatColor.DARK_RED + "Select at your own demise."
			}
	};

	public Twisted(Plugin plugin, @Nullable Player player) {
		super(plugin, player, Modifier.TWISTED);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity enemy) {
		Set<String> tags = event.getDamagee().getScoreboardTags();
		if (tags != null && tags.contains(SHADE_OF_CORRUPTION_TAG)) {
			event.setDamage(event.getDamage() * CORRUPTION_DAMAGE_RECEIVED_MULTIPLIER);
		}
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		Set<String> tags = source.getScoreboardTags();
		if (tags != null && tags.contains(SHADE_OF_CORRUPTION_TAG)) {
			event.setDamage(event.getDamage() * CORRUPTION_DAMAGE_DEALT_MULTIPLIER);
		}
	}

	@Override
	protected void applyModifiers(LivingEntity mob, SpawnerSpawnEvent event) {
		if (FastUtils.RANDOM.nextDouble() < EVENT_ATTEMPT_CHANCE) {
			mob.addScoreboardTag(TwistedEventBoss.identityTag);
		}
	}

	public static void runEvent(LivingEntity mob) {
		if (mob instanceof Mob) {
			Plugin plugin = Plugin.getInstance();
			int random = FastUtils.RANDOM.nextInt(4);
			List<LivingEntity> mobs = getAffectedMobs(mob);

			if (random == 0) {
				shadeOfWrath(plugin, mobs);
			} else if (random == 1) {
				shadeOfDeath(mobs);
			} else if (random == 2) {
				shadeOfDespair(plugin, mobs);
			} else {
				shadeOfCorruption(plugin, mobs);
			}
		}
	}

	private static void shadeOfWrath(Plugin plugin, List<LivingEntity> mobs) {
		for (LivingEntity mob : mobs) {
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					if (!mob.isValid() || mob.isDead()) {
						this.cancel();
					}

					mob.setNoDamageTicks(0);
					mob.damage(0.0001);

					mTicks += 2;
					if (mTicks > 60) {
						mob.setHealth(0);

						if (FastUtils.RANDOM.nextBoolean()) {
							LibraryOfSoulsIntegration.summon(mob.getLocation(), SHADE_OF_WRATH);
						} else {
							LibraryOfSoulsIntegration.summon(mob.getLocation(), SHADE_OF_GREED);
						}

						this.cancel();
					}
				}
			}.runTaskTimer(plugin, 0, 2);
		}
	}

	private static void shadeOfDeath(List<LivingEntity> mobs) {
		for (LivingEntity mob : mobs) {
			mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.3f, 0.5f);

			try {
				BossManager.createBoss(null, mob, ShadePossessedBoss.identityTag);
			} catch (Exception ex) {
				Plugin.getInstance().getLogger().severe("Failed to create ShadePossessedBoss: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	private static void shadeOfDespair(Plugin plugin, List<LivingEntity> mobs) {
		for (LivingEntity mob : mobs) {
			World world = mob.getWorld();
			Location loc = mob.getLocation();
			world.playSound(loc, Sound.ENTITY_GHAST_SCREAM, 0.5f, 0.5f);
			world.playSound(loc, Sound.ENTITY_HORSE_DEATH, 0.2f, 0.25f);

			new BukkitRunnable() {
				@Override
				public void run() {
					world.playSound(loc, Sound.ENTITY_GHAST_DEATH, 1f, 0.5f);
					world.spawnParticle(Particle.SMOKE_LARGE, loc, 20, 0.2, 0.3, 0.2, 0.1);

					LibraryOfSoulsIntegration.summon(mob.getLocation(), SHADE_OF_DESPAIR);
				}
			}.runTaskLater(plugin, 60);
		}
	}

	private static void shadeOfCorruption(Plugin plugin, List<LivingEntity> mobs) {
		for (LivingEntity mob : mobs) {
			World world = mob.getWorld();
			Location loc = mob.getLocation();
			world.playSound(loc, Sound.ENTITY_HUSK_CONVERTED_TO_ZOMBIE, 0.5f, 0.5f);
			world.playSound(loc, Sound.ENTITY_HUSK_DEATH, 0.5f, 0.5f);

			try {
				BossManager.createBoss(null, mob, ShadeParticleBoss.identityTag);
			} catch (Exception ex) {
				Plugin.getInstance().getLogger().severe("Failed to create ShadeParticleBoss: " + ex.getMessage());
				ex.printStackTrace();
			}

			new BukkitRunnable() {
				@Override
				public void run() {
					world.spawnParticle(Particle.SMOKE_LARGE, loc, 20, 0.2, 0.3, 0.2, 0.1);
					world.playSound(loc, Sound.ENTITY_HUSK_AMBIENT, 1f, 0.5f);

					// These mobs don't have visible equipment and are obnoxious when invisible
					if (!(mob instanceof Creeper || mob instanceof Spider || mob instanceof Witch
						|| mob instanceof Enderman || mob instanceof IronGolem)) {
						PotionUtils.applyPotion(null, mob,
						                        new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 3600 * 100, 0, false, false));
					}

					EntityUtils.addAttribute(mob, Attribute.GENERIC_MOVEMENT_SPEED,
					                         new AttributeModifier(PERCENT_SPEED_MODIFIER_NAME, PERCENT_SPEED_MODIFIER, Operation.MULTIPLY_SCALAR_1));
				}
			}.runTaskLater(plugin, 60);
		}
	}

	private static List<LivingEntity> getAffectedMobs(LivingEntity sourceMob) {
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(sourceMob.getEyeLocation(), EFFECT_RANGE);

		Iterator<LivingEntity> iter = mobs.iterator();
		while (iter.hasNext()) {
			LivingEntity mob = iter.next();

			if (!sourceMob.hasLineOfSight(mob) || mob instanceof Flying) {
				iter.remove();
				continue;
			}

			Set<String> tags = mob.getScoreboardTags();
			if (tags != null && (tags.contains(ShadeParticleBoss.identityTag)
							|| tags.contains(AFFECTED_MOB_TAG)
							|| tags.contains(DelveModifier.AVOID_MODIFIERS)
							|| EntityUtils.isElite(mob)
							|| EntityUtils.isBoss(mob))) {
				iter.remove();
				continue;
			}

			mob.addScoreboardTag(AFFECTED_MOB_TAG);
		}

		return mobs;
	}

}
