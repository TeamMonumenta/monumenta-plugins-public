package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsCombosAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
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

public class EarthenCombos extends DepthsCombosAbility {

	public static final String ABILITY_NAME = "Earthen Combos";
	public static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "EarthenCombosPercentDamageReceivedEffect";
	private static final double[] PERCENT_DAMAGE_RECEIVED = {0.08, 0.1, 0.12, 0.14, 0.16, 0.20};
	private static final int DURATION = 20 * 4;
	private static final int ROOT_DURATION = 25;
	private static final int HIT_REQUIREMENT = 3;

	public static final DepthsAbilityInfo<EarthenCombos> INFO =
		new DepthsAbilityInfo<>(EarthenCombos.class, ABILITY_NAME, EarthenCombos::new, DepthsTree.EARTHBOUND, DepthsTrigger.COMBO)
			.displayItem(Material.WOODEN_SWORD)
			.descriptions(EarthenCombos::getDescription)
			.singleCharm(false);

	private final double mDamageReduction;
	private final int mEffectDuration;
	private final int mRootDuration;

	public EarthenCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO, HIT_REQUIREMENT, CharmEffects.EARTHEN_COMBOS_HIT_REQUIREMENT.mEffectName);
		mDamageReduction = PERCENT_DAMAGE_RECEIVED[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.EARTHEN_COMBOS_RESISTANCE_AMPLIFIER.mEffectName);
		mEffectDuration = CharmManager.getDuration(mPlayer, CharmEffects.EARTHEN_COMBOS_EFFECT_DURATION.mEffectName, DURATION);
		mRootDuration = CharmManager.getDuration(mPlayer, CharmEffects.EARTHEN_COMBOS_ROOT_DURATION.mEffectName, ROOT_DURATION);
	}

	@Override
	public void activate(DamageEvent event, LivingEntity enemy) {
		activate(enemy, mPlayer, mPlugin, mDamageReduction, mEffectDuration, mRootDuration);
	}

	public static void activate(LivingEntity enemy, Player player) {
		activate(enemy, player, Plugin.getInstance(), PERCENT_DAMAGE_RECEIVED[0], DURATION, ROOT_DURATION);
	}

	public static void activate(LivingEntity enemy, Player player, Plugin plugin, double damageReduction, int effectDuration, int rootDuration) {
		plugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME, new PercentDamageReceived(effectDuration, -damageReduction));
		EntityUtils.applySlow(plugin, rootDuration, .99, enemy);

		Location loc = player.getLocation().add(0, 1, 0);
		World world = player.getWorld();
		Location entityLoc = enemy.getLocation();
		playSounds(world, loc);
		new PartialParticle(Particle.CRIT_MAGIC, entityLoc.add(0, 1, 0), 10, 0.5, 0.2, 0.5, 0.65).spawnAsPlayerActive(player);
		new PartialParticle(Particle.BLOCK_DUST, loc, 15, 0.5, 0.3, 0.5, 0.5, Material.PODZOL.createBlockData()).spawnAsPlayerActive(player);
		new PartialParticle(Particle.BLOCK_DUST, loc, 15, 0.5, 0.3, 0.5, 0.5, Material.ANDESITE.createBlockData()).spawnAsPlayerActive(player);
	}

	public static void playSounds(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 0.6f, 0.9f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 0.6f, 0.1f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.4f, 0.1f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.6f, 1.4f);
	}

	private static Description<EarthenCombos> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<EarthenCombos>(color)
			.add("Every ")
			.add(a -> a.mHitRequirement, HIT_REQUIREMENT, true)
			.add(" melee strikes, gain ")
			.addPercent(a -> a.mDamageReduction, PERCENT_DAMAGE_RECEIVED[rarity - 1], false, true)
			.add(" resistance for ")
			.addDuration(a -> a.mEffectDuration, DURATION)
			.add(" seconds and roots the enemy for ")
			.addDuration(a -> a.mRootDuration, ROOT_DURATION)
			.add(" seconds.");
	}

}

