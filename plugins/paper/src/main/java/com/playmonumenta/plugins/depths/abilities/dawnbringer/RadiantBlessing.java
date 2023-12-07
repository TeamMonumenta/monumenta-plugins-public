package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RadiantBlessing extends DepthsAbility {

	public static final String ABILITY_NAME = "Radiant Blessing";
	private static final int HEALING_RADIUS = 18;
	private static final int COOLDOWN = 22 * 20;
	private static final double[] PERCENT_DAMAGE = {0.12, 0.15, 0.18, 0.21, 0.24, 0.3};
	private static final int DURATION = 10 * 20;
	private static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "RadiantBlessingPercentDamageReceivedEffect";
	private static final double PERCENT_DAMAGE_RECEIVED = 0.2;

	public static final String CHARM_COOLDOWN = "Radiant Blessing Cooldown";

	public static final DepthsAbilityInfo<RadiantBlessing> INFO =
		new DepthsAbilityInfo<>(RadiantBlessing.class, ABILITY_NAME, RadiantBlessing::new, DepthsTree.DAWNBRINGER, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.RADIANT_BLESSING)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", RadiantBlessing::cast, DepthsTrigger.SHIFT_LEFT_CLICK))
			.displayItem(Material.SUNFLOWER)
			.descriptions(RadiantBlessing::getDescription);

	private final double mRadius;
	private final double mDamageReduction;
	private final double mDamageBuff;
	private final int mDuration;

	public RadiantBlessing(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.RADIANT_BLESSING_RADIUS.mEffectName, HEALING_RADIUS);
		mDamageReduction = PERCENT_DAMAGE_RECEIVED + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.RADIANT_BLESSING_RESISTANCE_AMPLIFIER.mEffectName);
		mDamageBuff = PERCENT_DAMAGE[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.RADIANT_BLESSING_DAMAGE_AMPLIFIER.mEffectName);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.RADIANT_BLESSING_BUFF_DURATION.mEffectName, DURATION);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		Location userLoc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		for (Player p : PlayerUtils.playersInRange(userLoc, mRadius, true)) {
			mPlugin.mEffectManager.addEffect(p, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME, new PercentDamageReceived(mDuration, -mDamageReduction));
			mPlugin.mEffectManager.addEffect(p, ABILITY_NAME, new PercentDamageDealt(mDuration, mDamageBuff));

			Location loc = p.getLocation();
			new PartialParticle(Particle.VILLAGER_HAPPY, loc, 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.END_ROD, loc, 10, 0, 0, 0, 0.1).spawnAsPlayerActive(mPlayer);
			world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.5f, 1.6f);
			world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.5f, 1f);
			world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.5f, 1.75f);
			world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.5f, 1.0f);
			new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					mT++;
					if (mT % 5 == 0) {
						Location loc = p.getLocation().add(0, 1, 0);
						new PartialParticle(Particle.VILLAGER_HAPPY, loc, 3, 0.5, 0.5, 0.5, 0.001).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.WAX_OFF, loc, 3, 0.7, 0.5, 0.5, 0.001).spawnAsPlayerActive(mPlayer);
					}
					if (mT >= mDuration) {
						this.cancel();
						Location loc = p.getLocation().add(0, 1, 0);
						world.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.5f, 1f);
						world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.5f, 0.75f);
						world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.5f, 0.65f);
						new PartialParticle(Particle.SPELL_INSTANT, loc, 30, 0.5, 0.5, 0.5, 0.25).spawnAsPlayerActive(mPlayer);
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
		world.playSound(userLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.5f, 1f);
		world.playSound(userLoc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.5f, 2f);
		world.playSound(userLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.5f, 1.75f);
		world.playSound(userLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.05f, 1.0f);

		putOnCooldown();
	}

	private static Description<RadiantBlessing> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<RadiantBlessing>(color)
			.add("Left click while sneaking and holding a weapon to enchant players within ")
			.add(a -> a.mRadius, HEALING_RADIUS)
			.add(" blocks, including yourself, with ")
			.addPercent(a -> a.mDamageReduction, PERCENT_DAMAGE_RECEIVED)
			.add(" resistance and ")
			.addPercent(a -> a.mDamageBuff, PERCENT_DAMAGE[rarity - 1], false, true)
			.add(" increased damage for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN, false);
	}

}


