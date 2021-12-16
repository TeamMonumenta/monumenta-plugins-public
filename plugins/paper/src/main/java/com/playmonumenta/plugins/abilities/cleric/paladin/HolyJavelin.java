package com.playmonumenta.plugins.abilities.cleric.paladin;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;



public class HolyJavelin extends Ability {
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 50), 1.0f);

	private static final double HITBOX_LENGTH = 0.75;
	private static final int RANGE = 12;
	private static final int UNDEAD_DAMAGE_1 = 18;
	private static final int UNDEAD_DAMAGE_2 = 24;
	private static final int DAMAGE_1 = 9;
	private static final int DAMAGE_2 = 12;
	private static final int FIRE_DURATION = 5 * 20;
	private static final int COOLDOWN = 12 * 20;

	private final int mDamage;
	private final int mUndeadDamage;

	private @Nullable Crusade mCrusade;
	private @Nullable DivineJustice mDivineJustice;
	private @Nullable LuminousInfusion mLuminousInfusion;

	public HolyJavelin(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Holy Javelin");
		mInfo.mLinkedSpell = ClassAbility.HOLY_JAVELIN;
		mInfo.mScoreboardId = "HolyJavelin";
		mInfo.mShorthandName = "HJ";
		mInfo.mDescriptions.add("While sprinting, left-clicking with a non-pickaxe throws a piercing spear of light, instantly travelling up to 12 blocks or until it hits a solid block. It deals 18 holy damage to all enemies in a 0.75-block cube around it along its path, or 9 damage to non-undead, and sets them all on fire for 5s. Cooldown: 12s.");
		mInfo.mDescriptions.add("Attacking an undead enemy with that left-click now transmits any passive Divine Justice and Luminous Infusion damage to other enemies pierced by the spear. Damage is increased from 18 to 24, and from 9 to 18 against non-undead.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDisplayItem = new ItemStack(Material.TRIDENT, 1);
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
		mUndeadDamage = getAbilityScore() == 1 ? UNDEAD_DAMAGE_1 : UNDEAD_DAMAGE_2;

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mCrusade = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Crusade.class);
				mDivineJustice = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, DivineJustice.class);
				mLuminousInfusion = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, LuminousInfusion.class);
			});
		}
	}

	@Override
	public boolean runCheck() {
		if (mPlayer == null) {
			return false;
		}
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSprinting() && !mPlayer.isSneaking() && !ItemUtils.isPickaxe(mainHand);
	}

	@Override
	public void cast(Action action) {
		execute(0, null);
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		//TODO pass in casted entities for events like these
		LivingEntity enemy = (LivingEntity)event.getEntity();

		if (DamageCause.ENTITY_ATTACK.equals(event.getCause())) {
			double sharedPassiveDamage = 0;
			if (mLuminousInfusion != null) {
				sharedPassiveDamage += mLuminousInfusion.mLastPassiveMeleeDamage;
				sharedPassiveDamage += mLuminousInfusion.mLastPassiveDJDamage;
				mLuminousInfusion.mLastPassiveMeleeDamage = 0;
				mLuminousInfusion.mLastPassiveDJDamage = 0;
			}
			if (mDivineJustice != null) {
				sharedPassiveDamage += mDivineJustice.mLastPassiveDamage;
				mDivineJustice.mLastPassiveDamage = 0;
			}
			execute(sharedPassiveDamage, enemy);
		}

		return true;
	}

	public void execute(
		double bonusDamage,
		@Nullable LivingEntity triggeringEnemy
	) {
		if (mPlayer == null) {
			return;
		}
		putOnCooldown();

		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
		world.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 0.9f);
		Location playerLoc = mPlayer.getEyeLocation();
		Location location = playerLoc.clone();
		Vector increment = location.getDirection();
		world.spawnParticle(Particle.EXPLOSION_NORMAL, location.clone().add(increment), 10, 0, 0, 0, 0.125f);

		// Get a list of all the mobs this could possibly hit (that are within range of the player)
		List<LivingEntity> potentialTargets = EntityUtils.getNearbyMobs(location, RANGE + HITBOX_LENGTH, mPlayer);
		BoundingBox box = BoundingBox.of(playerLoc, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
		for (double i = 0; i < RANGE; i += HITBOX_LENGTH) {
			box.shift(increment);
			Location loc = box.getCenter().toLocation(world);
			world.spawnParticle(Particle.REDSTONE, loc, 22, 0.25, 0.25, 0.25, COLOR);
			world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0f, 0f, 0f, 0.025f);

			Iterator<LivingEntity> iterator = potentialTargets.iterator();
			while (iterator.hasNext()) {
				LivingEntity enemy = iterator.next();
				if (enemy.getBoundingBox().overlaps(box)) {
					double damage = (
						Crusade.enemyTriggersAbilities(enemy, mCrusade)
							? mUndeadDamage
							: mDamage
					);
					if (enemy != triggeringEnemy) {
						// Triggering enemy would've already received the melee damage from Luminous
						// Infusion
						damage += bonusDamage;
					}
					EntityUtils.applyFire(mPlugin, FIRE_DURATION, enemy, mPlayer);
					EntityUtils.damageEntity(mPlugin, enemy, damage, mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);
					iterator.remove();
				}
			}

			if (!loc.isChunkLoaded() || loc.getBlock().getType().isSolid()) {
				loc.subtract(increment.multiply(0.5));
				world.spawnParticle(Particle.CLOUD, loc, 30, 0, 0, 0, 0.125f);
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
				world.playSound(loc, Sound.ENTITY_ARROW_HIT, 1, 0.9f);
				break;
			}
		}
	}
}
