package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsCombosAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SoothingCombos extends DepthsCombosAbility {

	public static final String ABILITY_NAME = "Soothing Combos";
	public static final double[] HEAL = {0.06, 0.07, 0.08, 0.09, 0.10, 0.12};
	public static final double[] SPEED_PERCENT = {0.1, 0.125, 0.15, 0.175, 0.2, 0.25};
	public static final String SPEED_EFFECT_NAME = "SoothingCombosPercentSpeedEffect";
	public static final int DURATION = 6 * 20;
	public static final int RANGE = 12;
	public static final int HIT_REQ = 3;

	public static final DepthsAbilityInfo<SoothingCombos> INFO =
		new DepthsAbilityInfo<>(SoothingCombos.class, ABILITY_NAME, SoothingCombos::new, DepthsTree.DAWNBRINGER, DepthsTrigger.COMBO)
			.displayItem(Material.HONEYCOMB)
			.descriptions(SoothingCombos::getDescription)
			.singleCharm(false);

	private final double mHeal;
	private final int mDuration;
	private final double mSpeed;
	private final int mHaste;
	private final double mRange;

	public SoothingCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO, HIT_REQ, CharmEffects.SOOTHING_COMBOS_HIT_REQUIREMENT.mEffectName);
		mHeal = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.SOOTHING_COMBOS_HEALING.mEffectName, HEAL[mRarity - 1]);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.SOOTHING_COMBOS_DURATION.mEffectName, DURATION);
		mSpeed = SPEED_PERCENT[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.SOOTHING_COMBOS_SPEED_AMPLIFIER.mEffectName);
		mHaste = (int) CharmManager.getLevel(mPlayer, CharmEffects.SOOTHING_COMBOS_HASTE_LEVEL.mEffectName) + mRarity == 6 ? 1 : 0;
		mRange = CharmManager.getRadius(mPlayer, CharmEffects.SOOTHING_COMBOS_RANGE.mEffectName, RANGE);
	}

	@Override
	public void activate(DamageEvent event, LivingEntity enemy) {
		activate(mPlayer, mPlugin, mHeal, mDuration, mSpeed, mHaste, mRange);
	}

	public static void activate(Player player) {
		activate(player, Plugin.getInstance(), HEAL[0], DURATION, SPEED_PERCENT[0], 0, RANGE);
	}

	public static void activate(Player player, Plugin plugin, double heal, int duration, double speed, int haste, double range) {
		PotionEffect hasteEffect = new PotionEffect(PotionEffectType.FAST_DIGGING, duration, haste, false, true);

		Location playerLoc = player.getLocation();
		World world = player.getWorld();
		List<Player> nearPlayers = PlayerUtils.playersInRange(playerLoc, range / 2.0, true);
		for (Player p : nearPlayers) {

			double healAmount = EntityUtils.getMaxHealth(p) * heal;
			if (p != player) {
				healAmount *= 1.5;
			}
			double healed = PlayerUtils.healPlayer(plugin, p, healAmount, player);
			if (healed > 0) {
				new PartialParticle(Particle.HEART, LocationUtils.getHalfHeightLocation(p), 6, 0.5, 1, 0.5, 0).spawnAsPlayerActive(player);
			}
		}

		List<Player> farPlayers = PlayerUtils.playersInRange(playerLoc, range, true);
		for (Player p : farPlayers) {
			p.addPotionEffect(hasteEffect);
			plugin.mEffectManager.addEffect(p, SPEED_EFFECT_NAME, new PercentSpeed(duration, speed, SPEED_EFFECT_NAME));
			new PartialParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(player);
			new PartialParticle(Particle.VILLAGER_HAPPY, p.getLocation().add(0, 1, 0), 5, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(player);
			world.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.6f);
		}

		Location loc = playerLoc.clone().add(0, 1, 0);
		world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.6f);
		new PartialParticle(Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(player);
	}

	private static Description<SoothingCombos> getDescription(int rarity, TextColor color) {
		TextComponent haste = rarity == 6 ? Component.text("II", color) : Component.text("I");
		return new DescriptionBuilder<SoothingCombos>(color)
			.add("Every ")
			.add(a -> a.mHitRequirement, HIT_REQ, true)
			.add(" melee strikes, heal all players within ")
			.add(a -> a.mRange / 2, RANGE / 2.0)
			.add(" blocks for ")
			.addPercent(a -> a.mHeal, HEAL[rarity - 1], false, true)
			.add(" of their max health. Other players receive 50% more healing. Additionally, give ")
			.addPercent(a -> a.mSpeed, SPEED_PERCENT[rarity - 1], false, true)
			.add(" speed and Haste ")
			.add(haste)
			.add(" for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds to all players within ")
			.add(a -> a.mRange, RANGE)
			.add(" blocks.");
	}

}

