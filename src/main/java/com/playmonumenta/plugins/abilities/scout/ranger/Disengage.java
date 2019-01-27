package com.playmonumenta.plugins.abilities.scout.ranger;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
* Sneak right click (without a bow) to leap backwards 6 ish blocks from your
* position, with a bit of vertical velocity as well. Enemies within melee range
* of you previous position are stunned for 4 seconds (does not work on elites
* and bosses) (Cooldown: 12 seconds) At Level 2, you deal 10 damage.
*/

public class Disengage extends Ability {

	private static final double DISENGAGE_VELOCITY_MULTIPLIER = 1.65;
	private static final double DISENGAGE_Y_VELOCITY = 0.65;
	private static final double DISENGAGE_STUN_RADIUS = 3;
	private static final int DISENGAGE_STUN_DURATION = 4 * 20;
	private static final int DISENGAGE_1_DAMAGE = 0;
	private static final int DISENGAGE_2_DAMAGE = 10;
	private static final int DISENGAGE_COOLDOWN = 12 * 20;

	public Disengage(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.DISENGAGE;
		mInfo.scoreboardId = "Disengage";
		mInfo.cooldown = DISENGAGE_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public boolean runCheck() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack inOffHand = mPlayer.getInventory().getItemInOffHand();
		return mPlayer.isSneaking() && !InventoryUtils.isBowItem(inMainHand) && !InventoryUtils.isBowItem(inOffHand);
	}

	@Override
	public boolean cast() {
		for (LivingEntity le : EntityUtils.getNearbyMobs(mPlayer.getLocation(), DISENGAGE_STUN_RADIUS)) {
			EntityUtils.applyStun(mPlugin, DISENGAGE_STUN_DURATION, le);
			int damage = getAbilityScore() == 1 ? DISENGAGE_1_DAMAGE : DISENGAGE_2_DAMAGE;
			EntityUtils.damageEntity(mPlugin, le, damage, mPlayer);
		}

		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 2);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1, 1.2f);
		mWorld.spawnParticle(Particle.CLOUD, mPlayer.getLocation(), 15, 0.1f, 0, 0.1f, 0.125f);
		mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation(), 10, 0.1f, 0, 0.1f, 0.15f);
		mWorld.spawnParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 25, 0.1f, 0, 0.1f, 0.15f);
		Vector dir = mPlayer.getLocation().getDirection().setY(0).normalize();
		double xVelocity = dir.getX() * DISENGAGE_VELOCITY_MULTIPLIER;
		double zVelocity = dir.getZ() * DISENGAGE_VELOCITY_MULTIPLIER;
		mPlayer.setVelocity(new Vector(-xVelocity, DISENGAGE_Y_VELOCITY, -zVelocity));
		putOnCooldown();
		return true;
	}
}
