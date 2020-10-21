package com.playmonumenta.plugins.abilities.cleric.paladin;

import java.util.Iterator;
import java.util.List;

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

public class HolyJavelin extends Ability {

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 50), 1.0f);
	private static final double HITBOX_LENGTH = 0.75;
	private static final int RANGE = 12;
	private static final int UNDEAD_DAMAGE_1 = 12;
	private static final int UNDEAD_DAMAGE_2 = 24;
	private static final int DAMAGE_1 = 5;
	private static final int DAMAGE_2 = 10;
	private static final int FIRE_DURATION = 5 * 20;
	private static final int COOLDOWN = 7 * 20;

	private final int mDamage;
	private final int mUndeadDamage;

	public HolyJavelin(Plugin plugin, Player player) {
		super(plugin, player, "Holy Javelin");
		mInfo.mLinkedSpell = Spells.HOLY_JAVELIN;
		mInfo.mScoreboardId = "HolyJavelin";
		mInfo.mShorthandName = "HJ";
		mInfo.mDescriptions.add("Sprint left-clicking while not holding a pickaxe throws a piercing spear of light 12 blocks dealing 12 damage to undead and 5 damage to all others. All hit enemies are set on fire for 5s. Cooldown: 7s.");
		mInfo.mDescriptions.add("Damage is increased to 24 to undead and 10 to all others.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
		mUndeadDamage = getAbilityScore() == 1 ? UNDEAD_DAMAGE_1 : UNDEAD_DAMAGE_2;
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSprinting() && !mPlayer.isSneaking() && !InventoryUtils.isPickaxeItem(mainHand);
	}

	@Override
	public void cast(Action action) {
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
		world.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 0.9f);
		Location playerLoc = mPlayer.getEyeLocation();
		Location location = playerLoc.clone();
		Vector increment = location.getDirection();
		world.spawnParticle(Particle.EXPLOSION_NORMAL, location.clone().add(increment), 10, 0, 0, 0, 0.125f);

		// Get a list of all the mobs this could possibly hit (that are within range of the player)
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(location, RANGE, mPlayer);
		BoundingBox box = BoundingBox.of(playerLoc, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
		for (int i = 0; i < RANGE; i++) {
			box.shift(increment);
			Location loc = box.getCenter().toLocation(world);
			world.spawnParticle(Particle.REDSTONE, loc, 22, 0.25, 0.25, 0.25, COLOR);
			world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0f, 0f, 0f, 0.025f);

			Iterator<LivingEntity> iter = mobs.iterator();
			while (iter.hasNext()) {
				LivingEntity mob = iter.next();
				if (mob.getBoundingBox().overlaps(box)) {
					if (EntityUtils.isUndead(mob)) {
						EntityUtils.damageEntity(mPlugin, mob, mUndeadDamage, mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);
					} else {
						EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);
					}
					EntityUtils.applyFire(mPlugin, FIRE_DURATION, mob, mPlayer);
					iter.remove();
				}
			}

			if (loc.getBlock().getType().isSolid()) {
				loc.subtract(increment.multiply(0.5));
				world.spawnParticle(Particle.CLOUD, loc, 30, 0, 0, 0, 0.125f);
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
				world.playSound(loc, Sound.ENTITY_ARROW_HIT, 1, 0.9f);
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
