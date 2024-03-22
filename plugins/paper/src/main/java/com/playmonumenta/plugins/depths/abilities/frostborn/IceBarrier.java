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
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.FluidCollisionMode;
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
import org.bukkit.util.BlockIterator;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class IceBarrier extends DepthsAbility {

	public static final String ABILITY_NAME = "Ice Barrier";
	public static final double[] DAMAGE = {13, 16, 19, 22, 25, 30};
	public static final int[] ICE_TICKS = {8 * 20, 10 * 20, 12 * 20, 14 * 20, 16 * 20, 20 * 20};
	public static final int[] COOLDOWN = {20 * 20, 18 * 20, 16 * 20, 14 * 20, 12 * 20, 8 * 20};
	public static final int CAST_RANGE = 30;
	public static final int[] MAX_LENGTH = {20, 25, 30, 35, 40, 50};
	public static final int CAST_TIME = 5 * 20;

	public static final String CHARM_COOLDOWN = "Ice Barrier Cooldown";

	public static final DepthsAbilityInfo<IceBarrier> INFO =
		new DepthsAbilityInfo<>(IceBarrier.class, ABILITY_NAME, IceBarrier::new, DepthsTree.FROSTBORN, DepthsTrigger.SHIFT_RIGHT_CLICK)
			.linkedSpell(ClassAbility.ICE_BARRIER)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", IceBarrier::cast, DepthsTrigger.SHIFT_RIGHT_CLICK))
			.displayItem(Material.PRISMARINE_WALL)
			.descriptions(IceBarrier::getDescription);

	private final int mRange;
	private final int mIceDuration;
	private final double mLength;
	private final double mDamage;

	public boolean mIsPrimed;
	public @Nullable Location mPrimedLoc;

	public IceBarrier(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRange = (int) CharmManager.getRadius(mPlayer, CharmEffects.ICE_BARRIER_CAST_RANGE.mEffectName, CAST_RANGE);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.ICE_BARRIER_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mIceDuration = CharmManager.getDuration(mPlayer, CharmEffects.ICE_BARRIER_ICE_DURATION.mEffectName, ICE_TICKS[mRarity - 1]);
		mLength = CharmManager.getRadius(mPlayer, CharmEffects.ICE_BARRIER_MAX_LENGTH.mEffectName, MAX_LENGTH[mRarity - 1]);
		mIsPrimed = false;
		mPrimedLoc = null;
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		World world = mPlayer.getWorld();

		RayTraceResult rayTraceResult = mPlayer.getWorld().rayTraceBlocks(mPlayer.getEyeLocation(), mPlayer.getLocation().getDirection(), mRange, FluidCollisionMode.NEVER, true);
		if (rayTraceResult == null) {
			return true;
		}
		Block block = rayTraceResult.getHitBlock();
		if (block == null) {
			return true;
		}

		boolean validLength = !mIsPrimed || mPrimedLoc == null || (mPrimedLoc.distance(block.getLocation()) <= mLength && mPrimedLoc.distance(block.getLocation()) >= 1);
		if (!validLength || block.getType().isAir() || block.getType() == Material.BEDROCK) {
			return true;
		}

		DepthsUtils.spawnIceTerrain(block, CAST_TIME, mPlayer);
		new PartialParticle(Particle.CRIT, block.getLocation(), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, block.getLocation(), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.5f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 0.8f, 1.0f);
		world.playSound(loc, Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 0.8f, 2.0f);
		if (!mIsPrimed || mPrimedLoc == null) {
			mIsPrimed = true;
			mPrimedLoc = block.getLocation();
			ClientModHandler.updateAbility(mPlayer, this);

			new BukkitRunnable() {

				@Override
				public void run() {
					if (mIsPrimed && mPrimedLoc != null) {
						mIsPrimed = false;
						mPrimedLoc = null;
						world.playSound(mPlayer.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2.0f, 0.5f);
						//Reset cd
						mPlugin.mTimers.addCooldown(mPlayer, ClassAbility.ICE_BARRIER, 0);
					}
				}

			}.runTaskLater(mPlugin, Math.min(CAST_TIME, getModifiedCooldown()));
		} else {
			//Build the wall
			mIsPrimed = false;
			putOnCooldown();

			ArrayList<Block> blocksToIce = new ArrayList<>();

			Vector v = block.getLocation().toVector().subtract(mPrimedLoc.toVector());
			BlockIterator iterator = new BlockIterator(world, mPrimedLoc.toVector(), v, 0.0, (int) block.getLocation().distance(mPrimedLoc));
			while (iterator.hasNext()) {
				Block b = iterator.next();

				if (!(Math.abs(mPrimedLoc.getY() - block.getLocation().getY()) >= 4)) {
					// barrier mode
					blocksToIce.add(b);
					blocksToIce.add(b.getRelative(BlockFace.UP));
					blocksToIce.add(b.getRelative(BlockFace.UP).getRelative(BlockFace.UP));
					blocksToIce.add(b.getRelative(BlockFace.DOWN));
				} else {
					// stairway mode
					blocksToIce.add(b.getRelative(BlockFace.DOWN));
					blocksToIce.add(b.getRelative(BlockFace.DOWN).getRelative(BlockFace.EAST));
					blocksToIce.add(b.getRelative(BlockFace.DOWN).getRelative(BlockFace.WEST));
					blocksToIce.add(b.getRelative(BlockFace.DOWN).getRelative(BlockFace.NORTH));
					blocksToIce.add(b.getRelative(BlockFace.DOWN).getRelative(BlockFace.SOUTH));
				}
			}

			List<LivingEntity> hitMobs = new ArrayList<>();
			for (Block b : blocksToIce) {
				Location blockLocation = BlockUtils.getCenterBlockLocation(b);
				DepthsUtils.spawnIceTerrain(b.getRelative(BlockFace.UP), mIceDuration, mPlayer, true);
				new PartialParticle(Particle.CRIT, blockLocation, 10, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CRIT_MAGIC, blockLocation, 10, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);

				for (LivingEntity mob : EntityUtils.getNearbyMobs(blockLocation, 2, 2, 2)) {
					if (!hitMobs.contains(mob)) {
						DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true);
						hitMobs.add(mob);
					}
				}
			}

			world.playSound(loc, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 0.8f, 2.0f);
			world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 0.8f, 0.5f);
			world.playSound(loc, Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 0.8f, 1.0f);
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 0.6f, 1.6f);
			world.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.2f, 2.0f);
			world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 0.8f, 2.0f);
			world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 0.4f, 0.7f);
			world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.4f, 2.0f);

			mPrimedLoc = null;
		}

		return true;
	}

	private static Description<IceBarrier> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<IceBarrier>(color)
			.add("Right clicking while sneaking to place an ice marker up to ")
			.add(a -> a.mRange, CAST_RANGE)
			.add(" blocks away. Placing a second marker within ")
			.addDuration(CAST_TIME)
			.add(" seconds and within ")
			.add(a -> a.mLength, MAX_LENGTH[rarity - 1], false, null, true)
			.add(" blocks of the first marker forms a wall of ice connecting the two points, lasting for ")
			.addDuration(a -> a.mIceDuration, ICE_TICKS[rarity - 1])
			.add(" seconds. If there is a height difference of 4 or more blocks between the two markers, an ice staircase is placed instead. Deal ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage to mobs near the ice when placed. Enemies that break the barrier are stunned for 2s. Cooldown is refunded if no second marker is placed.")
			.addCooldown(COOLDOWN[rarity - 1], true);
	}


	@Override
	public @Nullable String getMode() {
		return mIsPrimed ? "primed" : null;
	}
}
