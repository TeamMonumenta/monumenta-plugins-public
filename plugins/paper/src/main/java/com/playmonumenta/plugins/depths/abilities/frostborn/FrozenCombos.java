package com.playmonumenta.plugins.depths.abilities.frostborn;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;

import net.md_5.bungee.api.ChatColor;

public class FrozenCombos extends DepthsAbility {

	public static final String ABILITY_NAME = "Frigid Combos";
	public static final int TIME = 2 * 20;
	public static final double[] SLOW_AMPLIFIER = {0.3, 0.35, 0.4, 0.45, 0.5};
	public static final double[] DAMAGE = {2, 3, 4, 5, 6};
	public static final int RADIUS = 4;

	private int mComboCount = 0;

	public FrozenCombos(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.BLUE_DYE;
		mTree = DepthsTree.FROSTBORN;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (DepthsUtils.isValidComboAttack(event, mPlayer)) {
			mComboCount++;

			if (mComboCount >= 3 && mRarity > 0) {
				Location targetLoc = event.getEntity().getLocation();
				World world = targetLoc.getWorld();
				for (LivingEntity mob : EntityUtils.getNearbyMobs(targetLoc, RADIUS)) {
					if (!(mob.getHealth() <= 0 || mob == null)) {
						world.spawnParticle(Particle.CRIT_MAGIC, mob.getLocation(), 25, .5, .2, .5, 0.65);
						EntityUtils.applySlow(mPlugin, TIME, SLOW_AMPLIFIER[mRarity - 1], mob);
						EntityUtils.damageEntity(mPlugin, mob, DAMAGE[mRarity - 1], mPlayer, MagicType.ICE, true, mInfo.mLinkedSpell, true, true, true, false);
					}
				}

				mComboCount = 0;

				//Particles
				Location playerLoc = mPlayer.getLocation().add(0, 1, 0);
				world.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.65f);
				world.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.45f);
				world.spawnParticle(Particle.SNOW_SHOVEL, targetLoc, 25, .5, .2, .5, 0.65);
			}
		}

		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "Every third melee attack deals " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " damage to all mobs within " + RADIUS + " blocks and applies " + DepthsUtils.getRarityColor(rarity) + DepthsUtils.roundPercent(SLOW_AMPLIFIER[rarity - 1]) + "%" + ChatColor.WHITE + " slowness for " + TIME / 20.0 + " seconds to affected mobs.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FROSTBORN;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.COMBO;
	}
}

