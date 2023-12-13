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
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class WardOfLight extends DepthsAbility {

	public static final String ABILITY_NAME = "Ward of Light";
	public static final double[] HEAL = {0.32, 0.4, 0.48, 0.56, 0.64, 1.0};
	private static final int HEALING_RADIUS = 12;
	private static final double HEALING_CONE_ANGLE = 70;
	private static final int COOLDOWN = 12 * 20;

	public static final String CHARM_COOLDOWN = "Ward of Light Cooldown";

	public static final DepthsAbilityInfo<WardOfLight> INFO =
		new DepthsAbilityInfo<>(WardOfLight.class, ABILITY_NAME, WardOfLight::new, DepthsTree.DAWNBRINGER, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.WARD_OF_LIGHT)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WardOfLight::cast, DepthsTrigger.RIGHT_CLICK))
			.displayItem(Material.LANTERN)
			.descriptions(WardOfLight::getDescription);

	private final double mHealPercent;
	private final double mRadius;
	private final double mConeAngle;

	public WardOfLight(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mHealPercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.WARD_OF_LIGHT_HEALING.mEffectName, HEAL[mRarity - 1]);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.WARD_OF_LIGHT_HEAL_RADIUS.mEffectName, HEALING_RADIUS);
		mConeAngle = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.WARD_OF_LIGHT_CONE_ANGLE.mEffectName, HEALING_CONE_ANGLE);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		World world = mPlayer.getWorld();
		Vector playerDir = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
		boolean healed = false;
		double dotAngle = FastUtils.cosDeg(mConeAngle);
		for (Player p : PlayerUtils.otherPlayersInRange(mPlayer, mRadius, true)) {
			Vector toMobVector = p.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();

			// Only heal players in the correct direction
			if (playerDir.dot(toMobVector) > dotAngle || p.getLocation().distance(mPlayer.getLocation()) < 2) {

				PlayerUtils.healPlayer(mPlugin, p, EntityUtils.getMaxHealth(p) * mHealPercent, mPlayer);

				Location loc = p.getLocation();
				new PartialParticle(Particle.HEART, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
				world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 2.0f, 1.6f);
				world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.05f, 1.0f);

				healed = true;
			}
		}

		if (healed) {
			world.playSound(mPlayer.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 2.0f, 1.6f);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.05f, 1.0f);

			ParticleUtils.explodingConeEffectSkill(mPlugin, mPlayer, (float) mRadius, Particle.SPIT, 0.35f, Particle.PORTAL, 3.0f, dotAngle, mPlayer);
			putOnCooldown();
		}

		return true;
	}

	private static Description<WardOfLight> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<WardOfLight>(color)
			.add("Right click while holding a weapon and not sneaking to heal nearby players within ")
			.add(a -> a.mRadius, HEALING_RADIUS)
			.add(" blocks in front of you for ")
			.addPercent(a -> a.mHealPercent, HEAL[rarity - 1], false, true)
			.add(" of their max health.")
			.addCooldown(COOLDOWN);
	}

}

