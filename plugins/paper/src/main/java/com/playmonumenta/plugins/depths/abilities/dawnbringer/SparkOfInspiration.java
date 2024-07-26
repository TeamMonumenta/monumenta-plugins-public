package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.AbilityCooldownRechargeRate;
import com.playmonumenta.plugins.effects.ColoredGlowingEffect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SparkOfInspiration extends DepthsAbility {

	public static final String ABILITY_NAME = "Spark of Inspiration";
	public static final int COOLDOWN = 50 * 20;
	private static final int CAST_RANGE = 16;
	private static final int[] BUFF_DURATION = {100, 110, 120, 130, 140, 160};
	private static final double[] CD_RECHARGE_BUFF = {0.60, 0.75, 0.90, 1.05, 1.20, 1.50};
	private static final double[] STRENGTH_BUFF = {0.15, 0.20, 0.25, 0.30, 0.35, 0.45};
	private static final double HEALTH_THRESHOLD = 0.3;
	private static final int RESIST_DURATION = 20;
	private static final String CD_RECHARGE_EFFECT_NAME = "SparkOfInspirationCooldownRecharge";
	private static final String STRENGTH_EFFECT_NAME = "SparkOfInspirationStrength";
	private static final String GLOWING_EFFECT_NAME = "SparkOfInspirationGlowing";
	private static final String RESIST_EFFECT_NAME = "SparkOfInspirationResist";

	private static final Color ORANGE = Color.fromRGB(255, 190, 0);
	private static final Color YELLOW = Color.fromRGB(255, 225, 75);

	public static final String CHARM_COOLDOWN = "Spark of Inspiration Cooldown";

	public static final DepthsAbilityInfo<SparkOfInspiration> INFO =
		new DepthsAbilityInfo<>(SparkOfInspiration.class, ABILITY_NAME, SparkOfInspiration::new, DepthsTree.DAWNBRINGER, DepthsTrigger.SWAP)
			.linkedSpell(ClassAbility.SPARK_OF_INSPIRATION)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", SparkOfInspiration::cast, DepthsTrigger.SWAP))
			.displayItem(Material.BELL)
			.descriptions(SparkOfInspiration::getDescription);

	private final double mRange;
	private final int mBuffDuration;
	private final double mBuffCDRecharge;
	private final double mStrength;
	private final int mResistDuration;

	private @Nullable BukkitRunnable mInspireRunnable;

	public SparkOfInspiration(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRange = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.SPARK_OF_INSPIRATION_RANGE.mEffectName, CAST_RANGE);
		mBuffDuration = CharmManager.getDuration(mPlayer, CharmEffects.SPARK_OF_INSPIRATION_BUFF_DURATION.mEffectName, BUFF_DURATION[mRarity - 1]);
		mBuffCDRecharge = CD_RECHARGE_BUFF[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.SPARK_OF_INSPIRATION_CDR.mEffectName);
		mStrength = STRENGTH_BUFF[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.SPARK_OF_INSPIRATION_STRENGTH.mEffectName);
		mResistDuration = CharmManager.getDuration(mPlayer, CharmEffects.SPARK_OF_INSPIRATION_RESIST_DURATION.mEffectName, RESIST_DURATION);

		mInspireRunnable = null;
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		DepthsParty party = DepthsManager.getInstance().getDepthsParty(mPlayer);
		if (party == null) {
			return false;
		}

		Player targetPlayer = EntityUtils.getPlayerAtCursor(mPlayer, mRange, 1.5);
		if (party.getPlayers().size() == 1) {
			targetPlayer = mPlayer; // grant to self if solo
		} else if (targetPlayer == null) {
			return false;
		}

		double red = ORANGE.getRed() / 255.0;
		double green = ORANGE.getGreen() / 255.0;
		double blue = ORANGE.getBlue() / 255.0;
		new PPCircle(Particle.SPELL_MOB_AMBIENT, targetPlayer.getLocation().add(0, 0.5, 0), 1.5).delta(red, green, blue).extra(1).directionalMode(true).countPerMeter(3).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.SPELL_MOB_AMBIENT, mPlayer.getLocation().add(0, 0.5, 0), 1.5).delta(red, green, blue).extra(1).directionalMode(true).countPerMeter(3).spawnAsPlayerActive(mPlayer);
		for (int i = 0; i < 8; i++) {
			if (i % 2 == 0) {
				createLink(mPlayer, targetPlayer);
			} else {
				createLink(targetPlayer, mPlayer);
			}
		}

		putOnCooldown();

		PlayerUtils.healPlayer(mPlugin, targetPlayer, EntityUtils.getMaxHealth(targetPlayer), mPlayer);
		PlayerUtils.healPlayer(mPlugin, mPlayer, EntityUtils.getMaxHealth(targetPlayer), mPlayer);

		mPlugin.mEffectManager.addEffect(targetPlayer, CD_RECHARGE_EFFECT_NAME, new AbilityCooldownRechargeRate(mBuffDuration, mBuffCDRecharge));
		mPlugin.mEffectManager.addEffect(mPlayer, STRENGTH_EFFECT_NAME, new PercentDamageDealt(mBuffDuration, mStrength));

		mPlugin.mEffectManager.addEffect(targetPlayer, GLOWING_EFFECT_NAME, new ColoredGlowingEffect(mBuffDuration, NamedTextColor.YELLOW));
		mPlugin.mEffectManager.addEffect(mPlayer, GLOWING_EFFECT_NAME, new ColoredGlowingEffect(mBuffDuration, NamedTextColor.YELLOW));

		if (mInspireRunnable != null) {
			mInspireRunnable.cancel();
		}

		Player finalTargetPlayer = targetPlayer;
		mInspireRunnable = new BukkitRunnable() {
			int mTicks = 0;
			final Player mTarget = finalTargetPlayer;
			final World mWorld = mTarget.getWorld();
			@Override
			public void run() {
				Location targetLoc = mTarget.getLocation();

				// we love ability sfx and vfx
				switch (mTicks) {
					case 0 -> {
						mWorld.playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.8f, 1.2f);
						mWorld.playSound(targetLoc, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 0.6f, 1.6f);
						mWorld.playSound(targetLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1f, 1f);
						mWorld.playSound(targetLoc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 1f, 1f);
					}
					case 2 -> {
						mWorld.playSound(targetLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1f, 1.5f);
						mWorld.playSound(targetLoc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 1f, 1.5f);
					}
					case 4 -> {
						mWorld.playSound(targetLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1f, 2f);
						mWorld.playSound(targetLoc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 1f, 2f);
					}
					default -> {
						if (mTicks % 10 == 0 && mTicks < mBuffDuration - 10) {
							float pitch = (float) (1.2f + 0.5 * (mTicks + 1) / mBuffDuration);
							mTarget.playSound(targetLoc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.5f, pitch);
						}
					}
				}
				new PartialParticle(Particle.ELECTRIC_SPARK, LocationUtils.getEntityCenter(mTarget), 2).delta(0.5).spawnAsPlayerBuff(mTarget);
				if (mTicks % 4 == 0) {
					new PartialParticle(Particle.VILLAGER_ANGRY, LocationUtils.getEntityCenter(mPlayer), 2).delta(0.75).spawnAsPlayerBuff(mPlayer);
				}

				// protection effect
				if (mTarget.getHealth() / EntityUtils.getMaxHealth(mTarget) < HEALTH_THRESHOLD || mPlayer.getHealth() / EntityUtils.getMaxHealth(mPlayer) < HEALTH_THRESHOLD) {
					cancelEarly();
				}

				mTicks++;
				if (mTicks > mBuffDuration) {
					this.cancel();
				}
			}

			private void cancelEarly() {
				mPlugin.mEffectManager.clearEffects(mTarget, CD_RECHARGE_EFFECT_NAME);
				mPlugin.mEffectManager.clearEffects(mPlayer, STRENGTH_EFFECT_NAME);

				mPlugin.mEffectManager.clearEffects(mTarget, GLOWING_EFFECT_NAME);
				mPlugin.mEffectManager.clearEffects(mPlayer, GLOWING_EFFECT_NAME);

				mPlugin.mEffectManager.addEffect(mTarget, RESIST_EFFECT_NAME, new PercentDamageReceived(mResistDuration, -1));
				mPlugin.mEffectManager.addEffect(mPlayer, RESIST_EFFECT_NAME, new PercentDamageReceived(mResistDuration, -1));

				this.cancel();
			}

			@Override
			public synchronized void cancel() {
				mTarget.getWorld().playSound(mTarget.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1f, 0.9f);
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1f, 0.9f);
				super.cancel();
			}
		};

		mInspireRunnable.runTaskTimer(mPlugin, 0, 1);

		return true;
	}

	private void createLink(Player source, Player target) {
		Vector dir = VectorUtils.randomUnitVector().multiply(1.2);

		new BukkitRunnable() {
			final Location mL = LocationUtils.varyInUniform(source.getEyeLocation(), 1);
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = dir.clone();

			@Override
			public void run() {
				mT++;

				Location to = target.getEyeLocation();

				for (int i = 0; i < 6; i++) {
					if (mT <= 2) {
						mD = dir.clone();
					} else {
						mArcCurve += 0.105;
						mD = dir.clone().add(LocationUtils.getDirectionTo(to, mL).multiply(mArcCurve));
					}

					if (mD.length() > 0.3) {
						mD.normalize().multiply(0.3);
					}

					mL.add(mD);

					for (int j = 0; j < 1; j++) {
						Color c = FastUtils.RANDOM.nextBoolean() ? ORANGE : YELLOW;
						double red = c.getRed() / 255.0;
						double green = c.getGreen() / 255.0;
						double blue = c.getBlue() / 255.0;
						new PartialParticle(Particle.SPELL_MOB, mL.clone(), 1, red, green, blue, 1).directionalMode(true).spawnAsPlayerActive(mPlayer);
					}
					if (i % 2 == 0) {
						new PartialParticle(Particle.REDSTONE, mL, 1).data(new DustOptions(FastUtils.RANDOM.nextBoolean() ? ORANGE : YELLOW, 1f)).spawnAsPlayerActive(mPlayer);
					}


					if (mT > 5 && mL.distance(to) < 0.5) {
						this.cancel();
						return;
					}
				}

				if (mT >= 100) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private static Description<SparkOfInspiration> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<SparkOfInspiration>(color)
			.add("Swap hands while looking at a player within ")
			.add(a -> a.mRange, CAST_RANGE)
			.add(" blocks to instantly heal both of you for 100% of your max HP and empower both of you for the next ")
			.addDuration(a -> a.mBuffDuration, BUFF_DURATION[rarity - 1], false, true)
			.add(" seconds, giving them ")
			.add(Component.text("+", Style.style(color)))
			.addPercent(a -> a.mBuffCDRecharge, CD_RECHARGE_BUFF[rarity - 1], false, true)
			.add(" faster cooldown recharge rate, while you gain ")
			.addPercent(a -> a.mStrength, STRENGTH_BUFF[rarity - 1], false, true)
			.add(" Strength. If either player's health drops below ")
			.addPercent(HEALTH_THRESHOLD)
			.add(" during this time, the effect immediately ends for both players and both are granted 100% Resistance for ")
			.addDuration(a -> a.mResistDuration, RESIST_DURATION)
			.add("s. If there are no other players in your party, you may activate this ability on yourself instead.")
			.addCooldown(COOLDOWN);
	}
}
