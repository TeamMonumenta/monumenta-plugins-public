package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PercentSpeed;
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
import org.bukkit.util.Vector;


public class Whirlwind extends DepthsAbility {

	public static final String ABILITY_NAME = "Whirlwind";
	private static final int RADIUS = 3;
	private static final double[] KNOCKBACK_SPEED = {0.8, 1.0, 1.2, 1.4, 1.6, 2.0};
	private static final double[] SPEED = {0.1, 0.125, 0.15, 0.175, 0.2, 0.3};
	private static final int SPEED_DURATION = 6 * 20;
	private static final String SPEED_EFFECT_NAME = "WhirlwindSpeedEffect";

	public static final DepthsAbilityInfo<Whirlwind> INFO =
		new DepthsAbilityInfo<>(Whirlwind.class, ABILITY_NAME, Whirlwind::new, DepthsTree.WINDWALKER, DepthsTrigger.SPAWNER)
			.displayItem(Material.IRON_PICKAXE)
			.descriptions(Whirlwind::getDescription);

	private final double mRadius;
	private final double mKnockback;
	private final double mSpeed;
	private final int mDuration;

	public Whirlwind(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.WHIRLWIND_RADIUS.mEffectName, RADIUS);
		mKnockback = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.WHIRLWIND_KNOCKBACK.mEffectName, KNOCKBACK_SPEED[mRarity - 1]);
		mSpeed = SPEED[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.WHIRLWIND_SPEED_AMPLIFIER.mEffectName);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.WHIRLWIND_SPEED_DURATION.mEffectName, SPEED_DURATION);
	}

	public static void onSpawnerBreak(Plugin plugin, Player player, int rarity, Location loc) {
		double radius = CharmManager.getRadius(player, CharmEffects.WHIRLWIND_RADIUS.mEffectName, RADIUS);
		double knockback = CharmManager.calculateFlatAndPercentValue(player, CharmEffects.WHIRLWIND_KNOCKBACK.mEffectName, KNOCKBACK_SPEED[rarity - 1]);
		double speed = SPEED[rarity - 1] + CharmManager.getLevelPercentDecimal(player, CharmEffects.WHIRLWIND_SPEED_AMPLIFIER.mEffectName);
		int duration = CharmManager.getDuration(player, CharmEffects.WHIRLWIND_SPEED_DURATION.mEffectName, SPEED_DURATION);
		onSpawnerBreak(plugin, player, loc, radius, knockback, speed, duration);
	}

	public static void onSpawnerBreak(Plugin plugin, Player player, Location loc, double radius, double knockback, double speed, int duration) {
		World world = player.getWorld();
		loc = loc.clone().add(0, 0.5, 0);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.2f, 0.25f);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.2f, 0.35f);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.2f, 0.45f);
		new PartialParticle(Particle.CLOUD, loc, 30, 1, 1, 1, 0.8).spawnAsPlayerActive(player);
		for (LivingEntity e : EntityUtils.getNearbyMobs(loc, radius)) {
			e.setVelocity(e.getVelocity().add(e.getLocation().toVector().subtract(loc.subtract(0, 0.5, 0).toVector()).normalize().multiply(knockback).add(new Vector(0, 0.3, 0))));
			new PartialParticle(Particle.EXPLOSION_NORMAL, e.getLocation(), 5, 0, 0, 0, 0.35).spawnAsPlayerActive(player);
		}
		plugin.mEffectManager.addEffect(player, SPEED_EFFECT_NAME, new PercentSpeed(duration, speed, SPEED_EFFECT_NAME));
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return true;
		}

		Block block = event.getBlock();
		if (ItemUtils.isPickaxe(mPlayer.getInventory().getItemInMainHand()) && block.getType() == Material.SPAWNER) {
			onSpawnerBreak(mPlugin, mPlayer, block.getLocation().add(0.5, 0, 0.5), mRadius, mKnockback, mSpeed, mDuration);
		}
		return true;
	}

	private static Description<Whirlwind> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Whirlwind>(color)
			.add("Breaking a spawner knocks back all mobs within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks with a speed of ")
			.add(a -> a.mKnockback, KNOCKBACK_SPEED[rarity - 1], false, null, true)
			.add(". Additionally, you receive ")
			.addPercent(a -> a.mSpeed, SPEED[rarity - 1], false, true)
			.add(" speed for ")
			.addDuration(a -> a.mDuration, SPEED_DURATION)
			.add(" seconds.");
	}

}

