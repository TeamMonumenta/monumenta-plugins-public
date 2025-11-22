package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Permafrost extends DepthsAbility implements AbilityWithChargesOrStacks {
	public static final String ABILITY_NAME = "Permafrost";
	public static final int DEBUFF_TICKS = 3 * 20;
	public static final int[] ICE_TICKS = {8 * 20, 11 * 20, 14 * 20, 17 * 20, 20 * 20, 26 * 20};
	public static final double RADIUS = 4;
	public static final double SLOWNESS = 0.2;
	public static final double[] VULNERABILITY = {0.1, 0.13, 0.16, 0.19, 0.22, 0.28};
	public static final int MAX_CHARGES = 3;
	public static final int KILLS_PER_CHARGE = 3;
	public static final String DEBUFF_SOURCE = "PermafrostDebuff";

	public static final DepthsAbilityInfo<Permafrost> INFO =
		new DepthsAbilityInfo<>(Permafrost.class, ABILITY_NAME, Permafrost::new, DepthsTree.FROSTBORN, DepthsTrigger.WILDCARD)
			.linkedSpell(ClassAbility.PERMAFROST)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Permafrost::cast,
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(false)
					.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
					.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.QUARTZ)
			.descriptions(Permafrost::getDescription);

	private final int mDebuffDuration;
	private final int mIceDuration;
	private final double mRadius;
	private final double mSlowness;
	private final double mVulnerability;

	private int mCharges;
	private int mMobKills;

	public Permafrost(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDebuffDuration = CharmManager.getDuration(mPlayer, CharmEffects.PERMAFROST_DEBUFF_DURATION.mEffectName, DEBUFF_TICKS);
		mIceDuration = CharmManager.getDuration(mPlayer, CharmEffects.PERMAFROST_ICE_DURATION.mEffectName, ICE_TICKS[mRarity - 1]);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.PERMAFROST_RADIUS.mEffectName, RADIUS);
		mSlowness = SLOWNESS + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.PERMAFROST_SLOWNESS_AMPLIFIER.mEffectName);
		mVulnerability = VULNERABILITY[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.PERMAFROST_VULNERABILITY_AMPLIFIER.mEffectName);
		mCharges = Math.min(AbilityManager.getManager().getTrackedCharges(mPlayer, ClassAbility.PERMAFROST), MAX_CHARGES);
		mMobKills = 0;
	}

	public boolean cast() {
		if (mCharges == 0) {
			return false;
		}
		mCharges--;
		// start at -2 * PI / 3 or 2 * PI / 3
		// increment by PI / 18 or -PI / 18
		// increment 5 times per tick, total of 5 ticks
		final double start = (FastUtils.randomBoolean() ? Math.PI : -Math.PI) * 2 / 3;
		cancelOnDeath(new BukkitRunnable() {
			int mT = 0;
			final double mIncrement = -start / 12;
			final Location mOriginalLocation = mPlayer.getEyeLocation();
			final Vector mOriginalDirection = getXZDirection();
			final Vector mDirection = mOriginalDirection.clone().multiply(mRadius).rotateAroundY(start);

			@Override
			public void run() {
				if (mT == 5) {
					cancel();
					return;
				}
				for (int i = 0; i < 5; i++) {
					Location loc = mOriginalLocation.clone().add(mDirection);
					loc = LocationUtils.fallToGround(loc, mPlayer.getLocation().getY() - 0.5);
					Block block = loc.getBlock();
					DepthsUtils.iceExposedBlock(block, mIceDuration, mPlayer);
					DepthsUtils.iceExposedBlock(block.getRelative(mOriginalDirection.getX() > 0 ? BlockFace.EAST : BlockFace.WEST), mIceDuration, mPlayer);
					DepthsUtils.iceExposedBlock(block.getRelative(mOriginalDirection.getZ() > 0 ? BlockFace.SOUTH : BlockFace.NORTH), mIceDuration, mPlayer);
					mDirection.rotateAroundY(mIncrement);
				}
				mT++;
			}
		}.runTaskTimer(mPlugin, 0, 1));

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 1.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.6f, 1.6f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 1.9f);

		double angle = FastUtils.randomDoubleInRange(-15, 15);
		if (start < 0) {
			angle += 180;
		}
		ParticleUtils.drawHalfArc(mPlayer.getEyeLocation(), mRadius, angle,
			-30, 210, 6, 0.3, false, 60,
			(l, rings, angleProgress) -> {
				new PartialParticle(Particle.REDSTONE, l, 1,
					new Particle.DustOptions(Color.fromRGB(170, 170, 255), 1f)
				).spawnAsPlayerActive(mPlayer);
				if (rings % 2 == 0) {
					new PartialParticle(Particle.SWEEP_ATTACK, l, 1).extra(0).spawnAsPlayerActive(mPlayer);
				}
			});

		Hitbox.approximateCone(loc, mRadius, Math.PI * 2 / 3).getHitMobs().forEach(m -> {
			EntityUtils.applySlow(mPlugin, mDebuffDuration, mSlowness, m, DEBUFF_SOURCE);
			EntityUtils.applySelfishVulnerability(mPlugin, mDebuffDuration, mVulnerability, m, mPlayer);
			GlowingManager.startGlowing(m, NamedTextColor.AQUA, mDebuffDuration, GlowingManager.PLAYER_ABILITY_PRIORITY, p -> p.equals(mPlayer), DEBUFF_SOURCE);
		});

		AbilityManager.getManager().trackCharges(mPlayer, ClassAbility.PERMAFROST, mCharges);
		ClientModHandler.updateAbility(mPlayer, this);
		return true;
	}

	private static Description<Permafrost> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("For every " + KILLS_PER_CHARGE + " mobs you kill, gain 1 charge, up to a maximum of " + MAX_CHARGES + ". ")
			.addTrigger()
			.add(" to consume the charge, spawning ice in a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block cone for ")
			.addDuration(a -> a.mIceDuration, ICE_TICKS[rarity - 1], false, true)
			.add(" seconds and applies ")
			.addPercent(a -> a.mSlowness, SLOWNESS)
			.add(" slowness and ")
			.addPercent(a -> a.mVulnerability, VULNERABILITY[rarity - 1], false, true)
			.add(" vulnerability for ")
			.addDuration(a -> a.mDebuffDuration, DEBUFF_TICKS)
			.add(" seconds. This vulnerability only applies to the caster.");
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (mCharges == MAX_CHARGES) {
			return;
		}
		mMobKills++;
		if (mMobKills == KILLS_PER_CHARGE) {
			mCharges = Math.min(MAX_CHARGES, mCharges + 1);
			mMobKills = 0;
			mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 2f, (float) (mCharges * 0.25 + 0.75));
			AbilityManager.getManager().trackCharges(mPlayer, ClassAbility.PERMAFROST, mCharges);
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	@Override
	public int getCharges() {
		return mCharges;
	}

	@Override
	public int getMaxCharges() {
		return MAX_CHARGES;
	}

	private Vector getXZDirection() {
		Location loc = mPlayer.getLocation();
		loc.setPitch(0);
		return loc.getDirection();
	}
}
