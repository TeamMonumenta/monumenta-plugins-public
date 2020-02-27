package com.playmonumenta.plugins.abilities.warlock;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class GraspingClaws extends Ability {

	private static final int GRASPING_CLAWS_RADIUS = 8;
	private static final float GRASPING_CLAWS_SPEED = 0.175f;
	private static final int GRASPING_CLAWS_EFFECT_LEVEL = 3;
	private static final int GRASPING_CLAWS_1_DAMAGE = 3;
	private static final int GRASPING_CLAWS_2_DAMAGE = 8;
	private static final int GRASPING_CLAWS_DURATION = 8 * 20;
	private static final int GRASPING_CLAWS_COOLDOWN = 16 * 20;

	private Arrow arrow = null;

	public GraspingClaws(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Grasping Claws");
		mInfo.scoreboardId = "GraspingClaws";
		mInfo.mShorthandName = "GC";
		mInfo.mDescriptions.add("Left-clicking while shifted while holding a bow fires an arrow that pulls nearby enemies towards your arrow once it makes contact with a mob or block. Mobs caught in the arrow's 6 block radius are given Slowness 3 for 8 seconds and take 3 damage. (Cooldown: 16s)");
		mInfo.mDescriptions.add("The pulled enemies now take 8 damage, the arrow's radius is increased to 8.");
		mInfo.linkedSpell = Spells.GRASPING_CLAWS;
		mInfo.cooldown = GRASPING_CLAWS_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		mInfo.ignoreCooldown = true;
	}

	@Override
	public void cast(Action action) {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.GRASPING_CLAWS) && mPlayer.isSneaking() && InventoryUtils.isBowItem(inMainHand)) {
			arrow = mPlayer.launchProjectile(Arrow.class);
			arrow.setDamage(0);
			arrow.setVelocity(mPlayer.getLocation().getDirection().multiply(1.5));
			mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.SPELL_WITCH);
			putOnCooldown();
		}
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Arrow arrow) {
		if (this.arrow != null && this.arrow == arrow) {
			this.arrow = null;
			Location loc = arrow.getLocation();
			World world = arrow.getWorld();

			world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1.25f, 1.25f);
			world.playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, 1.25f, 1.45f);
			world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, 1.25f, 0.65f);
			world.spawnParticle(Particle.PORTAL, loc, 125, 2, 2, 2, 0.25);
			world.spawnParticle(Particle.PORTAL, loc, 400, 0, 0, 0, 1.45);
			world.spawnParticle(Particle.DRAGON_BREATH, loc, 85, 0, 0, 0, 0.125);
			world.spawnParticle(Particle.FALLING_DUST, loc, 150, 2, 2, 2, Material.ANVIL.createBlockData());


			int damage = (getAbilityScore() == 1) ? GRASPING_CLAWS_1_DAMAGE : GRASPING_CLAWS_2_DAMAGE;

			for (LivingEntity mob : EntityUtils.getNearbyMobs(arrow.getLocation(), GRASPING_CLAWS_RADIUS, mPlayer)) {
				EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer, MagicType.DARK_MAGIC);
				MovementUtils.pullTowards(arrow, mob, GRASPING_CLAWS_SPEED);
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, GRASPING_CLAWS_DURATION, GRASPING_CLAWS_EFFECT_LEVEL, false, true));
			}

			arrow.remove();
		}
	}

	public boolean onCooldown() {
		return mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell);
	}
}
