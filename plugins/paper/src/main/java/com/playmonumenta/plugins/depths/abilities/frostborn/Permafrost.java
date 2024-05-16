package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PermafrostMark;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.ArrayList;
import net.kyori.adventure.text.Component;
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
import org.bukkit.event.block.BlockBreakEvent;

public class Permafrost extends DepthsAbility {

	public static final String ABILITY_NAME = "Permafrost";
	public static final int[] ICE_TICKS = {8 * 20, 11 * 20, 14 * 20, 17 * 20, 20 * 20, 26 * 20};
	public static final int[] ICE_BONUS_DURATION = {40, 60, 80, 100, 120, 160};
	public static final double TRAIL_MARK_RADIUS = 6;
	public static final int ICE_TRAIL_TICKS = 5 * 20;
	public static final int TRAIL_MARK_DURATION = 10 * 20;
	public static final Material PERMAFROST_ICE_MATERIAL = Material.PACKED_ICE;

	public static final DepthsAbilityInfo<Permafrost> INFO =
		new DepthsAbilityInfo<>(Permafrost.class, ABILITY_NAME, Permafrost::new, DepthsTree.FROSTBORN, DepthsTrigger.SPAWNER)
			.displayItem(Material.QUARTZ)
			.descriptions(Permafrost::getDescription)
			.singleCharm(false);

	private final int mIceDuration;
	private final double mRadius;
	private final int mTrailDuration;
	private final int mTrailIceDuration;
	private final int mBonusIceDuration;

	public Permafrost(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mIceDuration = CharmManager.getDuration(mPlayer, CharmEffects.PERMAFROST_ICE_DURATION.mEffectName, ICE_TICKS[mRarity - 1]);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.PERMAFROST_RADIUS.mEffectName, TRAIL_MARK_RADIUS);
		mTrailDuration = CharmManager.getDuration(mPlayer, CharmEffects.PERMAFROST_TRAIL_DURATION.mEffectName, TRAIL_MARK_DURATION);
		mTrailIceDuration = CharmManager.getDuration(mPlayer, CharmEffects.PERMAFROST_TRAIL_ICE_DURATION.mEffectName, ICE_TRAIL_TICKS);
		mBonusIceDuration = CharmManager.getDuration(mPlayer, CharmEffects.PERMAFROST_ICE_BONUS_DURATION.mEffectName, ICE_BONUS_DURATION[mRarity - 1]);
	}

	public static void onSpawnerBreak(Player player, int rarity, Block spawner) {
		int iceDuration = CharmManager.getDuration(player, CharmEffects.PERMAFROST_ICE_DURATION.mEffectName, ICE_TICKS[rarity - 1]);
		double radius = CharmManager.getRadius(player, CharmEffects.PERMAFROST_RADIUS.mEffectName, TRAIL_MARK_RADIUS);
		int trailDuration = CharmManager.getDuration(player, CharmEffects.PERMAFROST_TRAIL_DURATION.mEffectName, TRAIL_MARK_DURATION);
		int trailIceDuration = CharmManager.getDuration(player, CharmEffects.PERMAFROST_TRAIL_ICE_DURATION.mEffectName, ICE_TRAIL_TICKS);
		onSpawnerBreak(player, spawner, iceDuration, radius, trailDuration, trailIceDuration);
	}

	public static void onSpawnerBreak(Player player, Block spawner, int iceDuration, double radius, int trailDuration, int trailIceDuration) {
		Block block = spawner.getRelative(BlockFace.DOWN);
		if (block.isSolid() || block.getType() == Material.WATER) {
			DepthsUtils.spawnIceTerrain(block, iceDuration, player);
		}

		ArrayList<Block> blocksToIce = new ArrayList<>();
		blocksToIce.add(block.getRelative(BlockFace.NORTH));
		blocksToIce.add(block.getRelative(BlockFace.EAST));
		blocksToIce.add(block.getRelative(BlockFace.SOUTH));
		blocksToIce.add(block.getRelative(BlockFace.WEST));
		blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.NORTH));
		blocksToIce.add(block.getRelative(BlockFace.WEST).getRelative(BlockFace.WEST));
		blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.SOUTH));
		blocksToIce.add(block.getRelative(BlockFace.EAST).getRelative(BlockFace.EAST));
		blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.WEST));
		blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.EAST));
		blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.WEST));
		blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.EAST));

		for (Block b : blocksToIce) {
			DepthsUtils.iceExposedBlock(b, iceDuration, player);
		}

		Location centerLoc = block.getLocation().add(0.5, 0, 0.5);
		for (LivingEntity mob : EntityUtils.getNearbyMobs(centerLoc, radius)) {
			Plugin.getInstance().mEffectManager.addEffect(mob, "PermafrostMark", new PermafrostMark(trailIceDuration, trailDuration, player));
		}
		new PPCircle(Particle.BLOCK_CRACK, centerLoc.add(0, 1, 0), 6).data(Material.ICE.createBlockData()).ringMode(false).count(50).spawnAsPlayerActive(player);
		ParticleUtils.drawParticleCircleExplosion(player, centerLoc, 0, 0.5, 0, 0, 40,
			0.5f, true, 0, 0.1, Particle.EXPLOSION_NORMAL);
		World world = player.getWorld();
		world.playSound(centerLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1f, 1.1f);
		world.playSound(centerLoc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1f, 1.5f);
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return true;
		}

		Block block = event.getBlock();
		if (ItemUtils.isPickaxe(mPlayer.getInventory().getItemInMainHand()) && block.getType() == Material.SPAWNER) {
			onSpawnerBreak(mPlayer, block, mIceDuration, mRadius, mTrailDuration, mTrailIceDuration);
		}
		return true;
	}

	public int getBonusIceDuration() {
		return mBonusIceDuration;
	}

	private static Description<Permafrost> getDescription(int rarity, TextColor color) {
		DescriptionBuilder<Permafrost> desc = new DescriptionBuilder<Permafrost>(color)
			.add("Breaking a spawner spawns ice around it that lasts for ")
			.addDuration(a -> a.mIceDuration, ICE_TICKS[rarity - 1], false, true)
			.add(" seconds. Mobs within ")
			.add(a -> a.mRadius, TRAIL_MARK_RADIUS)
			.add(" blocks leave a trail of ice that lasts ")
			.addDuration(a -> a.mTrailIceDuration, ICE_TRAIL_TICKS)
			.add(" seconds wherever they walk for the next ")
			.addDuration(a -> a.mTrailDuration, TRAIL_MARK_DURATION)
			.add(" seconds. All ice you place with abilities lasts ")
			.addDuration(a -> a.mBonusIceDuration, ICE_BONUS_DURATION[rarity - 1], false, true)
			.add(" seconds longer");
		if (rarity == 6) {
			desc.add(Component.text(" Additionally, all ice you place is packed ice, which when Avalanched becomes normal ice.", color));
		}
		return desc;
	}


}

