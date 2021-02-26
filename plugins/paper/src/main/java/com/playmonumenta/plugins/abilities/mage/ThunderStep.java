package com.playmonumenta.plugins.abilities.mage;

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
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;



public class ThunderStep extends Ability {
	private static final Particle.DustOptions COLOR_YELLOW = new Particle.DustOptions(Color.YELLOW, 0.75f);
	private static final Particle.DustOptions COLOR_AQUA = new Particle.DustOptions(Color.AQUA, 0.75f);

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
	private static final double CHECK_INCREMENT = 0.1;
	private static final int COOLDOWN_SECONDS = 24;
	private static final int COOLDOWN = COOLDOWN_SECONDS * 20;

	private final int mLevelDamage;

	public ThunderStep(Plugin plugin, Player player) {
		super(plugin, player, "Thunder Step");

		mInfo.mLinkedSpell = Spells.THUNDER_STEP;
		//TODO with the next balance change, update these two old Channeling values and tell players to reselect skills like what's happened in the past?
		// ScriptedQuests stuff for class selection & the Spec NPC will also need updating, possibly the class selection command blocks as well
		mInfo.mScoreboardId = "Channeling";
		mInfo.mShorthandName = "Ch";
		mInfo.mDescriptions.add(
			String.format(
				"While sneaking in mid-air, right-clicking with a wand materializes a flash of thunder, dealing %s damage to all enemies in a %s-block cube around you and knocking them away. The next moment, you are teleported towards where you're looking, travelling up to %s blocks or until you hit a solid block, and repeating your thunder attack at your destination. Cooldown: %ss.",
				DAMAGE_1,
				SIZE,
				DISTANCE,
				COOLDOWN_SECONDS
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

		Location playerStartLocation = mPlayer.getLocation();
		doDamage(
			SpellDamage.getSpellDamage(mPlayer, mLevelDamage),
			playerStartLocation
		);

		World world = mPlayer.getWorld();
		BoundingBox movingPlayerBox = mPlayer.getBoundingBox();
		Vector vector = playerStartLocation.getDirection();
		LocationUtils.travelTillObstructed(
			world,
			movingPlayerBox,
			DISTANCE,
			vector,
			CHECK_INCREMENT
		);
		Location playerEndLocation = movingPlayerBox
			.getCenter()
			.setY(
				movingPlayerBox.getMinY()
			)
			.toLocation(world)
			.setDirection(vector);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mPlayer == null || mPlayer.isDead() || !mPlayer.isValid()) {
					this.cancel();
				}

				mPlayer.teleport(playerEndLocation);
				doDamage(
					SpellDamage.getSpellDamage(mPlayer, mLevelDamage), // Recalculate damage for this second AoE, player spell power may have changed since first AoE
					playerEndLocation
				);
			}
		}.runTaskLater(Plugin.getInstance(), 1);
	}

	private void doDamage(float spellDamage, Location loc) {
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1f, 1.5f);
		world.spawnParticle(Particle.REDSTONE, loc, 100, 2.5, 2.5, 2.5, 3, COLOR_YELLOW);
		world.spawnParticle(Particle.REDSTONE, loc, 100, 2.5, 2.5, 2.5, 3, COLOR_AQUA);
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
