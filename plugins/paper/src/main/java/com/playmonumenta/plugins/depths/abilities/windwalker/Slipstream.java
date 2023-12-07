package com.playmonumenta.plugins.depths.abilities.windwalker;

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
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class Slipstream extends DepthsAbility {

	public static final String ABILITY_NAME = "Slipstream";
	public static final int[] COOLDOWN = {16 * 20, 14 * 20, 12 * 20, 10 * 20, 8 * 20, 6 * 20};
	private static final int DURATION = 8 * 20;
	private static final double SPEED_AMPLIFIER = 0.2;
	private static final String PERCENT_SPEED_EFFECT_NAME = "SlipstreamSpeedEffect";
	private static final int JUMP_AMPLIFIER = 2;
	private static final int RADIUS = 4;
	private static final float KNOCKBACK_SPEED = 0.9f;

	public static final String CHARM_COOLDOWN = "Slipstream Cooldown";

	public static final DepthsAbilityInfo<Slipstream> INFO =
		new DepthsAbilityInfo<>(Slipstream.class, ABILITY_NAME, Slipstream::new, DepthsTree.WINDWALKER, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.SLIPSTREAM)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Slipstream::cast, DepthsTrigger.RIGHT_CLICK))
			.displayItem(Material.PHANTOM_MEMBRANE)
			.descriptions(Slipstream::getDescription);

	private final float mKnockbackSpeed;
	private final int mDuration;
	private final double mSpeed;
	private final int mJump;
	private final double mRadius;

	public Slipstream(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mKnockbackSpeed = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.SLIPSTREAM_KNOCKBACK.mEffectName, KNOCKBACK_SPEED);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.SLIPSTREAM_DURATION.mEffectName, DURATION);
		mSpeed = SPEED_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.SLIPSTREAM_SPEED_AMPLIFIER.mEffectName);
		mJump = (mRarity == 6 ? JUMP_AMPLIFIER + 1 : JUMP_AMPLIFIER) + (int) CharmManager.getLevel(mPlayer, CharmEffects.SLIPSTREAM_JUMP_BOOST_AMPLIFIER.mEffectName);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.SLIPSTREAM_RADIUS.mEffectName, RADIUS);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(mDuration, mSpeed, PERCENT_SPEED_EFFECT_NAME));
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, mDuration, mJump));

		Location loc = mPlayer.getEyeLocation();
		loc.add(0, -0.75, 0);
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.0f, 0.25f);

		new BukkitRunnable() {
			double mR = 0;
			@Override
			public void run() {
				mR += 1.25;
				for (double j = 0; j < 360; j += 18) {
					double radian1 = Math.toRadians(j);
					loc.add(FastUtils.cos(radian1) * mR, 0.15, FastUtils.sin(radian1) * mR);
					new PartialParticle(Particle.CLOUD, loc, 3, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 1, 0, 0, 0, 0.15).spawnAsPlayerActive(mPlayer);
					loc.subtract(FastUtils.cos(radian1) * mR, 0.15, FastUtils.sin(radian1) * mR);
				}
				if (mR >= mRadius + 1) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, mRadius, mPlayer)) {
			if (!EntityUtils.isCCImmuneMob(mob)) {
				MovementUtils.knockAway(mPlayer.getLocation(), mob, mKnockbackSpeed, mKnockbackSpeed / 2, true);
			}
		}
	}

	private static Description<Slipstream> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Slipstream>(color)
			.add("Right click to knock all enemies within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks away from you and gain Jump Boost ")
			.add(a -> a.mJump + 1, rarity == 6 ? 4 : 3, false, d -> StringUtils.toRoman(d.intValue()), rarity == 6)
			.add(" and ")
			.addPercent(a -> a.mSpeed, SPEED_AMPLIFIER)
			.add(" speed for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN[rarity - 1], true);
	}

}

