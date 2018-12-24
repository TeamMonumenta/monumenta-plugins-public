package com.playmonumenta.plugins.abilities.warlock;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class GraspingClaws extends Ability {

	private static final int GRASPING_CLAWS_RADIUS = 6;
	private static final float GRASPING_CLAWS_SPEED = 0.175f;
	private static final int GRASPING_CLAWS_EFFECT_LEVEL = 3;
	private static final int GRASPING_CLAWS_1_DAMAGE = 3;
	private static final int GRASPING_CLAWS_2_DAMAGE = 7;
	private static final int GRASPING_CLAWS_DURATION = 8 * 20;
	private static final int GRASPING_CLAWS_COOLDOWN = 16 * 20;

	public GraspingClaws(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 7;
		mInfo.specId = -1;
		mInfo.scoreboardId = "GraspingClaws";
		mInfo.linkedSpell = Spells.GRASPING_CLAW;
		mInfo.cooldown = GRASPING_CLAWS_COOLDOWN;
	}

	@Override
	public void ProjectileHitEvent(ProjectileHitEvent event, Arrow arrow) {
		Location loc = arrow.getLocation();
		World world = arrow.getWorld();

		world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.8f);
		world.spawnParticle(Particle.ENCHANTMENT_TABLE, loc.add(0, 1, 0), 200, 3, 3, 3, 0.0);
		world.spawnParticle(Particle.DRAGON_BREATH, loc, 75, 1, 1, 1, 0.0);

		int damage = (getAbilityScore() == 1) ? GRASPING_CLAWS_1_DAMAGE : GRASPING_CLAWS_2_DAMAGE;

		for (LivingEntity mob : EntityUtils.getNearbyMobs(arrow.getLocation(), GRASPING_CLAWS_RADIUS)) {
			EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer);
			MovementUtils.PullTowards(arrow, mob, GRASPING_CLAWS_SPEED);
			mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, GRASPING_CLAWS_DURATION, GRASPING_CLAWS_EFFECT_LEVEL, false, true));
			EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer);
		}

		// Put Grasping Claws on cooldown
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking();
	}

}
