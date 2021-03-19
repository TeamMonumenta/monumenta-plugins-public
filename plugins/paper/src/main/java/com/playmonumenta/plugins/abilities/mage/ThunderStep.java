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
import org.bukkit.event.player.PlayerSwapHandItemsEvent;


public class ThunderStep extends Ability {
	public static final String NAME = "Thunder Step";
	public static final Particle.DustOptions COLOR_YELLOW = new Particle.DustOptions(Color.YELLOW, 0.75f);
	public static final Particle.DustOptions COLOR_AQUA = new Particle.DustOptions(Color.AQUA, 0.75f);

	/*
	 * Cloud's standardised constant order:
	 * damage/extra damage/bonus damage, size/distance, amplifiers/multipliers, durations, other skill technicalities eg knockback, cooldowns
	 *
	 * For pairs of values where one is used to calculate the other, like seconds or hearts, the resulting value goes second
	 */
	public static final int DAMAGE_1 = 5;
	public static final int DAMAGE_2 = 8;
	public static final int SIZE = 4;
	public static final int DISTANCE_1 = 8;
	public static final int DISTANCE_2 = 10;
	public static final double CHECK_INCREMENT = 0.1;
	public static final int COOLDOWN_SECONDS = 22;
	public static final int COOLDOWN = COOLDOWN_SECONDS * 20;
	public static final int STUN_SECONDS = 1;
	public static final int STUN_DURATION = STUN_SECONDS * 20;

	private final int mLevelDamage;
	private final int mDistance;

	public ThunderStep(Plugin plugin, Player player) {
		super(plugin, player, NAME);

		mInfo.mLinkedSpell = Spells.THUNDER_STEP;
		mInfo.mScoreboardId = "ThunderStep";
		mInfo.mShorthandName = "TS";
		mInfo.mDescriptions.add(
			String.format(
				"Pressing the swap key while sneaking with a wand in your hand materializes a flash of thunder, dealing %s damage to all enemies in a %s-block cube around you and knocking them away. The next moment, you are teleported towards where you're looking, travelling up to %s blocks or until you hit a solid block, and repeating your thunder attack at your destination. Cooldown: %ss.",
				DAMAGE_1,
				SIZE,
				DISTANCE_1,
				COOLDOWN_SECONDS
			)
		);
		mInfo.mDescriptions.add(
			String.format(
				"Damage is increased to %s and teleport distance is increased to %s blocks. Additionally, stun all mobs within the damage radius for %s second.",
				DAMAGE_2,
				DISTANCE_2,
				STUN_SECONDS
			)
		);
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;

		mLevelDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
		mDistance = getAbilityScore() == 1 ? DISTANCE_1 : DISTANCE_2;
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
			if (getAbilityScore() > 1) {
				EntityUtils.applyStun(mPlugin, STUN_DURATION, enemy);
			}
			world.spawnParticle(Particle.CLOUD, enemy.getLocation().add(0, 1, 0), mobParticles, 0.5, 0.5, 0.5, 0.5);
			world.spawnParticle(Particle.END_ROD, enemy.getLocation().add(0, 1, 0), mobParticles, 0.5, 0.5, 0.5, 0.5);
		}
	}
	
	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		ItemStack mainHandItem = mPlayer.getInventory().getItemInMainHand();
		if (InventoryUtils.isWandItem(mainHandItem) && mPlayer.isSneaking() && !ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			event.setCancelled(true);
			if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
				return;
			}
			putOnCooldown();

			Location playerStartLocation = mPlayer.getLocation();
			doDamage(
				SpellDamage.getSpellDamage(mPlayer, mLevelDamage),
				playerStartLocation
			);

			World world = mPlayer.getWorld();
			BoundingBox movingPlayerBox = mPlayer.getBoundingBox();
			Vector vector = playerStartLocation.getDirection();
			vector.setY(Math.max(vector.getY(), 0));
			LocationUtils.travelTillObstructed(
				world,
				movingPlayerBox,
				mDistance,
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
	}
}
