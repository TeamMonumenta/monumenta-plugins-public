package com.playmonumenta.plugins.depths.abilities.steelsage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

import net.md_5.bungee.api.ChatColor;

public class DepthsSplitArrow extends DepthsAbility {

	private static final int SPLIT_ARROW_CHAIN_RANGE = 5;
	public static final String ABILITY_NAME = "Split Arrow";
	public static final double[] DAMAGE_MOD = {.40, .50, .60, .70, .80};
	private static final PotionEffect SPECTRAL_ARROW_EFFECT = new PotionEffect(PotionEffectType.GLOWING, 200, 0);

	public DepthsSplitArrow(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.CHAIN;
		mTree = DepthsTree.METALLIC;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity le, EntityDamageByEntityEvent event) {
		LivingEntity damagee = le;
		if ((proj instanceof Arrow || proj instanceof SpectralArrow) && !proj.hasMetadata(DepthsRapidFire.META_DATA_TAG)) {
			LivingEntity nearestMob = EntityUtils.getNearestMob(damagee.getLocation(), SPLIT_ARROW_CHAIN_RANGE, damagee);

			if (nearestMob != null) {
				Location loc = damagee.getEyeLocation();
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
				world.spawnParticle(Particle.CRIT, eye, 30, 0, 0, 0, 0.6);
				world.spawnParticle(Particle.CRIT_MAGIC, eye, 20, 0, 0, 0, 0.6);
				world.playSound(eye, Sound.ENTITY_ARROW_HIT, 1, 1.2f);

				if (proj instanceof SpectralArrow) {
					nearestMob.addPotionEffect(SPECTRAL_ARROW_EFFECT);
				}

				EntityUtils.damageEntity(mPlugin, nearestMob, event.getDamage() * DAMAGE_MOD[mRarity - 1], mPlayer, MagicType.PHYSICAL, true, mInfo.mLinkedSpell, true, true, true, false);
				MovementUtils.knockAway(damagee, nearestMob, 0.125f, 0.35f);
			}
		}

		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "When you shoot an enemy with an arrow, the nearest enemy within " + SPLIT_ARROW_CHAIN_RANGE + " blocks takes " + DepthsUtils.getRarityColor(rarity) + DepthsUtils.roundPercent(DAMAGE_MOD[rarity - 1]) + "%" + ChatColor.WHITE + " of the arrow's damage. Bypasses iframes.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.METALLIC;
	}
}

