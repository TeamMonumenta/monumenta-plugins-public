package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class LastBreath extends DepthsAbility {
	public static final String ABILITY_NAME = "Last Breath";
	public static final int COOLDOWN = 60 * 20;
	private static final double TRIGGER_HEALTH = 0.4;
	public static final double[] COOLDOWN_REDUCTION = {0.4, 0.5, 0.6, 0.7, 0.8, 1.0};
	private static final double[] SPEED = {0.1, 0.125, 0.15, 0.175, 0.2, 0.35};
	private static final int[] RESISTANCE_TICKS = {30, 35, 40, 45, 50, 70};
	private static final int SPEED_DURATION = 6 * 20;
	private static final String SPEED_EFFECT_NAME = "LastBreathSpeedEffect";
	private static final String RESIST_EFFECT_NAME = "LastBreathResistanceEffect";
	public static final int RADIUS = 5;
	public static final double KNOCKBACK_SPEED = 2;

	public static final String CHARM_COOLDOWN = "Last Breath Cooldown";

	public static final DepthsAbilityInfo<LastBreath> INFO =
		new DepthsAbilityInfo<>(LastBreath.class, ABILITY_NAME, LastBreath::new, DepthsTree.WINDWALKER, DepthsTrigger.LIFELINE)
			.linkedSpell(ClassAbility.LAST_BREATH)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.displayItem(Material.DRAGON_BREATH)
			.descriptions(LastBreath::getDescription)
			.priorityAmount(10000);

	private final int mSpeedDuration;
	private final double mSpeed;
	private final int mResistDuration;
	private final double mRadius;
	private final double mCDR;

	public LastBreath(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mSpeedDuration = CharmManager.getDuration(mPlayer, CharmEffects.LAST_BREATH_SPEED_DURATION.mEffectName, SPEED_DURATION);
		mSpeed = SPEED[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.LAST_BREATH_SPEED_AMPLIFIER.mEffectName);
		mResistDuration = CharmManager.getDuration(mPlayer, CharmEffects.LAST_BREATH_RESISTANCE_DURATION.mEffectName, RESISTANCE_TICKS[mRarity - 1]);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.LAST_BREATH_RADIUS.mEffectName, RADIUS);
		mCDR = COOLDOWN_REDUCTION[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.LAST_BREATH_COOLDOWN_REDUCTION.mEffectName);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isBlocked() || isOnCooldown() || event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}

		// Calculate whether this effect should not be run based on player health.
		double healthRemaining = mPlayer.getHealth() - event.getFinalDamage(true);

		if (healthRemaining > EntityUtils.getMaxHealth(mPlayer) * TRIGGER_HEALTH) {
			return;
		}

		putOnCooldown();

		for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
			ClassAbility linkedSpell = abil.getInfo().getLinkedSpell();
			if (abil == this || linkedSpell == null) {
				continue;
			}
			int totalCD = abil.getModifiedCooldown();
			int reducedCD;
			if (abil instanceof DepthsAbility da && da.getInfo().getDepthsTree() == DepthsTree.WINDWALKER) {
				reducedCD = totalCD;
			} else {
				reducedCD = (int) (totalCD * mCDR);
			}
			mPlugin.mTimers.updateCooldown(mPlayer, linkedSpell, reducedCD);
		}

		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();

		mPlugin.mEffectManager.addEffect(mPlayer, SPEED_EFFECT_NAME, new PercentSpeed(mSpeedDuration, mSpeed, SPEED_EFFECT_NAME));
		mPlugin.mEffectManager.addEffect(mPlayer, RESIST_EFFECT_NAME, new PercentDamageReceived(mResistDuration, -1));
		for (LivingEntity e : EntityUtils.getNearbyMobs(loc, mRadius)) {
			if (!EntityUtils.isCCImmuneMob(e)) {
				Vector relative = e.getLocation().toVector().subtract(loc.toVector()).normalize();
				if (!Double.isFinite(relative.getX())) {
					relative = new Vector(0, 1, 0);
				}
				Vector knockback = e.getVelocity().add(relative.multiply(KNOCKBACK_SPEED));
				knockback.setY(knockback.getY() * 2);
				e.setVelocity(knockback.add(new Vector(0, 0.25, 0)));
				new PartialParticle(Particle.EXPLOSION_NORMAL, e.getLocation(), 5, 0, 0, 0, 0.35).spawnAsPlayerActive(mPlayer);
			}
		}

		Location loc2 = loc.clone().add(0, 1, 0);
		new PartialParticle(Particle.END_ROD, loc2, 50, 0.2, 0.2, 0.2, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, loc2, 75, 0.2, 0.2, 0.2, 0.5).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc2, 75, 0.2, 0.2, 0.2, 0.5).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.VILLAGER_HAPPY, loc2, 20, 1.25, 1.25, 1.25).spawnAsPlayerActive(mPlayer);

		world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2.0f, 1.2f);
		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 2.0f, 2.0f);
		world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 2.0f, 1.2f);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 2.0f, 0.4f);

		sendActionBarMessage("Last Breath has been activated!");
		event.setCancelled(true);

	}

	@Override
	public void onHurtFatal(DamageEvent event) {
		onHurt(event, null, null);
	}

	private static Description<LastBreath> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<LastBreath>(color)
			.add("When your health drops below ")
			.addPercent(TRIGGER_HEALTH)
			.add(", all your other Windwalker abilities' cooldowns are reset, and abilities from other trees have their cooldowns reduced by ")
			.addPercent(a -> a.mCDR, COOLDOWN_REDUCTION[rarity - 1], false, true)
			.add(". You gain ")
			.addPercent(a -> a.mSpeed, SPEED[rarity - 1], false, true)
			.add(" speed for ")
			.addDuration(a -> a.mSpeedDuration, SPEED_DURATION)
			.add(" seconds and 100% resistance for ")
			.addDuration(a -> a.mResistDuration, RESISTANCE_TICKS[rarity - 1], false, true)
			.add(" seconds. Mobs within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks are knocked away.")
			.addCooldown(COOLDOWN);
	}


}
