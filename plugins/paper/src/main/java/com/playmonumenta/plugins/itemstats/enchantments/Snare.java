package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class Snare implements Enchantment {

	private static final double CHANCE = 0.1;
	private static final int ARM_SNARE_TIME = 15;
	private static final int MAX_SNARE_TIME = 20 * 5;
	static final int ROOT_DURATION = 20 * 3;
	static final double SNARE_RADIUS = 0.35;
	static final double SLOWNESS_AMPLIFIER = 1;

	private static final String DAMAGED_THIS_TICK_METADATA = "SnareThisTick";
	private static final String DAMAGE_DEALT_METADATA = "SnareDamageDealt";

	private static final EnumSet<DamageEvent.DamageType> ACTIVATION_DAMAGE_TYPES = EnumSet.of(
		DamageEvent.DamageType.MELEE,
		DamageEvent.DamageType.PROJECTILE
	);

	@Override
	public String getName() {
		return "Snare";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SNARE;
	}

	@Override
	public void onKill(Plugin plugin, Player player, double level, EntityDeathEvent event, LivingEntity enemy) {
		final Location loc = enemy.getLocation();
		EntityDamageEvent e = enemy.getLastDamageCause();

		if (e != null && (MetadataUtils.happenedThisTick(enemy, DAMAGED_THIS_TICK_METADATA) && enemy.hasMetadata(DAMAGE_DEALT_METADATA))) {
			if (FastUtils.RANDOM.nextDouble() < (CHANCE * level)) {
				placeSnare(loc, player, plugin);
			}
		}
	}

	//onKill, roll chance. If successful, placeSnare
	public void placeSnare(Location loc, Player player, Plugin plugin) {

		new BukkitRunnable() {
			final Player mPlayer = player;
			final Location mLoc = loc;
			final Plugin mPlugin = plugin;
			final World mWorld = loc.getWorld();
			int mTicks = 0;

			@Override
			public void run() {
				//cosmetic & visuals
				if (mTicks == 1) {
					mWorld.playSound(loc, Sound.ITEM_CROSSBOW_LOADING_START, SoundCategory.PLAYERS, 0.9f, 0.85f);
				}
				if (mTicks == 20) {
					mWorld.playSound(loc, Sound.ITEM_CROSSBOW_LOADING_END, SoundCategory.PLAYERS, 0.9f, 0.85f);
					mWorld.playSound(loc, Sound.ITEM_CROSSBOW_LOADING_END, SoundCategory.PLAYERS, 0.9f, 0.65f);
				}
				if (mTicks % 6 == 0) { //armed
					new PPCircle(Particle.CRIT, mLoc.clone().add(0, 0.1, 0), SNARE_RADIUS)
						.count(10)
						.delta(0, 1, 0).directionalMode(true).extraRange(0.15, 0.2)
						.spawnAsPlayerActive(mPlayer);
				}
				//hitbox functional stuff
				Hitbox hitbox = new Hitbox.UprightCylinderHitbox(loc.clone().subtract(0, SNARE_RADIUS, 0), SNARE_RADIUS * 2, SNARE_RADIUS);
				List<LivingEntity> hitMobs = hitbox.getHitMobs();
				hitMobs.removeIf(mob -> ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG));
				if (mTicks > ARM_SNARE_TIME && !hitMobs.isEmpty()) {
					this.cancel();
				}

				// just erupt if we wait too long
				if (mTicks > MAX_SNARE_TIME) {
					this.cancel();
				}
				mTicks++;
			}

			@Override
			public synchronized void cancel() {
				doEruption(mLoc, mPlayer, mPlugin);
				super.cancel();
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (ACTIVATION_DAMAGE_TYPES.contains(event.getType())) {
			double damage = event.getDamage();
			if (MetadataUtils.checkOnceThisTick(plugin, enemy, DAMAGED_THIS_TICK_METADATA) && enemy.hasMetadata(DAMAGE_DEALT_METADATA) && enemy.getMetadata(DAMAGE_DEALT_METADATA).getFirst().asDouble() > damage) {
				return;
			}
			enemy.setMetadata(DAMAGE_DEALT_METADATA, new FixedMetadataValue(plugin, damage));
		}
	}

	public void doEruption(Location loc, Player mPlayer, Plugin mPlugin) {
		final World mWorld = loc.getWorld();
		//audio, visuals
		mWorld.playSound(loc, Sound.ITEM_CROSSBOW_HIT, SoundCategory.PLAYERS, 0.9f, 1.4f);
		mWorld.playSound(loc, Sound.ITEM_CROSSBOW_HIT, SoundCategory.PLAYERS, 0.9f, 0.9f);
		mWorld.playSound(loc, Sound.ITEM_CROSSBOW_HIT, SoundCategory.PLAYERS, 0.9f, 0.55f);
		mWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, SoundCategory.PLAYERS, 0.7f, 1.33f);

		new PartialParticle(Particle.CRIT, loc, 25)
			.delta(0.5)
			.extraRange(0.1, 0.2)
			.spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT, loc, 15)
			.delta(SNARE_RADIUS / 2)
			.spawnAsPlayerActive(mPlayer);

		// actual effect
		for (LivingEntity mob : new Hitbox.UprightCylinderHitbox(
			loc.clone().subtract(0, SNARE_RADIUS, 0), SNARE_RADIUS * 2, SNARE_RADIUS).getHitMobs()) {

			if (!EntityUtils.isBoss(mob) && !EntityUtils.isCCImmuneMob(mob)) {
				EntityUtils.applySlow(mPlugin, ROOT_DURATION, SLOWNESS_AMPLIFIER, mob);
				MovementUtils.knockAway(loc, mob, 0.1f, 0.1f, true);

				//even more visuals on hit
				new PartialParticle(Particle.CRIT, mob.getEyeLocation(), 55)
					.delta(0.2, 0.2, 0.2)
					.extraRange(0.4, 1.4)
					.spawnAsPlayerActive(mPlayer);

				new PPCircle(Particle.BUBBLE_POP, mob.getEyeLocation(), 0.4)
					.countPerMeter(5)
					.delta(0.1, 0.3, 0.1).extra(0.6)
					.spawnAsPlayerActive(mPlayer);
			}
		}
	}
}
