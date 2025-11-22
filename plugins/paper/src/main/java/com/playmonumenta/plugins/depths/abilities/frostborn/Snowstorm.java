package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.effects.SnowstormStacks;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Snowstorm extends DepthsAbility {

	public static final String ABILITY_NAME = "Snowstorm";
	public static final int COOLDOWN = 16 * 20;
	public static final int DURATION = 8 * 20;
	public static final int RADIUS = 5;
	public static final double[] DAMAGE = {1.5, 2.0, 2.5, 3.0, 3.5, 4.5};
	public static final double SLOW_AMOUNT = 0.15;
	public static final int SLOW_DURATION = 4 * 20;
	public static final double[] FREEZE_DAMAGE = {10, 12.5, 15, 17.5, 20, 25};
	public static final int FREEZE_DURATION = 2 * 20;
	public static final int ICE_TICKS = 8 * 20;
	public static final Particle.DustOptions LIGHT_BLUE = new Particle.DustOptions(Color.fromRGB(180, 230, 255), 1f);

	public static final String CHARM_COOLDOWN = "Snowstorm Cooldown";

	public static final DepthsAbilityInfo<Snowstorm> INFO =
		new DepthsAbilityInfo<>(Snowstorm.class, ABILITY_NAME, Snowstorm::new, DepthsTree.FROSTBORN, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.SNOWSTORM)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Snowstorm::cast, DepthsTrigger.SHIFT_LEFT_CLICK))
			.displayItem(Material.ICE)
			.descriptions(Snowstorm::getDescription);

	private final int mDuration;
	private final double mRadius;
	private final double mDamage;
	private final double mSlowAmount;
	private final int mSlowDuration;
	private final double mFreezeDamage;
	private final int mFreezeDuration;
	private final int mIceDuration;

	public Snowstorm(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = DURATION;
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.SNOWSTORM_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.SNOWSTORM_RADIUS.mEffectName, RADIUS);
		mSlowAmount = SLOW_AMOUNT + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.SNOWSTORM_SLOW_AMPLIFIER.mEffectName);
		mSlowDuration = CharmManager.getDuration(mPlayer, CharmEffects.SNOWSTORM_SLOW_DURATION.mEffectName, SLOW_DURATION);
		mFreezeDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.SNOWSTORM_DAMAGE.mEffectName, FREEZE_DAMAGE[mRarity - 1]);
		mFreezeDuration = CharmManager.getDuration(mPlayer, CharmEffects.SNOWSTORM_FREEZE_DURATION.mEffectName, FREEZE_DURATION);
		mIceDuration = CharmManager.getDuration(mPlayer, CharmEffects.SNOWSTORM_ICE_DURATION.mEffectName, ICE_TICKS);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		putOnCooldown();

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		new BukkitRunnable() {
			double mCurrRadius = 0;
			final Location mLoc = loc.clone();

			@Override
			public void run() {
				mCurrRadius += 1.4;
				for (double j = 0; j < 360; j += 18) {
					double radian1 = Math.toRadians(j);
					mLoc.add(FastUtils.cos(radian1) * mCurrRadius, 0.15, FastUtils.sin(radian1) * mCurrRadius);
					new PartialParticle(Particle.CLOUD, mLoc, 1, 0, 0, 0, 0.1).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.CRIT_MAGIC, mLoc, 8, 0, 0, 0, 0.65).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SNOWFLAKE, mLoc, 1, 0, 0, 0, 0.3).spawnAsPlayerActive(mPlayer);
					mLoc.subtract(FastUtils.cos(radian1) * mCurrRadius, 0.15, FastUtils.sin(radian1) * mCurrRadius);
				}

				if (mCurrRadius >= mRadius) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.7f, 0.65f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 1.5f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1f, 1.2f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1f, 0.7f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1f, 0.1f);
		world.playSound(loc, Sound.ENTITY_SNOW_GOLEM_DEATH, SoundCategory.PLAYERS, 1f, 0.1f);

		new PartialParticle(Particle.CLOUD, loc, 25, 0, 0, 0, 0.35).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPIT, loc, 35, 0, 0, 0, 0.45).spawnAsPlayerActive(mPlayer);

		// blizzard!
		PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		loc.setPitch(0); // blizzard should only go horizontally
		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;
			final Location mLoc = loc.clone();
			final Vector mDir = mLoc.getDirection();

			@Override
			public void run() {
				new PartialParticle(Particle.SNOWBALL, mLoc, 4).delta(mRadius / 2).spawnAsPlayerActive(mPlayer);
				if (mTicks % 10 == 0) {
					world.playSound(mLoc, Sound.ENTITY_SNOW_GOLEM_DEATH, SoundCategory.PLAYERS, 0.35f, 0.5f);
					new PPCircle(Particle.CLOUD, mLoc.clone().add(0, 6, 0), mRadius * 0.8).ringMode(false).countPerMeter(0.75).spawnAsPlayerActive(mPlayer);
					new PPCircle(Particle.CLOUD, mLoc.clone().add(0, 6, 0), mRadius * 0.4).delta(0, 0.25, 0).ringMode(false).countPerMeter(0.5).spawnAsPlayerActive(mPlayer);
				}
				for (int i = 0; i < 4; i++) {
					new PartialParticle(Particle.SNOWFLAKE, LocationUtils.randomLocationInCircle(mLoc.clone().add(0, 6, 0), mRadius), 1)
						.directionalMode(true).delta(0, -1, 0).extraRange(0.3, 0.5).spawnAsPlayerActive(mPlayer);
				}
				double angle = mTicks * 3 % 60;
				new PPCircle(Particle.REDSTONE, mLoc, mRadius).data(LIGHT_BLUE).count(7)
					.arcDegree(angle, angle + 360).spawnAsPlayerActive(mPlayer);

				if (mTicks % 10 == 0) {
					Hitbox hitbox = new Hitbox.UprightCylinderHitbox(mLoc.clone().add(0, -mRadius, 0), mRadius * 2, mRadius);
					List<LivingEntity> mobs = hitbox.getHitMobs();
					for (LivingEntity mob : mobs) {
						DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), mDamage, true, false, false);
						addBlizzardStacks(mob);
					}

					List<Player> players = hitbox.getHitPlayers(true);
					for (Player player : players) {
						player.setFireTicks(0);
						mPlugin.mEffectManager.addEffect(player, "SnowstormKBR", new PercentKnockbackResist(2 * 20, 1, "SnowstormKBR"));
					}
				}

				if (mTicks % 5 == 0 || mTicks == 1 || mTicks == 2 || mTicks == 3 || mTicks == 4) {
					int i = 0;
					int successes = 0;
					int blocksToIce = (int) (12 * Math.pow(mRadius / 6, 2));
					while (successes < blocksToIce) {
						Location iceLoc = LocationUtils.fallToGround(LocationUtils.randomLocationInCircle(mLoc, mRadius), mLoc.getY() - 6);
						Block iceBlock = iceLoc.getBlock();
						if (DepthsUtils.iceExposedBlock(iceBlock, mIceDuration, mPlayer)) {
							new PartialParticle(Particle.REDSTONE, BlockUtils.getCenteredBlockBaseLocation(iceBlock), 8).delta(0.25, 0, 0.25).data(LIGHT_BLUE).spawnAsPlayerActive(mPlayer);
							successes++;
						}

						i++;
						if (i > 50) {
							break;
						}
					}
				}

				mLoc.add(mDir.clone().multiply(0.15));

				mTicks += 1;
				if (mTicks > mDuration) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));

		return true;
	}

	public void addBlizzardStacks(LivingEntity mob) {
		SnowstormStacks blizzardEffect = mPlugin.mEffectManager.getActiveEffect(mob, SnowstormStacks.class);
		if (blizzardEffect == null) {
			mPlugin.mEffectManager.addEffect(mob, "SnowstormStacks", new SnowstormStacks(mPlayer, 0, mSlowAmount, mSlowDuration, 5, mFreezeDamage, mFreezeDuration));
		} else {
			blizzardEffect.incrementStacks(mob);
		}
	}

	private static Description<Snowstorm> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.addTrigger()
			.add(" to summon a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius snowstorm that lasts for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds and slowly travels forwards, freezing the ground beneath it for ")
			.addDuration(a -> a.mIceDuration, ICE_TICKS)
			.add(" seconds. Every 0.5 seconds, mobs in the storm take ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage and are slowed by ")
			.addPercent(a -> a.mSlowAmount, SLOW_AMOUNT)
			.add(" for ")
			.addDuration(a -> a.mSlowDuration, SLOW_DURATION)
			.add(" seconds, stacking up to 5 times. Once a mob reaches 5 stacks, they take ")
			.addDepthsDamage(a -> a.mFreezeDamage, FREEZE_DAMAGE[rarity - 1], true)
			.add(" magic damage and are frozen for ")
			.addDuration(a -> a.mFreezeDuration, FREEZE_DURATION)
			.add(" seconds. (Bosses and CC immune mobs are immune.) Players in the storm are extinguished and receive 100% Knockback Resistance.")
			.addCooldown(COOLDOWN);
	}


}

