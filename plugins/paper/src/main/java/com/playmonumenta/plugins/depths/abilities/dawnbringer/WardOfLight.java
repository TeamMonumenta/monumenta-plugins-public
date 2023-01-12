package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class WardOfLight extends DepthsAbility {

	public static final String ABILITY_NAME = "Ward of Light";
	public static final double[] HEAL = {0.32, 0.4, 0.48, 0.56, 0.64, 1.0};
	private static final int HEALING_RADIUS = 12;
	private static final double HEALING_DOT_ANGLE = 0.33;
	private static final int COOLDOWN = 12 * 20;

	public static final DepthsAbilityInfo<WardOfLight> INFO =
		new DepthsAbilityInfo<>(WardOfLight.class, ABILITY_NAME, WardOfLight::new, DepthsTree.DAWNBRINGER, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.WARD_OF_LIGHT)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WardOfLight::cast,
				new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false), HOLDING_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.LANTERN))
			.descriptions(WardOfLight::getDescription, MAX_RARITY);

	public WardOfLight(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		Vector playerDir = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
		int count = 0;
		for (Player p : PlayerUtils.otherPlayersInRange(mPlayer, HEALING_RADIUS, true)) {
			Vector toMobVector = p.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();

			// Only heal players in the correct direction
			// Don't heal players that have their class disabled (so it doesn't work on arena contenders)
			// Don't heal players with PvP enabled
			// If the source player was included (because PvP is on), heal them
			if (p.equals(mPlayer)
			    || (!p.getScoreboardTags().contains("disable_class")
			        && !AbilityManager.getManager().isPvPEnabled(mPlayer)
			        && (playerDir.dot(toMobVector) > HEALING_DOT_ANGLE
			        || p.getLocation().distance(mPlayer.getLocation()) < 2))) {

				PlayerUtils.healPlayer(mPlugin, p, EntityUtils.getMaxHealth(p) * HEAL[mRarity - 1], mPlayer);

				Location loc = p.getLocation();
				new PartialParticle(Particle.HEART, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
				mPlayer.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 2.0f, 1.6f);
				mPlayer.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.05f, 1.0f);

				count++;
			}
		}

		if (count > 0) {
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 2.0f, 1.6f);
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.05f, 1.0f);

			ParticleUtils.explodingConeEffect(mPlugin, mPlayer, HEALING_RADIUS, Particle.SPIT, 0.35f, Particle.PORTAL, 3.0f, HEALING_DOT_ANGLE);
			putOnCooldown();
		}
	}

	private static String getDescription(int rarity) {
		return "Right click while holding a weapon and not sneaking to heal nearby players within " + HEALING_RADIUS + " blocks in front of you for " + DepthsUtils.getRarityColor(rarity) + (int) DepthsUtils.roundPercent(HEAL[rarity - 1]) + "%" + ChatColor.WHITE + " of their max health. Cooldown: " + COOLDOWN / 20 + "s.";
	}

}

