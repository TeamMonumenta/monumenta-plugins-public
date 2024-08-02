package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsCombosAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class WindsweptCombos extends DepthsCombosAbility {

	public static final String ABILITY_NAME = "Windswept Combos";
	private static final double[] COOLDOWN_REDUCTION = {0.05, 0.075, 0.1, 0.125, 0.15, 0.2};
	private static final int HIT_REQUIREMENT = 3;
	private static final int RADIUS = 4;
	private static final double PULL_STRENGTH = 0.5;
	public static final int CAP_TICKS = 5 * Constants.TICKS_PER_SECOND;

	public static final DepthsAbilityInfo<WindsweptCombos> INFO =
		new DepthsAbilityInfo<>(WindsweptCombos.class, ABILITY_NAME, WindsweptCombos::new, DepthsTree.WINDWALKER, DepthsTrigger.COMBO)
			.displayItem(Material.WHITE_CANDLE)
			.descriptions(WindsweptCombos::getDescription)
			.singleCharm(false);

	private final double mCooldownReduction;
	private final double mRadius;
	private final double mPull;

	public WindsweptCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO, HIT_REQUIREMENT, CharmEffects.WINDSWEPT_COMBOS_HIT_REQUIREMENT.mEffectName);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.WINDSWEPT_COMBOS_RADIUS.mEffectName, RADIUS);
		mPull = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.WINDSWEPT_COMBOS_PULL_STRENGTH.mEffectName, PULL_STRENGTH);
		mCooldownReduction = COOLDOWN_REDUCTION[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.WINDSWEPT_COMBOS_COOLDOWN_REDUCTION.mEffectName);
	}

	@Override
	public void activate(DamageEvent event, LivingEntity enemy) {
		activate(enemy, mPlayer, mPlugin, mRadius, mPull, mCooldownReduction);
	}

	public static void activate(LivingEntity enemy, Player player) {
		activate(enemy, player, Plugin.getInstance(), RADIUS, PULL_STRENGTH, COOLDOWN_REDUCTION[0]);
	}

	public static void activate(LivingEntity enemy, Player player, Plugin plugin, double radius, double pull, double cooldownReduction) {
		Location location = enemy.getLocation();
		for (LivingEntity e : EntityUtils.getNearbyMobs(location, radius, enemy)) {
			e.setVelocity(e.getVelocity().add(e.getLocation().toVector().subtract(location.subtract(0, 0.3, 0).toVector()).normalize().multiply(-pull).add(new Vector(0, 0.2, 0))));
		}

		for (Ability ability : AbilityManager.getManager().getPlayerAbilities(player).getAbilities()) {
			AbilityInfo<?> info = ability.getInfo();
			ClassAbility spell = info.getLinkedSpell();
			if (spell == null) {
				continue;
			}
			int totalCD = ability.getModifiedCooldown();
			int reducedCD = Math.min((int) (totalCD * cooldownReduction), CAP_TICKS);
			plugin.mTimers.updateCooldown(player, spell, reducedCD);
		}


		Location loc = player.getLocation().add(0, 1, 0);
		World world = player.getWorld();
		Location entityLoc = enemy.getEyeLocation().clone().subtract(0, 0.5, 0);
		playSounds(world, loc);
		new PartialParticle(Particle.EXPLOSION_NORMAL, entityLoc, 20, 0, 0.2, 0, 0.2).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FIREWORKS_SPARK, entityLoc, 20, 0, 0.2, 0, 0.2).spawnAsPlayerActive(player);
		for (double j = 0; j < 360; j += 12) {
			double radian = Math.toRadians(j);
			Location angleLoc = loc.clone().add(FastUtils.cos(radian) * radius, 0.15, FastUtils.sin(radian) * radius);
			new PartialParticle(Particle.CLOUD, angleLoc, 1, 0, 0, 0, 0.125).spawnAsPlayerActive(player);
		}
	}

	public static void playSounds(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0f, 1.6f);
		world.playSound(loc, Sound.ENTITY_SHULKER_BULLET_HURT, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.7f, 2.0f);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 2.0f, 0.5f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 1.0f, 1.7f);
	}

	private static Description<WindsweptCombos> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<WindsweptCombos>(color)
			.add("Every ")
			.add(a -> a.mHitRequirement, HIT_REQUIREMENT, true)
			.add(" melee strikes, gain ")
			.addPercent(a -> a.mCooldownReduction, COOLDOWN_REDUCTION[rarity - 1], false, true)
			.add(" cooldown reduction for your abilities (max ")
			.addDuration(CAP_TICKS)
			.add("s) and pull nearby mobs inwards in a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius.");
	}

}

