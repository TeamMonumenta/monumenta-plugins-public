package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
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
import org.jetbrains.annotations.Nullable;

public class VolcanicCombos extends DepthsCombosAbility {
	public static final String ABILITY_NAME = "Volcanic Combos";
	public static final int[] DAMAGE = {6, 8, 10, 12, 14, 18};
	public static final int RADIUS = 4;
	public static final int FIRE_TICKS = 3 * 20;
	public static final int HIT_REQUIREMENT = 3;

	public static final DepthsAbilityInfo<VolcanicCombos> INFO =
		new DepthsAbilityInfo<>(VolcanicCombos.class, ABILITY_NAME, VolcanicCombos::new, DepthsTree.FLAMECALLER, DepthsTrigger.COMBO)
			.displayItem(Material.BLAZE_ROD)
			.descriptions(VolcanicCombos::getDescription)
			.singleCharm(false);

	private final double mRadius;
	private final double mDamage;
	private final int mFireDuration;

	public VolcanicCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO, HIT_REQUIREMENT, CharmEffects.VOLCANIC_COMBOS_HIT_REQUIREMENT.mEffectName);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.VOLCANIC_COMBOS_RADIUS.mEffectName, RADIUS);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.VOLCANIC_COMBOS_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mFireDuration = CharmManager.getDuration(mPlayer, CharmEffects.VOLCANIC_COMBOS_FIRE_DURATION.mEffectName, FIRE_TICKS);
	}

	@Override
	public void activate(DamageEvent event, LivingEntity enemy) {
		activate(enemy, mPlayer, mPlugin, mRadius, mDamage, mFireDuration, mInfo.getLinkedSpell());
	}

	public static void activate(LivingEntity enemy, Player player) {
		activate(enemy, player, Plugin.getInstance(), RADIUS, DAMAGE[0], FIRE_TICKS, null);
	}

	public static void activate(LivingEntity enemy, Player player, Plugin plugin, double radius, double damage, int fireDuration, @Nullable ClassAbility classAbility) {
		Location location = enemy.getLocation();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(location, radius)) {
			EntityUtils.applyFire(plugin, fireDuration, mob, player);
			DamageUtils.damage(player, mob, DamageType.MAGIC, damage, classAbility, true);
		}
		World world = player.getWorld();
		for (int i = 0; i < 360; i += 12) {
			double rad = Math.toRadians(i);
			Location locationDelta = new Location(world, radius / 2.0 * FastUtils.cos(rad), 0.5, radius / 2.0 * FastUtils.sin(rad));
			location.add(locationDelta);
			new PartialParticle(Particle.FLAME, location, 2).spawnAsPlayerActive(player);
			location.subtract(locationDelta);
		}
		new PartialParticle(Particle.LAVA, location, 25, 0, 0.2, 0, 1).spawnAsPlayerActive(player);
		world.playSound(location, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1f, 1);
		world.playSound(location, Sound.ITEM_FLINTANDSTEEL_USE, SoundCategory.PLAYERS, 1f, 1);
	}

	private static Description<VolcanicCombos> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<VolcanicCombos>(color)
			.add("Every ")
			.add(a -> a.mHitRequirement, HIT_REQUIREMENT, true)
			.add(" melee attacks, deal ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage to enemies in a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius and set those enemies on fire for ")
			.addDuration(a -> a.mFireDuration, FIRE_TICKS)
			.add(" seconds.");
	}

}
