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
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class DepthsFrostNova extends DepthsAbility {

	public static final String ABILITY_NAME = "Frost Nova";
	public static final int[] DAMAGE = {8, 10, 12, 14, 16, 20};
	public static final int SIZE = 6;
	public static final double[] SLOW_MULTIPLIER = {0.25, 0.3, 0.35, 0.4, 0.45, 0.55};
	public static final int DURATION_TICKS = 4 * 20;
	public static final int COOLDOWN_TICKS = 16 * 20;
	public static final int ICE_TICKS = 8 * 20;

	public static final String CHARM_COOLDOWN = "Frost Nova Cooldown";

	public static final DepthsAbilityInfo<DepthsFrostNova> INFO =
		new DepthsAbilityInfo<>(DepthsFrostNova.class, ABILITY_NAME, DepthsFrostNova::new, DepthsTree.FROSTBORN, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.FROST_NOVA_DEPTHS)
			.cooldown(COOLDOWN_TICKS, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", DepthsFrostNova::cast, DepthsTrigger.SHIFT_LEFT_CLICK))
			.displayItem(Material.ICE)
			.descriptions(DepthsFrostNova::getDescription);

	private final double mDamage;
	private final double mRadius;
	private final double mSlow;
	private final int mDuration;
	private final int mIceDuration;

	public DepthsFrostNova(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.FROST_NOVA_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.FROST_NOVA_RADIUS.mEffectName, SIZE);
		mSlow = SLOW_MULTIPLIER[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.FROST_NOVA_SLOW_AMPLIFIER.mEffectName);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.FROST_NOVA_SLOW_DURATION.mEffectName, DURATION_TICKS);
		mIceDuration = CharmManager.getDuration(mPlayer, CharmEffects.FROST_NOVA_ICE_DURATION.mEffectName, ICE_TICKS);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), mRadius, mPlayer)) {
			EntityUtils.applySlow(mPlugin, mDuration, mSlow, mob);
			if (mob.getFireTicks() > 1) {
				mob.setFireTicks(1);
			}
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true);
		}

		// Extinguish fire on all nearby players
		for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), mRadius, true)) {
			if (player.getFireTicks() > 1) {
				player.setFireTicks(1);
			}
		}

		//Set ice in world
		Block block = mPlayer.getWorld().getBlockAt(mPlayer.getLocation()).getRelative(BlockFace.DOWN);
		List<Block> blocksToIce = getBlocksInCircle(block, (int) mRadius);
		for (Block b : blocksToIce) {
			DepthsUtils.iceExposedBlock(b, mIceDuration, mPlayer);
		}

		World world = mPlayer.getWorld();
		new BukkitRunnable() {
			double mCurrRadius = 0;
			final Location mLoc = mPlayer.getLocation();
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

		Location loc = mPlayer.getLocation().add(0, 1, 0);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.7f, 0.65f);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.7f, 1.1f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 1.2f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 1.5f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1f, 1.2f);

		new PartialParticle(Particle.CLOUD, loc, 25, 0, 0, 0, 0.35).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPIT, loc, 35, 0, 0, 0, 0.45).spawnAsPlayerActive(mPlayer);

		return true;
	}

	private List<Block> getBlocksInCircle(Block b, int radius) {
		World world = b.getWorld();
		int bx = b.getX();
		int by = b.getY();
		int bz = b.getZ();
		List<Block> blocks = new ArrayList<>();
		for (int x = bx - radius; x <= bx + radius; x++) {
			for (int z = bz - radius; z <= bz + radius; z++) {
				Location check = new Location(world, x, by, z);
				if (check.distance(b.getLocation()) <= radius) {
					blocks.add(check.getBlock());
				}
			}
		}
		return blocks;
	}

	private static Description<DepthsFrostNova> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<DepthsFrostNova>(color)
			.add("Left click while sneaking to unleash a frost nova, dealing ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage to all enemies in a ")
			.add(a -> a.mRadius, SIZE)
			.add(" block cube around you and afflicting them with ")
			.addPercent(a -> a.mSlow, SLOW_MULTIPLIER[rarity - 1], false, true)
			.add(" slowness for ")
			.addDuration(a -> a.mDuration, DURATION_TICKS)
			.add(" seconds. All mobs and players within range are extinguished if they are on fire. Nearby blocks in the radius are replaced with ice for ")
			.addDuration(a -> a.mIceDuration, ICE_TICKS)
			.add(" seconds.")
			.addCooldown(COOLDOWN_TICKS);
	}


}

