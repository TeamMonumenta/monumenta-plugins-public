package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class MelancholicLament extends Ability {

	private static final int DURATION = 20 * 8;
	private static final double WEAKEN_EFFECT_1 = 0.2;
	private static final double WEAKEN_EFFECT_2 = 0.3;
	private static final int COOLDOWN = 20 * 16;
	private static final int RADIUS = 8;
	private static final int CLEANSE_REDUCTION = 20 * 10;
	private static final int ENHANCE_RADIUS = 16;
	private static final double ENHANCE_DAMAGE = .025;
	private static final String ENHANCE_EFFECT_NAME = "LamentDamage";
	private static final String ENHANCE_EFFECT_PARTICLE_NAME = "LamentParticle";
	private static final int ENHANCE_EFFECT_DURATION = 20;
	private static final int ENHANCE_MAX_MOBS = 6;
	private static final EnumSet<DamageEvent.DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(DamageEvent.DamageType.MELEE);
	public static final String CHARM_RADIUS = "Melancholic Lament Radius";
	public static final String CHARM_COOLDOWN = "Melancholic Lament Cooldown";
	public static final String CHARM_WEAKNESS = "Melancholic Lament Weakness Amplifier";
	public static final String CHARM_RECOVERY = "Melancholic Lament Negative Effect Recovery";

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(235, 235, 224), 1.0f);

	public static final AbilityInfo<MelancholicLament> INFO =
		new AbilityInfo<>(MelancholicLament.class, "Melancholic Lament", MelancholicLament::new)
			.linkedSpell(ClassAbility.MELANCHOLIC_LAMENT)
			.scoreboardId("Melancholic")
			.shorthandName("MLa")
			.actionBarColor(TextColor.color(235, 235, 224))
			.descriptions(
				("Press the swap key while sneaking and holding a scythe to recite a haunting song, " +
					 "causing all mobs within %s blocks to target the user and afflicting them with %s%% Weaken for %s seconds. Cooldown: %ss.")
					.formatted(RADIUS, StringUtils.multiplierToPercentage(WEAKEN_EFFECT_1), StringUtils.ticksToSeconds(DURATION), StringUtils.ticksToSeconds(COOLDOWN)),
				"Increase the Weaken to %s%% and decrease the duration of all negative potion effects on players in the radius by %ss."
					.formatted(StringUtils.multiplierToPercentage(WEAKEN_EFFECT_2), StringUtils.ticksToSeconds(CLEANSE_REDUCTION)),
				"For %ss after casting this ability, you and your allies in a %s block radius gain +%s%% melee damage for each mob in that same radius targeting you (capped at %s mobs)."
					.formatted(StringUtils.ticksToSeconds(DURATION), ENHANCE_RADIUS, StringUtils.multiplierToPercentage(ENHANCE_DAMAGE), ENHANCE_MAX_MOBS))
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", MelancholicLament::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(new ItemStack(Material.GHAST_TEAR, 1));

	private final double mWeakenEffect;

	private int mEnhancementBonusDamage;

	public MelancholicLament(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mWeakenEffect = CharmManager.getLevelPercentDecimal(player, CHARM_WEAKNESS) + (isLevelOne() ? WEAKEN_EFFECT_1 : WEAKEN_EFFECT_2);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();

		world.playSound(loc, Sound.ENTITY_GHAST_SCREAM, SoundCategory.PLAYERS, 0.6f, 0.2f);
		world.playSound(loc, Sound.ENTITY_GHAST_SCREAM, SoundCategory.PLAYERS, 0.6f, 0.4f);
		world.playSound(loc, Sound.ENTITY_GHAST_SCREAM, SoundCategory.PLAYERS, 0.6f, 0.6f);

		new PartialParticle(Particle.REDSTONE, loc, 300, 8, 8, 8, 0.125, COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.ENCHANTMENT_TABLE, loc, 300, 8, 8, 8, 0.125).spawnAsPlayerActive(mPlayer);

		double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), radius);
		for (LivingEntity mob : hitbox.getHitMobs()) {
			EntityUtils.applyWeaken(mPlugin, DURATION, mWeakenEffect, mob);
			EntityUtils.applyTaunt(mPlugin, mob, mPlayer);
		}

		if (isEnhanced()) {
			cancelOnDeath(new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {

					if (!mPlayer.isOnline() || mPlayer.isDead()) {
						this.cancel();
						return;
					}

					Hitbox enhanceHitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), ENHANCE_RADIUS);
					int numTargeting = (int) enhanceHitbox
						                         .getHitMobs().stream()
						                         .filter(entity -> entity instanceof Mob mob && mob.getTarget() != null && mob.getTarget().equals(mPlayer))
						                         .limit(ENHANCE_MAX_MOBS)
						                         .count();
					for (Player player : enhanceHitbox.getHitPlayers(true)) {
						mPlugin.mEffectManager.addEffect(player, ENHANCE_EFFECT_NAME, new PercentDamageDealt(ENHANCE_EFFECT_DURATION, ENHANCE_DAMAGE * numTargeting, AFFECTED_DAMAGE_TYPES));
						mPlugin.mEffectManager.addEffect(player, ENHANCE_EFFECT_PARTICLE_NAME, new Aesthetics(ENHANCE_EFFECT_DURATION,
								(entity, fourHertz, twoHertz, oneHertz) -> {
									Location loc = player.getLocation().add(0, 1, 0);
									new PartialParticle(Particle.ENCHANTMENT_TABLE, loc, 20, 0.5, 0, 0.5, 0.125).spawnAsPlayerActive(mPlayer);
								}, (entity) -> {
							})
						);
					}

					mTicks += 1;
					if (mTicks > DURATION) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1));

		}

		if (isLevelTwo()) {
			int reductionTime = CharmManager.getDuration(mPlayer, CHARM_RECOVERY, CLEANSE_REDUCTION);
			for (Player player : hitbox.getHitPlayers(true)) {
				new PartialParticle(Particle.REDSTONE, player.getLocation(), 13, 0.25, 2, 0.25, 0.125, COLOR).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.ENCHANTMENT_TABLE, player.getLocation(), 13, 0.25, 2, 0.25, 0.125).spawnAsPlayerActive(mPlayer);
				for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, player)) {
					PotionEffect effect = player.getPotionEffect(effectType);
					if (effect != null) {
						player.removePotionEffect(effectType);
						if (effect.getDuration() - reductionTime > 0) {
							player.addPotionEffect(new PotionEffect(effectType, effect.getDuration() - reductionTime, effect.getAmplifier()));
						}
					}
				}
				EntityUtils.setWeakenTicks(mPlugin, player, Math.max(0, EntityUtils.getWeakenTicks(mPlugin, player) - reductionTime));
				EntityUtils.setSlowTicks(mPlugin, player, Math.max(0, EntityUtils.getSlowTicks(mPlugin, player) - reductionTime));
			}
		}
		putOnCooldown();
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() != DamageEvent.DamageType.AILMENT
			&& mEnhancementBonusDamage > 0) {
			event.setDamage(event.getDamage() + mEnhancementBonusDamage);
			mEnhancementBonusDamage = 0;
		}

		return false;
	}
}
