package com.playmonumenta.plugins.abilities.mage;

import java.util.Collection;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

/*
 * Channeling: After casting a spell, your next melee hit deals 3 / 6 extra damage.
 * Depending on the spell type cast (fire, ice, arcane), your attack will also
 * set the hit enemy on fire, apply slowness II, or apply weakness I for 4 seconds.
 */

public class ThunderStep extends Ability {

	private static final int THUNDER_STEP_1_DAMAGE = 5;
	private static final int THUNDER_STEP_2_DAMAGE = 10;

	private static final Particle.DustOptions THUNDER_STEP_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 0), 0.75f);
	private static final Particle.DustOptions THUNDER_STEP_COLOR2 = new Particle.DustOptions(Color.fromRGB(214, 247, 5), 0.75f);

	private final int mDamage;

	public ThunderStep(Plugin plugin, Player player) {
		super(plugin, player, "Thunder Step");
		mInfo.mLinkedSpell = Spells.THUNDER_STEP;
		mInfo.mScoreboardId = "Channeling";
		mInfo.mShorthandName = "Ch";
		mInfo.mDescriptions.add("Right-click while shifted and airborne to deal 5 damage to all mobs in a 3 block radius. After activating, you are teleported up to 8 blocks in the direction you are facing, dealing the AoE damage again upon landing. Cooldown: 24s.");
		mInfo.mDescriptions.add("Damage increased to 10.");
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mCooldown = 20 * 24;
		mDamage = getAbilityScore() == 1 ? THUNDER_STEP_1_DAMAGE : THUNDER_STEP_2_DAMAGE;
	}

	@Override
	public void cast(Action action) {
		putOnCooldown();
		Location startLoc = mPlayer.getLocation();
		Location endLoc = startLoc.clone();
		Location endLoc1 = startLoc.clone().add(0, 1, 0);

		Vector baseVect = startLoc.getDirection().normalize().multiply(0.1);
		BoundingBox box = mPlayer.getBoundingBox();

		boolean cancel = false;

		doDamage(startLoc);

		for (int i = 0; i < 200; i++) {
			box.shift(baseVect);
			endLoc.add(baseVect);
			endLoc1.add(baseVect);

			// Check if the bounding box overlaps with any of the surrounding blocks
			for (int x = -1; x <= 1 && !cancel; x++) {
				for (int y = -1; y <= 1 && !cancel; y++) {
					for (int z = -1; z <= 1 && !cancel; z++) {
						Block block = endLoc.clone().add(x, y, z).getBlock();
						// If it overlaps with any, move it back to the last safe location
						// and terminate the charge before the block.
						if (block.getBoundingBox().overlaps(box) && (!block.isLiquid() || (block.getType().isOccluding() && !ItemUtils.GOOD_OCCLUDERS.contains(block.getType())))) {
							endLoc.subtract(baseVect);
							cancel = true;
						}
					}
				}
			}
			if (!cancel && (endLoc.getBlock().getType().isSolid() || endLoc1.getBlock().getType().isSolid())) {
				// No longer air - need to go back a bit so we don't tele the boss into a block
				endLoc.subtract(baseVect.multiply(1));
				// Charge terminated at a block
				break;
			} else if (startLoc.distance(endLoc) > 8.0f) {
				// Reached end of charge without hitting anything
				break;
			}

			if (cancel) {
				break;
			}
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mPlayer == null || mPlayer.isDead() || !mPlayer.isValid()) {
					this.cancel();
				}
				mPlayer.teleport(endLoc);
				doDamage(endLoc);
			}
		}.runTaskLater(Plugin.getInstance(), 1);
	}

	public void doDamage(Location loc) {
		final Location mLoc = mPlayer.getLocation();
		World mWorld = mLoc.getWorld();
		mWorld.playSound(mLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1f, 1.5f);
		mWorld.spawnParticle(Particle.REDSTONE, mLoc, 100, 2.5, 2.5, 2.5, 3, THUNDER_STEP_COLOR);
		mWorld.spawnParticle(Particle.REDSTONE, mLoc, 100, 2.5, 2.5, 2.5, 3, THUNDER_STEP_COLOR2);

		mWorld.spawnParticle(Particle.FLASH, mLoc.clone().add(mLoc.getDirection()), 1, 0, 0, 0, 10);

		Collection<LivingEntity> mobs = loc.getNearbyLivingEntities(3);
		int mobParticles = 20;
		if (mobs.size() > 0) {
			mobParticles = Math.max(1, 20 / mobs.size());
		}

		for (LivingEntity mob : mobs) {
			if (mob.getType() != EntityType.PLAYER) {
				mob.setNoDamageTicks(0);
				EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.ARCANE, true, mInfo.mLinkedSpell, true, true);

				mWorld.spawnParticle(Particle.CLOUD, mob.getLocation().add(0, 1, 0), mobParticles, 0.5, 0.5, 0.5, 0.5);
				mWorld.spawnParticle(Particle.END_ROD, mob.getLocation().add(0, 1, 0), mobParticles, 0.5, 0.5, 0.5, 0.5);
			}
		}
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		Material blockType = mPlayer.getLocation().getBlock().getType();
		return (InventoryUtils.isWandItem(mainHand) && mPlayer.isSneaking() && !mPlayer.isOnGround() && blockType != Material.LADDER && blockType != Material.VINE
				&& !ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES));
	}

}
