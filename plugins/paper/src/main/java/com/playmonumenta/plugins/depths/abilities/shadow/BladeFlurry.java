package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BladeFlurry extends DepthsAbility {

	public static final String ABILITY_NAME = "Blade Flurry";
	public static final int COOLDOWN = 20 * 6;
	public static final int[] DAMAGE = {8, 10, 12, 14, 16, 20};
	public static final int RADIUS = 3;
	public static final int[] SILENCE_DURATION = {20, 25, 30, 35, 40, 50};

	public static final String CHARM_COOLDOWN = "Blade Flurry Cooldown";

	public static final DepthsAbilityInfo<BladeFlurry> INFO =
		new DepthsAbilityInfo<>(BladeFlurry.class, ABILITY_NAME, BladeFlurry::new, DepthsTree.SHADOWDANCER, DepthsTrigger.SHIFT_RIGHT_CLICK)
			.linkedSpell(ClassAbility.BLADE_FLURRY)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", BladeFlurry::cast, DepthsTrigger.SHIFT_RIGHT_CLICK))
			.displayItem(Material.IRON_SWORD)
			.descriptions(BladeFlurry::getDescription);

	private final double mRadius;
	private final double mDamage;
	private final int mSilenceDuration;

	public BladeFlurry(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.BLADE_FLURRY_RADIUS.mEffectName, RADIUS);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.BLADE_FLURRY_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mSilenceDuration = CharmManager.getDuration(mPlayer, CharmEffects.BLADE_FLURRY_SILENCE_DURATION.mEffectName, SILENCE_DURATION[mRarity - 1]);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		World mWorld = mPlayer.getWorld();
		Location loc = mPlayer.getEyeLocation().add(0, -0.5, 0);
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, mRadius);
		for (LivingEntity mob : mobs) {
			EntityUtils.applySilence(mPlugin, mSilenceDuration, mob);
			DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage, mInfo.getLinkedSpell());
			MovementUtils.knockAway(mPlayer, mob, 0.8f, true);
		}
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 0.75f);

		cancelOnDeath(new BukkitRunnable() {
			final Vector mEyeDir = loc.getDirection();

			double mStartAngle = Math.atan(mEyeDir.getZ() / mEyeDir.getX());
			int mIncrementDegrees = 0;

			@Override
			public void run() {
				if (mIncrementDegrees == 0) {
					if (mEyeDir.getX() < 0) {
						mStartAngle += Math.PI;
					}
					mStartAngle += Math.PI * 90 / 180;
				}
				Location mLoc = mPlayer.getEyeLocation().add(0, -0.5, 0);
				Vector direction = new Vector(Math.cos(mStartAngle - Math.PI * mIncrementDegrees / 180), 0, Math.sin(mStartAngle - Math.PI * mIncrementDegrees / 180));
				Location bladeLoc = mLoc.clone().add(direction.clone().multiply(mRadius));

				new PartialParticle(Particle.SPELL_WITCH, bladeLoc, (int) (10 * mRadius / RADIUS), 0.35, 0, 0.35, 1).spawnAsPlayerActive(mPlayer);
				mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.75f, 0.5f);

				if (mIncrementDegrees >= 360) {
					this.cancel();
				}

				mIncrementDegrees += 30;
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	private static Description<BladeFlurry> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<BladeFlurry>(color)
			.add("Right click while sneaking and holding a weapon to deal ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" melee damage in a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius around you. Affected mobs are silenced for ")
			.addDuration(a -> a.mSilenceDuration, SILENCE_DURATION[rarity - 1], false, true)
			.add(" seconds and knocked away slightly.")
			.addCooldown(COOLDOWN);
	}


}

