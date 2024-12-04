package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public class Entrench extends DepthsAbility {

	public static final String ABILITY_NAME = "Entrench";

	public static final int[] DURATION = {20, 25, 30, 35, 40, 60};
	public static final int RADIUS = 6;
	public static final double SLOW_MODIFIER = 0.99;

	public static final DepthsAbilityInfo<Entrench> INFO =
		new DepthsAbilityInfo<>(Entrench.class, ABILITY_NAME, Entrench::new, DepthsTree.EARTHBOUND, DepthsTrigger.SPAWNER)
			.displayItem(Material.SOUL_SAND)
			.descriptions(Entrench::getDescription)
			.singleCharm(false);

	private final int mDuration;
	private final double mRadius;

	public Entrench(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(player, CharmEffects.ENTRENCH_ROOT_DURATION.mEffectName, DURATION[mRarity - 1]);
		mRadius = CharmManager.getRadius(player, CharmEffects.ENTRENCH_RADIUS.mEffectName, RADIUS);
	}

	public static void onSpawnerBreak(Plugin plugin, Player player, int rarity, Location loc) {
		int duration = CharmManager.getDuration(player, CharmEffects.ENTRENCH_ROOT_DURATION.mEffectName, DURATION[rarity - 1]);
		double radius = CharmManager.getRadius(player, CharmEffects.ENTRENCH_RADIUS.mEffectName, RADIUS);
		onSpawnerBreak(plugin, player, loc, duration, radius);
	}

	public static void onSpawnerBreak(Plugin plugin, Player player, Location loc, int duration, double radius) {
		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, radius)) {
			EntityUtils.applySlow(plugin, duration, SLOW_MODIFIER, mob);
		}

		World world = player.getWorld();
		world.playSound(loc, Sound.BLOCK_NETHER_BRICKS_BREAK, SoundCategory.PLAYERS, 1.2f, 0.45f);
		world.playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, SoundCategory.PLAYERS, 1, 0.6f);
		double mult = radius / RADIUS;
		new PartialParticle(Particle.BLOCK_CRACK, loc, (int) (35 * mult), 1.5 * mult, 1.5 * mult, 1.5 * mult, 1, Material.SOUL_SOIL.createBlockData()).spawnAsPlayerActive(player);
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return true;
		}
		Block block = event.getBlock();
		if (ItemUtils.isPickaxe(event.getPlayer().getInventory().getItemInMainHand()) && block.getType() == Material.SPAWNER) {
			onSpawnerBreak(mPlugin, mPlayer, block.getLocation().add(0.5, 0, 0.5), mDuration, mRadius);
		}
		return true;
	}

	private static Description<Entrench> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Entrench>(color)
			.add("Breaking a spawner roots mobs within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks for ")
			.addDuration(a -> a.mDuration, DURATION[rarity - 1], false, true)
			.add(" seconds.");
	}


}
