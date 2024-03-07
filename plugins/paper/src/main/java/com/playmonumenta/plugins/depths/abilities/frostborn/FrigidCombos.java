package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsCombosAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.HashSet;
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
import org.jetbrains.annotations.Nullable;


public class FrigidCombos extends DepthsCombosAbility {

	public static final String ABILITY_NAME = "Frigid Combos";
	public static final int DURATION = 2 * 20;
	public static final double[] SLOW_AMPLIFIER = {0.2, 0.25, 0.3, 0.35, 0.4, 0.5};
	public static final int[] DAMAGE = {2, 3, 4, 5, 6, 8};
	public static final int[] SHATTER_DAMAGE = {8, 10, 12, 14, 16, 20};
	public static final int RADIUS = 4;
	public static final int SHATTER_RADIUS = 6;
	public static final int HIT_REQUIREMENT = 3;

	public static final DepthsAbilityInfo<FrigidCombos> INFO =
		new DepthsAbilityInfo<>(FrigidCombos.class, ABILITY_NAME, FrigidCombos::new, DepthsTree.FROSTBORN, DepthsTrigger.COMBO)
			.linkedSpell(ClassAbility.FRIGID_COMBOS)
			.displayItem(Material.BLUE_DYE)
			.descriptions(FrigidCombos::getDescription)
			.singleCharm(false);

	private final double mRadius;
	private final double mDamage;
	private final double mShatterRadius;
	private final double mShatterDamage;
	private final double mSlow;
	private final int mDuration;

	public FrigidCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO, HIT_REQUIREMENT, CharmEffects.FRIGID_COMBOS_HIT_REQUIREMENT.mEffectName);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.FRIGID_COMBOS_RADIUS.mEffectName, RADIUS);
		mShatterRadius = CharmManager.getRadius(mPlayer, CharmEffects.FRIGID_COMBOS_RADIUS.mEffectName, SHATTER_RADIUS);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.FRIGID_COMBOS_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mShatterDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.FRIGID_COMBOS_DAMAGE.mEffectName, SHATTER_DAMAGE[mRarity - 1]);
		mSlow = SLOW_AMPLIFIER[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.FRIGID_COMBOS_SLOW_AMPLIFIER.mEffectName);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.FRIGID_COMBOS_SLOW_DURATION.mEffectName, DURATION);
	}

	@Override
	public void activate(DamageEvent event, LivingEntity enemy) {
		activate(enemy, mPlayer, mPlugin, mRadius, mShatterRadius, mDamage, mShatterDamage, mSlow, mDuration, mInfo.getLinkedSpell());
	}

	public static void activate(LivingEntity enemy, Player player) {
		activate(enemy, player, Plugin.getInstance(), RADIUS, SHATTER_RADIUS, DAMAGE[0], SHATTER_DAMAGE[0], SLOW_AMPLIFIER[0], DURATION, null);
	}

	public static void activate(LivingEntity enemy, Player player, Plugin plugin, double normalRadius, double shatterRadius, double normalDamage, double shatterDamage, double slow, int duration, @Nullable ClassAbility classAbility) {
		Location targetLoc = enemy.getLocation();
		World world = targetLoc.getWorld();

		boolean isOnIce = isOnIce(enemy);
		double damage = isOnIce ? shatterDamage : normalDamage;
		double radius = isOnIce ? shatterRadius : normalRadius;

		Location playerLoc = player.getLocation().add(0, 1, 0);
		if (isOnIce) {
			HashSet<Location> iceToBreak = new HashSet<>(DepthsUtils.iceActive.keySet());
			iceToBreak.removeIf(l -> !l.isWorldLoaded() || l.getWorld() != targetLoc.getWorld() || l.distance(targetLoc) > 1.5 || !DepthsUtils.isIce(l.getBlock().getType()));
			for (Location l : iceToBreak) {
				Block b = l.getBlock();
				if (b.getType() == Permafrost.PERMAFROST_ICE_MATERIAL) {
					//If special permafrost ice, set to normal ice instead of destroying
					b.setType(DepthsUtils.ICE_MATERIAL);
				} else {
					b.setBlockData(DepthsUtils.iceActive.get(l));
					DepthsUtils.iceActive.remove(l);
				}
				new PartialParticle(Particle.BLOCK_CRACK, l.add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0, Material.ICE.createBlockData()).spawnAsPlayerActive(player);
			}

			new PartialParticle(Particle.EXPLOSION_NORMAL, LocationUtils.getHalfHeightLocation(enemy), 40, 0.1, 0.1, 0.1, 0.2).spawnAsPlayerActive(player);
			world.playSound(playerLoc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 1.5f);
			world.playSound(playerLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1f, 0.8f);
			world.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.5f, 1.0f);
			world.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.5f, 0.1f);
		}

		for (LivingEntity mob : EntityUtils.getNearbyMobs(targetLoc, radius)) {
			new PartialParticle(Particle.CRIT_MAGIC, LocationUtils.getHalfHeightLocation(mob), 25, .5, .2, .5, 0.65).spawnAsPlayerActive(player);
			EntityUtils.applySlow(plugin, duration, slow, mob);
			DamageUtils.damage(player, mob, DamageType.MAGIC, damage, classAbility, true);
		}

		playSounds(world, playerLoc);
		new PartialParticle(Particle.SNOW_SHOVEL, LocationUtils.getHalfHeightLocation(enemy), 25, .5, .2, .5, 0.65).spawnAsPlayerActive(player);
	}

	private static boolean isOnIce(LivingEntity entity) {
		Location loc = entity.getLocation();
		return DepthsUtils.isIce(loc.getBlock().getRelative(BlockFace.DOWN).getType()) && DepthsUtils.iceActive.containsKey(loc.getBlock().getRelative(BlockFace.DOWN).getLocation());
	}

	public static void playSounds(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 0.8f, 1.0f);
		world.playSound(loc, Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 0.8f, 0.6f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.0f, 1.4f);
	}

	private static Description<FrigidCombos> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<FrigidCombos>(color)
			.add("Every ")
			.add(a -> a.mHitRequirement, HIT_REQUIREMENT, true, null, false)
			.add(" melee attacks, deal ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage to mobs within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks and apply ")
			.addPercent(a -> a.mSlow, SLOW_AMPLIFIER[rarity - 1], false, true)
			.add(" slowness to them for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds. If the mob was standing on ice, shatter it and increase the damage to ")
			.addDepthsDamage(a -> a.mShatterDamage, SHATTER_DAMAGE[rarity - 1], true)
			.add(" and the radius to ")
			.add(a -> a.mShatterRadius, SHATTER_RADIUS)
			.add(" blocks instead.");
	}


}

