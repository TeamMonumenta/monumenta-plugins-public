package com.playmonumenta.plugins.abilities.mage;

import java.util.EnumSet;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.mage.arcanist.ArcaneBarrage;
import com.playmonumenta.plugins.abilities.mage.elementalist.Starfall;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.BaseAbilityEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.SpellDamage;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class ManaLance extends Ability {
	public static class ManaLanceDamageEnchantment extends BaseAbilityEnchantment {
		public ManaLanceDamageEnchantment() {
			super("Mana Lance Damage", EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR));
		}
	}

	public static class ManaLanceCooldownEnchantment extends BaseAbilityEnchantment {
		public ManaLanceCooldownEnchantment() {
			super("Mana Lance Cooldown", EnumSet.of(ItemSlot.OFFHAND));
		}
	}

	private static final float DAMAGE_1 = 6.0f;
	private static final float DAMAGE_2 = 7.0f;
	private static final int COOLDOWN_1 = 5 * 20;
	private static final int COOLDOWN_2 = 3 * 20;
	private static final Particle.DustOptions MANA_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(91, 187, 255), 1.0f);

	public ManaLance(Plugin plugin, Player player) {
		super(plugin, player, "Mana Lance");
		mInfo.mLinkedSpell = Spells.MANA_LANCE;
		mInfo.mScoreboardId = "ManaLance";
		mInfo.mShorthandName = "ML";
		mInfo.mDescriptions.add("Right clicking with a wand fires forth a piercing beam of Mana going 8 blocks, dealing 6 damage to enemies in the path of the beam. This beam will not go through solid blocks. Cooldown: 5s.");
		mInfo.mDescriptions.add("The beam instead deals 7 damage. Cooldown: 3s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? COOLDOWN_1 : COOLDOWN_2;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action action) {
		//Ability enchantments
		float damage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
		damage += ManaLanceDamageEnchantment.getExtraDamage(mPlayer, ManaLanceDamageEnchantment.class);
		damage = SpellDamage.getSpellDamage(mPlayer, damage);
		float cd = getAbilityScore() == 1 ? COOLDOWN_1 : COOLDOWN_2;
		mInfo.mCooldown = (int) ManaLanceCooldownEnchantment.getCooldown(mPlayer, cd, ManaLanceCooldownEnchantment.class);

		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		BoundingBox box = BoundingBox.of(loc, 0.55, 0.55, 0.55);
		Vector dir = loc.getDirection();
		box.shift(dir);
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getLocation(), 10, mPlayer);
		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0, 0, 0, 0.125);

		for (int i = 0; i < 8; i++) {
			box.shift(dir);
			Location bLoc = box.getCenter().toLocation(world);

			world.spawnParticle(Particle.EXPLOSION_NORMAL, bLoc, 2, 0.05, 0.05, 0.05, 0.025);
			world.spawnParticle(Particle.REDSTONE, bLoc, 18, 0.35, 0.35, 0.35, MANA_LANCE_COLOR);

			if (bLoc.getBlock().getType().isSolid()) {
				bLoc.subtract(dir.multiply(0.5));
				world.spawnParticle(Particle.CLOUD, bLoc, 30, 0, 0, 0, 0.125);
				world.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
				break;
			}
			Iterator<LivingEntity> iter = mobs.iterator();
			while (iter.hasNext()) {
				LivingEntity mob = iter.next();
				if (box.overlaps(mob.getBoundingBox())) {
					EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer, MagicType.ARCANE, true, mInfo.mLinkedSpell);
					MovementUtils.knockAway(mPlayer.getLocation(), mob, 0.25f, 0.25f);
					iter.remove();
					mobs.remove(mob);
				}
			}
		}

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
	}

	@Override
	public boolean runCheck() {
		// Must not have triggered Starfall or Arcane Barrage
		Starfall starfall = AbilityManager.getManager().getPlayerAbility(mPlayer, Starfall.class);
		ArcaneBarrage barrage = AbilityManager.getManager().getPlayerAbility(mPlayer, ArcaneBarrage.class);
		if (starfall != null && starfall.shouldCancelManaLance() || barrage != null && barrage.shouldCancelManaLance()) {
			return false;
		}

		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return !mPlayer.isSneaking() && InventoryUtils.isWandItem(mainHand);
	}

	public float getDamage() {
		//Just in case the damage changes in the future
		return getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
	}
}
