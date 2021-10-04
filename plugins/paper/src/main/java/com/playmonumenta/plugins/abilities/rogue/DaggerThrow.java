package com.playmonumenta.plugins.abilities.rogue;

import java.util.EnumSet;
import java.util.List;

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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class DaggerThrow extends Ability {
	public static class DaggerThrowCooldownEnchantment extends BaseAbilityEnchantment {
		public DaggerThrowCooldownEnchantment() {
			super("Dagger Throw Cooldown", EnumSet.of(ItemSlot.ARMOR));
		}
	}

	private static final String DAGGER_THROW_MOB_HIT_TICK = "HitByDaggerThrowTick";
	private static final int DAGGER_THROW_COOLDOWN = 12 * 20;
	private static final int DAGGER_THROW_RANGE = 8;
	private static final int DAGGER_THROW_1_DAMAGE = 6;
	private static final int DAGGER_THROW_2_DAMAGE = 12;
	private static final int DAGGER_THROW_DURATION = 10 * 20;
	private static final int DAGGER_THROW_1_VULN = 3;
	private static final int DAGGER_THROW_2_VULN = 7;
	private static final double DAGGER_THROW_SPREAD = Math.toRadians(25);
	private static final Particle.DustOptions DAGGER_THROW_COLOR = new Particle.DustOptions(Color.fromRGB(64, 64, 64), 1);

	private final int mDamage;
	private final int mVulnAmplifier;

	public DaggerThrow(Plugin plugin, Player player) {
		super(plugin, player, "Dagger Throw");
		mInfo.mLinkedSpell = ClassAbility.DAGGER_THROW;
		mInfo.mScoreboardId = "DaggerThrow";
		mInfo.mShorthandName = "DT";
		mInfo.mDescriptions.add("Sneak left click while holding two swords to throw three daggers which deal 6 damage and gives each target 20% Vulnerability for 10 seconds. Cooldown: 12s.");
		mInfo.mDescriptions.add("The damage is increased to 12 and the Vulnerability increased to 40%.");
		mInfo.mCooldown = DAGGER_THROW_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDisplayItem = new ItemStack(Material.WOODEN_SWORD, 1);
		mDamage = getAbilityScore() == 1 ? DAGGER_THROW_1_DAMAGE : DAGGER_THROW_2_DAMAGE;
		mVulnAmplifier = getAbilityScore() == 1 ? DAGGER_THROW_1_VULN : DAGGER_THROW_2_VULN;
	}

	@Override
	public void cast(Action action) {
		Location loc = mPlayer.getEyeLocation();
		Vector dir = loc.getDirection();
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, DAGGER_THROW_RANGE + 1, mPlayer);
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.25f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.0f);

		for (int a = -1; a <= 1; a++) {
			double angle = a * DAGGER_THROW_SPREAD;
			Vector newDir = new Vector(FastUtils.cos(angle) * dir.getX() + FastUtils.sin(angle) * dir.getZ(), dir.getY(), FastUtils.cos(angle) * dir.getZ() - FastUtils.sin(angle) * dir.getX());
			newDir.normalize();

			// Since we want some hitbox allowance, we use bounding boxes instead of a raycast
			BoundingBox box = BoundingBox.of(loc, 0.55, 0.55, 0.55);

			for (int i = 0; i <= DAGGER_THROW_RANGE; i++) {
				box.shift(newDir);
				Location bLoc = box.getCenter().toLocation(world);
				Location pLoc = bLoc.clone();
				for (int t = 0; t < 10; t++) {
					pLoc.add((newDir.clone()).multiply(0.1));
					world.spawnParticle(Particle.REDSTONE, pLoc, 1, 0.1, 0.1, 0.1, DAGGER_THROW_COLOR);
				}

				for (LivingEntity mob : mobs) {
					if (mob.getBoundingBox().overlaps(box)
						&& MetadataUtils.checkOnceThisTick(mPlugin, mob, DAGGER_THROW_MOB_HIT_TICK)) {
						bLoc.subtract((newDir.clone()).multiply(0.5));
						world.spawnParticle(Particle.SWEEP_ATTACK, bLoc, 3, 0.3, 0.3, 0.3, 0.1);
						world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.4f, 2.5f);

						EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.PHYSICAL, true, mInfo.mLinkedSpell);
						PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.UNLUCK, DAGGER_THROW_DURATION, mVulnAmplifier, true, false));
						break;
					} else if (bLoc.getBlock().getType().isSolid()) {
						bLoc.subtract((newDir.clone()).multiply(0.5));
						world.spawnParticle(Particle.SWEEP_ATTACK, bLoc, 3, 0.3, 0.3, 0.3, 0.1);
						break;
					}
				}
			}
		}
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		if (mPlayer.isSneaking() && mPlayer.getLocation().getPitch() > -50) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
			return InventoryUtils.rogueTriggerCheck(mainHand, offHand);
		}
		return false;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
			cast(Action.LEFT_CLICK_AIR);
		}

		return true;
	}

	@Override
	public Class<? extends BaseAbilityEnchantment> getCooldownEnchantment() {
		return DaggerThrowCooldownEnchantment.class;
	}
}
