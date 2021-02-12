package com.playmonumenta.plugins.abilities.mage;

import java.util.ArrayList;
import java.util.List;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.SpellDamage;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;



public class ThunderStep extends Ability {
	private static final Particle.DustOptions COLORYELLOW = new Particle.DustOptions(Color.YELLOW, 0.75f);
	private static final Particle.DustOptions COLORAQUA = new Particle.DustOptions(Color.AQUA, 0.75f);

	/*
	 * Cloud's standardised constant order:
	 * damage/extra damage/bonus damage, size/distance, amplifiers/multipliers, durations, other skill technicalities eg knockback, cooldowns
	 *
	 * For pairs of values where one is used to calculate the other, like seconds or hearts, the resulting value goes second
	 */
	private static final int DAMAGE_1 = 5;
	private static final int DAMAGE_2 = 10;
	private static final int SIZE = 3;
	private static final int DISTANCE = 8;
	private static final double CHECKINCREMENT = 0.1;
	private static final int COOLDOWNSECONDS = 24;
	private static final int COOLDOWN = COOLDOWNSECONDS * 20;

	private final int mLevelDamage;

	public ThunderStep(Plugin plugin, Player player) {
		super(plugin, player, "Thunder Step");

		mInfo.mLinkedSpell = Spells.THUNDER_STEP;
		//TODO with the next balance change, update these two old Channeling values and tell players to reselect skills like what's happened in the past?
		mInfo.mScoreboardId = "Channeling";
		mInfo.mShorthandName = "Ch";
		mInfo.mDescriptions.add(
			String.format(
				"While sneaking in mid-air, right-clicking with a wand materialises a flash of thunder, dealing %s damage to all enemies in a 3-block cube around you. The next moment, you are teleported towards where you're looking, travelling up to %s blocks or until you hit a solid block, and repeating your thunder attack at your destination. Cooldown: %ss.",
				DAMAGE_1,
				SIZE,
				COOLDOWNSECONDS
			)
		);
		mInfo.mDescriptions.add(
			String.format(
				"Damage is increased from %s to %s.",
				DAMAGE_1,
				DAMAGE_2
			)
		);
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mCooldown = COOLDOWN;

		mLevelDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
	}

	@Override
	public void cast(Action action) {
		putOnCooldown();

		Location playerLocationStart = mPlayer.getLocation();
		BoundingBox potentialPlayerBox = mPlayer.getBoundingBox();
		Location potentialPlayerLocation = playerLocationStart.clone();
		Vector vectorIncrement = playerLocationStart.getDirection().normalize().multiply(CHECKINCREMENT);

		doDamage(
			SpellDamage.getSpellDamage(mPlayer, mLevelDamage),
			playerLocationStart
		);

		loopMaxGoodDistance:
		for (int i = 0; i < DISTANCE / CHECKINCREMENT * 1.1; i++) {
			potentialPlayerLocation.add(vectorIncrement);
			if (playerLocationStart.distanceSquared(potentialPlayerLocation) > DISTANCE * DISTANCE) {
				// Gone too far, stop processing
				break;
			}

			potentialPlayerBox.shift(vectorIncrement);
			ArrayList<Location> locationsTouching = LocationUtils.getLocationsTouching(potentialPlayerBox, mPlayer.getWorld());
			for (Location location : locationsTouching) {
				Block block = location.getBlock();
				BoundingBox blockBoxEstimate = block.getBoundingBox();
				Material blockMaterial = block.getType();
				if (blockBoxEstimate.overlaps(potentialPlayerBox)) { // Seems liquids have empty bounding boxes similar to air, so they won't count as overlapping
					if (blockMaterial.isSolid()) {
						// The player's potential bounding box now hits a block after shifting,
						// the teleport can go no further; exhausted eligible teleport locations
						break loopMaxGoodDistance;
					}
				}
			}
		}
		potentialPlayerLocation.subtract(vectorIncrement); // Undo the last increment, it was either too far or hit a solid block

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mPlayer == null || mPlayer.isDead() || !mPlayer.isValid()) {
					this.cancel();
				}

				mPlayer.teleport(potentialPlayerLocation);
				doDamage(
					SpellDamage.getSpellDamage(mPlayer, mLevelDamage), // Recalculate damage for this second AoE, player spell power may have changed since first AoE
					potentialPlayerLocation
				);
			}
		}.runTaskLater(Plugin.getInstance(), 1);
	}

	private void doDamage(float spellDamage, Location loc) {
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1f, 1.5f);
		world.spawnParticle(Particle.REDSTONE, loc, 100, 2.5, 2.5, 2.5, 3, COLORYELLOW);
		world.spawnParticle(Particle.REDSTONE, loc, 100, 2.5, 2.5, 2.5, 3, COLORAQUA);
		world.spawnParticle(Particle.FLASH, loc.clone().add(loc.getDirection()), 1, 0, 0, 0, 10);

		List<LivingEntity> enemies = EntityUtils.getNearbyMobs(loc, SIZE, mPlayer); // Does not return the getter (player)
		// The more enemies, the less particles for each one
		int mobParticles = Math.max(
			1,
			20 / Math.max(1, enemies.size()) // Never divide by 0. Always maximum 20 particles for <= 1 enemy
		);

		for (LivingEntity enemy : enemies) {
			EntityUtils.damageEntity(mPlugin, enemy, spellDamage, mPlayer, MagicType.ARCANE, true, mInfo.mLinkedSpell); // Can both apply & trigger Spellshock by default

			world.spawnParticle(Particle.CLOUD, enemy.getLocation().add(0, 1, 0), mobParticles, 0.5, 0.5, 0.5, 0.5);
			world.spawnParticle(Particle.END_ROD, enemy.getLocation().add(0, 1, 0), mobParticles, 0.5, 0.5, 0.5, 0.5);
		}
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHandItem = mPlayer.getInventory().getItemInMainHand();
		return (
			InventoryUtils.isWandItem(mainHandItem)
			&& mPlayer.isSneaking()
			&& PlayerUtils.notOnGround(mPlayer)
			&& !ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)
		);
	}
}
