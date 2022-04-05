package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class UnstableAmalgam extends Ability {

	private static final int UNSTABLE_AMALGAM_1_COOLDOWN = 20 * 20;
	private static final int UNSTABLE_AMALGAM_2_COOLDOWN = 16 * 20;
	private static final int UNSTABLE_AMALGAM_1_DAMAGE = 12;
	private static final int UNSTABLE_AMALGAM_2_DAMAGE = 20;
	private static final int UNSTABLE_AMALGAM_CAST_RANGE = 7;
	private static final int UNSTABLE_AMALGAM_DURATION = 3 * 20;
	private static final int UNSTABLE_AMALGAM_RADIUS = 4;
	private static final float UNSTABLE_AMALGAM_KNOCKBACK_SPEED = 2.5f;

	private @Nullable AlchemistPotions mAlchemistPotions;
	private @Nullable Slime mAmalgam;
	private int mDamage;

	public UnstableAmalgam(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Unstable Amalgam");
		mInfo.mLinkedSpell = ClassAbility.UNSTABLE_AMALGAM;
		mInfo.mScoreboardId = "UnstableAmalgam";
		mInfo.mShorthandName = "UA";
		mInfo.mDescriptions.add("Shift left click while holding an Alchemist's Bag to consume a potion to place an Amalgam with 1 health at the location you are looking, up to 7 blocks away. When the Amalgam dies, or after 3 seconds, it explodes, dealing your Alchemist Potion's damage + 12 magic damage to mobs in a 4 block radius and applying potion effects from all abilities. Mobs and players in the radius are knocked away from the Amalgam. For each mob damaged, gain an Alchemist's Potion. Cooldown: 20s.");
		mInfo.mDescriptions.add("The damage is increased to 20 and the cooldown is reduced to 16s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? UNSTABLE_AMALGAM_1_COOLDOWN : UNSTABLE_AMALGAM_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDisplayItem = new ItemStack(Material.GUNPOWDER, 1);

		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			mAlchemistPotions = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
		mAmalgam = null;
		mDamage = getAbilityScore() == 1 ? UNSTABLE_AMALGAM_1_DAMAGE : UNSTABLE_AMALGAM_2_DAMAGE;
	}

	@Override
	public void cast(Action action) {
		if (mPlayer != null && mAlchemistPotions != null && mPlayer.isSneaking() && ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand()) && mAlchemistPotions.decrementCharge()) {
			putOnCooldown();

			Location loc = mPlayer.getEyeLocation();
			Vector dir = loc.getDirection().normalize();
			for (double i = 0; i < UNSTABLE_AMALGAM_CAST_RANGE; i += 0.5) {
				Location prevLoc = loc;
				loc.add(dir);

				if (loc.getBlock().getType().isSolid()) {
					spawnAmalgam(prevLoc);

					return;
				}
			}

			spawnAmalgam(loc);
		}
	}

	private void spawnAmalgam(Location loc) {
		if (mPlayer == null || mAlchemistPotions == null) {
			return;
		}

		loc.setY(loc.getBlockY() + 1);

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		World world = loc.getWorld();
		Entity e = LibraryOfSoulsIntegration.summon(loc, "UnstableAmalgam");
		if (e instanceof Slime amalgam) {
			mAmalgam = amalgam;

			new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					if (mAmalgam == null) {
						this.cancel();
						return;
					}

					if (mPlayer == null || !mPlayer.isOnline()) {
						mAmalgam.remove();
						mAmalgam = null;
						this.cancel();
						return;
					}

					if (mAmalgam.isDead() || mTicks >= UNSTABLE_AMALGAM_DURATION) {
						explode(mAmalgam.getLocation(), playerItemStats);
						mAmalgam.remove();
						mAmalgam = null;
						this.cancel();
						return;
					} else if (mTicks % 20 == 0) {
						new PartialParticle(Particle.FLAME, loc, 20, 0.02, 0.02, 0.02, 0.1).spawnAsPlayerActive(mPlayer);
						world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 1.7f);
					}

					mTicks++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	private void explode(Location loc, ItemStatManager.PlayerItemStats playerItemStats) {
		if (mPlayer == null || !mPlayer.isOnline() || mAlchemistPotions == null) {
			return;
		}

		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, UNSTABLE_AMALGAM_RADIUS, mAmalgam)) {
			DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.mLinkedSpell, playerItemStats), mDamage + mAlchemistPotions.getDamage(), true, true, false);

			mAlchemistPotions.applyEffects(mob, false);
			mAlchemistPotions.applyEffects(mob, true);

			MovementUtils.knockAwayRealistic(loc, mob, UNSTABLE_AMALGAM_KNOCKBACK_SPEED, 2f, true);
			mAlchemistPotions.incrementCharge();
		}

		if (!ZoneUtils.hasZoneProperty(loc, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			for (Player player : PlayerUtils.playersInRange(loc, UNSTABLE_AMALGAM_RADIUS, true)) {
				if (!ZoneUtils.hasZoneProperty(player, ZoneProperty.NO_MOBILITY_ABILITIES)) {
					if (!player.equals(mPlayer) && ScoreboardUtils.getScoreboardValue(player, "RocketJumper").orElse(0) == 100) {
						MovementUtils.knockAwayRealistic(loc, player, UNSTABLE_AMALGAM_KNOCKBACK_SPEED, 2f, false);
					} else if (player.equals(mPlayer) && ScoreboardUtils.getScoreboardValue(player, "RocketJumper").orElse(1) > 0) {
						//by default any Alch can use Rocket Jump with his UA
						MovementUtils.knockAwayRealistic(loc, player, UNSTABLE_AMALGAM_KNOCKBACK_SPEED, 2f, false);
					}
				}
			}
		}

		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.25f);
		new PartialParticle(Particle.FLAME, loc, 115, 0.02, 0.02, 0.02, 0.2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 40, 0.02, 0.02, 0.02, 0.35).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 40, 0.02, 0.02, 0.02, 0.35).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			cast(Action.LEFT_CLICK_AIR);
		}
		return false;
	}
}
