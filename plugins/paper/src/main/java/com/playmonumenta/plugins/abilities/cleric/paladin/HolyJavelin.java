package com.playmonumenta.plugins.abilities.cleric.paladin;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
* Attacking while sprinting throws a spear of light in a 12 block line, dealing
* 10 / 20 damage to undead and 5 / 10 damage to all others, also lights all
* targets on fire for 5s. (7s cooldown)
*/

public class HolyJavelin extends Ability {

	private static final Particle.DustOptions HOLY_JAVELIN_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 50), 1.0f);
	private static final double HOLY_JAVELIN_RADIUS = 0.75;
	private static final int HOLY_JAVELIN_RANGE = 12;
	private static final int HOLY_JAVELIN_1_UNDEAD_DAMAGE = 10;
	private static final int HOLY_JAVELIN_2_UNDEAD_DAMAGE = 20;
	private static final int HOLY_JAVELIN_1_DAMAGE = 5;
	private static final int HOLY_JAVELIN_2_DAMAGE = 10;
	private static final int HOLY_JAVELIN_FIRE_DURATION = 5 * 20;
	private static final int HOLY_JAVELIN_1_COOLDOWN = 7 * 20;
	private static final int HOLY_JAVELIN_2_COOLDOWN = 8 * 20;

	private int mDamage;
	private int mDamageUndead;

	public HolyJavelin(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Holy Javelin");
		mInfo.linkedSpell = Spells.HOLY_JAVELIN;
		mInfo.scoreboardId = "HolyJavelin";
		mInfo.mShorthandName = "HJ";
		mInfo.mDescriptions.add("Sprint left-clicking while not holding a pickaxe throws a piercing spear of light 12 blocks dealing 10 damage to undead and 5 damage to all others. All hit enemies are set on fire for 5s. Cooldown: 7s.");
		mInfo.mDescriptions.add("Damage is increased to 20 to undead and 10 to all others.");
		mInfo.cooldown = getAbilityScore() == 1 ? HOLY_JAVELIN_1_COOLDOWN : HOLY_JAVELIN_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		mDamage = getAbilityScore() == 1 ? HOLY_JAVELIN_1_DAMAGE : HOLY_JAVELIN_2_DAMAGE;
		mDamageUndead = getAbilityScore() == 1 ? HOLY_JAVELIN_1_UNDEAD_DAMAGE : HOLY_JAVELIN_2_UNDEAD_DAMAGE;
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSprinting() && !InventoryUtils.isPickaxeItem(mainHand);
	}

	@Override
	public void cast(Action action) {
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 0.9f);
		Location playerLoc = mPlayer.getEyeLocation();
		Location location = playerLoc.clone();
		Vector increment = location.getDirection();
		mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, location.clone().add(increment), 10, 0, 0, 0, 0.125f);

		// Get a list of all the mobs this could possibly hit (that are within range of the player)
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(location, HOLY_JAVELIN_RANGE, mPlayer);
		BoundingBox box = BoundingBox.of(playerLoc, HOLY_JAVELIN_RADIUS, HOLY_JAVELIN_RADIUS, HOLY_JAVELIN_RADIUS);
		for (int i = 0; i < HOLY_JAVELIN_RANGE; i++) {
			box.shift(increment);
			Location loc = box.getCenter().toLocation(mWorld);
			mWorld.spawnParticle(Particle.REDSTONE, loc, 22, 0.25, 0.25, 0.25, HOLY_JAVELIN_COLOR);
			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0f, 0f, 0f, 0.025f);

			Iterator<LivingEntity> iter = mobs.iterator();
			while (iter.hasNext()) {
				LivingEntity mob = iter.next();
				if (mob.getBoundingBox().overlaps(box)) {
					if (EntityUtils.isUndead(mob)) {
						EntityUtils.damageEntity(mPlugin, mob, mDamageUndead, mPlayer, MagicType.HOLY, true, mInfo.linkedSpell);
					} else {
						EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.HOLY, true, mInfo.linkedSpell);
					}
					EntityUtils.applyFire(mPlugin, HOLY_JAVELIN_FIRE_DURATION, mob);
					iter.remove();
				}
			}

			if (loc.getBlock().getType().isSolid()) {
				loc.subtract(increment.multiply(0.5));
				mWorld.spawnParticle(Particle.CLOUD, loc, 30, 0, 0, 0, 0.125f);
				mWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
				mWorld.playSound(loc, Sound.ENTITY_ARROW_HIT, 1, 0.9f);
				break;
			}
		}

		putOnCooldown();
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
			cast(Action.LEFT_CLICK_AIR);
		}

		return true;
	}

}
