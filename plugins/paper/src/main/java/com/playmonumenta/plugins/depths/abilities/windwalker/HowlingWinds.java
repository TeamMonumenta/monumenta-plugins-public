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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HowlingWinds extends DepthsAbility {

	public static final String ABILITY_NAME = "Howling Winds";
	public static final int COOLDOWN = 20 * 20;
	public static final int VULN_RADIUS = 4;
	public static final int PULL_RADIUS = 16;
	public static final int DISTANCE = 6;
	public static final int[] PULL_INTERVAL = {20, 18, 16, 14, 12, 8};
	public static final double[] VULN_AMPLIFIER = {0.075, 0.1, 0.125, 0.15, 0.175, 0.225};
	public static final int DURATION_TICKS = 5 * 20;
	public static final double PULL_VELOCITY = 0.9;
	public static final double BASE_RATIO = 0.15;

	public static final String CHARM_COOLDOWN = "Howling Winds Cooldown";

	public static final DepthsAbilityInfo<HowlingWinds> INFO =
		new DepthsAbilityInfo<>(HowlingWinds.class, ABILITY_NAME, HowlingWinds::new, DepthsTree.WINDWALKER, DepthsTrigger.SWAP)
			.linkedSpell(ClassAbility.HOWLINGWINDS)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", HowlingWinds::cast, DepthsTrigger.SWAP))
			.displayItem(Material.HOPPER)
			.descriptions(HowlingWinds::getDescription);

	private final int mDuration;
	private final double mDistance;
	private final double mRadius;
	private final double mVulnRadius;
	private final double mVuln;


	public HowlingWinds(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.HOWLING_WINDS_DURATION.mEffectName, DURATION_TICKS);
		mDistance = CharmManager.getRadius(mPlayer, CharmEffects.HOWLING_WINDS_RANGE.mEffectName, DISTANCE);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.HOWLING_WINDS_RADIUS.mEffectName, PULL_RADIUS);
		mVulnRadius = CharmManager.getRadius(mPlayer, CharmEffects.HOWLING_WINDS_RADIUS.mEffectName, VULN_RADIUS);
		mVuln = VULN_AMPLIFIER[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.HOWLING_WINDS_VULNERABILITY_AMPLIFIER.mEffectName);

	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 0.8f, 0.25f);
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 1.0f, 1.2f);
		new PartialParticle(Particle.CLOUD, mPlayer.getLocation(), 15, 0.25f, 0.1f, 0.25f).spawnAsPlayerActive(mPlayer);
		Vector dir = loc.getDirection().normalize();
		for (int i = 0; i < mDistance; i++) {
			loc.add(dir);

			new PartialParticle(Particle.FIREWORKS_SPARK, loc, 5, 0.1, 0.1, 0.1, 0.1).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CLOUD, loc, 5, 0.1, 0.1, 0.1, 0.1).spawnAsPlayerActive(mPlayer);
			int size = EntityUtils.getNearbyMobs(loc, 2, mPlayer).size();
			if (loc.getBlock().getType().isSolid() || i >= mDistance - 1 || size > 0) {
				explode(loc);
				break;
			}
		}

		return true;
	}

	private void explode(Location loc) {
		World world = mPlayer.getWorld();
		new PartialParticle(Particle.CLOUD, loc, 60, 4, 4, 4, 0.125).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 40, 2, 2, 2, 0.125).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.8f, 1f);

		// strong pull at the start
		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, mRadius)) {
			if (!EntityUtils.isCCImmuneMob(mob)) {
				Vector vector = mob.getLocation().toVector().subtract(loc.toVector());
				double ratio = BASE_RATIO * 4.5 + vector.length() / mRadius;
				mob.setVelocity(mob.getVelocity().add(vector.normalize().multiply(CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.HOWLING_WINDS_VELOCITY.mEffectName, PULL_VELOCITY)).multiply(-ratio).add(new Vector(0, 0.1 + 0.2 * ratio, 0))));
			}
		}

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				if (mTicks % PULL_INTERVAL[mRarity - 1] == 0) {
					for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, mRadius)) {
						if (!EntityUtils.isCCImmuneMob(mob)) {
							Vector vector = mob.getLocation().toVector().subtract(loc.toVector());
							double ratio = BASE_RATIO + vector.length() / mRadius;
							mob.setVelocity(mob.getVelocity().add(vector.normalize().multiply(CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.HOWLING_WINDS_VELOCITY.mEffectName, PULL_VELOCITY)).multiply(-ratio).add(new Vector(0, 0.1 + 0.2 * ratio, 0))));
						}
						if (loc.distance(mob.getLocation()) < mVulnRadius) {
							EntityUtils.applyVulnerability(mPlugin, mDuration, mVuln, mob);
						}
					}
					if (mTicks <= DURATION_TICKS - 5 * 20) {
						world.playSound(loc, Sound.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 0.8f, 1);
					}
				}
				double mult = mRadius / PULL_RADIUS;
				new PartialParticle(Particle.FIREWORKS_SPARK, loc, (int) (5 * mult), 2 * mult, 2 * mult, 2 * mult, 0.1).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CLOUD, loc, (int) (3 * mult), 2 * mult, 2 * mult, 2 * mult, 0.05).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CLOUD, loc, 2, 0.1, 0.1, 0.1, 0.15).spawnAsPlayerActive(mPlayer);
				if (mTicks >= mDuration) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private static Description<HowlingWinds> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<HowlingWinds>(color)
			.add("Swap hands to summon a hurricane that lasts ")
			.addDuration(a -> a.mDuration, DURATION_TICKS)
			.add(" seconds at the location you are looking at, up to ")
			.add(a -> a.mDistance, DISTANCE)
			.add(" blocks away. The hurricane strongly pulls enemies within ")
			.add(a -> a.mRadius, PULL_RADIUS)
			.add(" blocks towards its center on spawn and continues pulling every ")
			.addDuration(a -> PULL_INTERVAL[rarity - 1], PULL_INTERVAL[rarity - 1], true, true)
			.add(" second")
			.add(PULL_INTERVAL[rarity - 1] == 20 ? "" : "s")
			.add(". Enemies within ")
			.add(a -> a.mVulnRadius, VULN_RADIUS)
			.add(" blocks are inflicted with ")
			.addPercent(a -> a.mVuln, VULN_AMPLIFIER[rarity - 1], false, true)
			.add(" vulnerability for ")
			.addDuration(a -> a.mDuration, DURATION_TICKS)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}


}

