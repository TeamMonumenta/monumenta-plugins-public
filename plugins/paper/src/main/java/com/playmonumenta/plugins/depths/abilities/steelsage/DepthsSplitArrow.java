package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class DepthsSplitArrow extends DepthsAbility {

	private static final int SPLIT_ARROW_CHAIN_RANGE = 5;
	public static final String ABILITY_NAME = "Split Arrow";
	public static final double[] DAMAGE_MOD = {.40, .50, .60, .70, .80, 1.00};
	private static final PotionEffect SPECTRAL_ARROW_EFFECT = new PotionEffect(PotionEffectType.GLOWING, 200, 0);
	private static final int IFRAMES = 10;

	public DepthsSplitArrow(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayMaterial = Material.CHAIN;
		mTree = DepthsTree.METALLIC;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof AbstractArrow arrow && !arrow.hasMetadata(RapidFire.META_DATA_TAG) && EntityUtils.isHostileMob(enemy)) {
			LivingEntity nearestMob = EntityUtils.getNearestMob(enemy.getLocation(), SPLIT_ARROW_CHAIN_RANGE, enemy);

			if (nearestMob != null && !nearestMob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
				Location loc = enemy.getEyeLocation();
				Location eye = nearestMob.getEyeLocation();
				Vector dir = LocationUtils.getDirectionTo(eye, loc);
				World world = mPlayer.getWorld();
				for (int i = 0; i < 50; i++) {
					loc.add(dir.clone().multiply(0.1));
					world.spawnParticle(Particle.CRIT, loc, 2, 0.1, 0.1, 0.1, 0);
					if (loc.distance(eye) < 0.4) {
						break;
					}
				}

				if (!EntityUtils.hasArrowIframes(mPlugin, nearestMob)) {
					world.spawnParticle(Particle.CRIT, eye, 30, 0, 0, 0, 0.6);
					world.spawnParticle(Particle.CRIT_MAGIC, eye, 20, 0, 0, 0, 0.6);
					world.playSound(eye, Sound.ENTITY_ARROW_HIT, 1, 1.2f);

					if (arrow instanceof SpectralArrow) {
						nearestMob.addPotionEffect(SPECTRAL_ARROW_EFFECT);
					}

					DamageUtils.damage(mPlayer, nearestMob, DamageType.OTHER, event.getDamage() * DAMAGE_MOD[mRarity - 1], mInfo.mLinkedSpell, true);
					MovementUtils.knockAway(enemy, nearestMob, 0.125f, 0.35f, true);
					EntityUtils.applyArrowIframes(mPlugin, IFRAMES, nearestMob);
				}
			}
		}
		return false; // applies damage of type OTHER for damage of type PROJECTILE, which should not cause recursion with any other ability (or itself)
	}

	@Override
	public String getDescription(int rarity) {
		return "When you shoot an enemy with an arrow or trident, the nearest enemy within " + SPLIT_ARROW_CHAIN_RANGE + " blocks takes " + DepthsUtils.getRarityColor(rarity) + (int) DepthsUtils.roundPercent(DAMAGE_MOD[rarity - 1]) + "%" + ChatColor.WHITE + " of the arrow's damage. Bypasses iframes.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.METALLIC;
	}
}

