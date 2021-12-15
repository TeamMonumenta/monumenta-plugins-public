package com.playmonumenta.plugins.depths.abilities.shadow;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.md_5.bungee.api.ChatColor;

public class Brutalize extends DepthsAbility {

	public static final String ABILITY_NAME = "Brutalize";
	public static final double[] DAMAGE = {0.09, 0.12, 0.15, 0.18, 0.21, 0.27};
	public static final int RADIUS = 2;

	public Brutalize(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.STONE_SWORD;
		mTree = DepthsTree.SHADOWS;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {

		if (mPlayer != null && event.getCause().equals(DamageCause.ENTITY_ATTACK) && PlayerUtils.isFallingAttack(mPlayer)) {
			double originalDamage = event.getDamage();
			double brutalizeDamage = DAMAGE[mRarity - 1] * originalDamage;
			event.setDamage(originalDamage + brutalizeDamage);
			mPlayer.getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.75f, 1.65f);
			mPlayer.getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 0.5f);
			for (LivingEntity mob : EntityUtils.getNearbyMobs(event.getEntity().getLocation(), RADIUS)) {
				if (mob.getUniqueId() != event.getEntity().getUniqueId()) {
					EntityUtils.damageEntity(mPlugin, mob, brutalizeDamage, mPlayer, null, true, null, true, true, false, false);
					MovementUtils.knockAway(event.getEntity(), mob, 0.5f);

					mPlayer.getWorld().spawnParticle(Particle.SPELL_WITCH, mob.getLocation(), 10, 0.5, 0.2, 0.5, 0.65);
				}
			}
			mPlayer.getWorld().spawnParticle(Particle.SPELL_WITCH, event.getEntity().getLocation(), 15, 0.5, 0.2, 0.5, 0.65);

		}
		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "When you critically strike you deal " + DepthsUtils.getRarityColor(rarity) + (int) DepthsUtils.roundPercent(DAMAGE[rarity - 1]) + "%" + ChatColor.WHITE + " of the damage to enemies in a " + RADIUS + " block radius and knock them away from the target.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SHADOWS;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.PASSIVE;
	}
}

