package com.playmonumenta.plugins.depths.abilities.windwalker;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

import net.md_5.bungee.api.ChatColor;

public class DepthsDisengage extends DepthsAbility {

	public static final String ABILITY_NAME = "Disengage";
	public static final int[] COOLDOWN = {16, 14, 12, 10, 8};
	private static final int RADIUS = 3;
	private static final float KNOCKBACK_SPEED = 0.5f;
	private static final double VELOCITY = 1.65;

	public DepthsDisengage(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.RABBIT_FOOT;
		mTree = DepthsTree.WINDWALKER;
		mInfo.mCooldown = (mRarity == 0) ? 16 * 20 : COOLDOWN[mRarity - 1] * 20;
		mInfo.mLinkedSpell = ClassAbility.DISENGAGE;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public void cast(Action trigger) {
		putOnCooldown();
		World world = mPlayer.getWorld();

		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RADIUS, mPlayer)) {
			if (!EntityUtils.isBoss(mob)) {
				MovementUtils.knockAway(mPlayer.getLocation(), mob, KNOCKBACK_SPEED, KNOCKBACK_SPEED / 2);
			}
		}

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 2);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1, 1.2f);
		world.spawnParticle(Particle.CLOUD, mPlayer.getLocation(), 15, 0.1f, 0, 0.1f, 0.125f);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation(), 10, 0.1f, 0, 0.1f, 0.15f);
		world.spawnParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 25, 0.1f, 0, 0.1f, 0.15f);
		Vector velocity = mPlayer.getLocation().getDirection().normalize().multiply(-VELOCITY);
		if (velocity.getY() < 0) {
			velocity.setY(0);
		}
		mPlayer.setVelocity(velocity.setY(velocity.getY() * 0.5 + 0.4));
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
	    if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
	        cast(Action.LEFT_CLICK_AIR);
	    }

	    return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "Left click while sneaking and holding a weapon to leap backwards, knocking back enemies in a " + RADIUS + " block radius. Cooldown: " + DepthsUtils.getRarityColor(rarity) + COOLDOWN[rarity - 1] + "s" + ChatColor.WHITE + ".";
	}

	@Override
	public boolean runCheck() {
		return (mPlayer.isSneaking() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand()));
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.WINDWALKER;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_LEFT_CLICK;
	}
}

