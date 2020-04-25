package com.playmonumenta.plugins.abilities.mage;

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
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.mage.elementalist.Starfall;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class ManaLance extends Ability {

	private static final int MANA_LANCE_1_DAMAGE = 8;
	private static final int MANA_LANCE_2_DAMAGE = 10;
	private static final int MANA_LANCE_1_COOLDOWN = 5 * 20;
	private static final int MANA_LANCE_2_COOLDOWN = 3 * 20;
	private static final Particle.DustOptions MANA_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(91, 187, 255), 1.0f);

	public ManaLance(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Mana Lance");
		mInfo.linkedSpell = Spells.MANA_LANCE;
		mInfo.scoreboardId = "ManaLance";
		mInfo.mShorthandName = "ML";
		mInfo.mDescriptions.add("Right clicking with a wand fires forth a piercing beam of Mana going 8 blocks, dealing 8 damage to enemies in the path of the beam. This beam will not go through solid blocks. 5 second cooldown.");
		mInfo.mDescriptions.add("The beam instead deals 10 damage with a 3 second cooldown.");
		// NOTE: getAbilityScore() can only be used after the scoreboardId is set!
		mInfo.cooldown = getAbilityScore() == 1 ? MANA_LANCE_1_COOLDOWN : MANA_LANCE_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action action) {
		int extraDamage = getAbilityScore() == 1 ? MANA_LANCE_1_DAMAGE : MANA_LANCE_2_DAMAGE;

		Location loc = mPlayer.getEyeLocation();
		BoundingBox box = BoundingBox.of(loc, 0.55, 0.55, 0.55);
		Vector dir = loc.getDirection();
		box.shift(dir);
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getLocation(), 10, mPlayer);
		mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0, 0, 0, 0.125);

		for (int i = 0; i < 8; i++) {
			box.shift(dir);
			Location bLoc = box.getCenter().toLocation(mWorld);

			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, bLoc, 2, 0.05, 0.05, 0.05, 0.025);
			mWorld.spawnParticle(Particle.REDSTONE, bLoc, 18, 0.35, 0.35, 0.35, MANA_LANCE_COLOR);

			if (bLoc.getBlock().getType().isSolid()) {
				bLoc.subtract(dir.multiply(0.5));
				mWorld.spawnParticle(Particle.CLOUD, bLoc, 30, 0, 0, 0, 0.125);
				mWorld.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
				break;
			}
			Iterator<LivingEntity> iter = mobs.iterator();
			while (iter.hasNext()) {
				LivingEntity mob = iter.next();
				if (box.overlaps(mob.getBoundingBox())) {
					EntityUtils.damageEntity(mPlugin, mob, extraDamage, mPlayer, MagicType.ARCANE, true, mInfo.linkedSpell);
					MovementUtils.knockAway(mPlayer.getLocation(), mob, 0.25f, 0.25f);
					iter.remove();
					mobs.remove(mob);
				}
			}
		}

		putOnCooldown();
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
	}

	@Override
	public boolean runCheck() {
		// Must not have triggered Starfall
		Starfall starfall = AbilityManager.getManager().getPlayerAbility(mPlayer, Starfall.class);
		if (starfall != null && starfall.shouldCancelManaLance()) {
			return false;
		}

		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return !mPlayer.isSneaking() && InventoryUtils.isWandItem(mainHand);
	}

}
