package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class EarthenWrath extends DepthsAbility {

	public static final String ABILITY_NAME = "Earthen Wrath";
	private static final double[] PERCENT_DAMAGE_REFLECTED = {0.8, 1.0, 1.2, 1.4, 1.6, 2.0};
	private static final double[] PERCENT_DAMAGE_REDUCTION = {0.35, 0.375, 0.4, 0.425, 0.45, 0.5};
	private static final int COOLDOWN = 20 * 30;
	private static final int DURATION = 6 * 20;
	private static final int DAMAGE_RADIUS = 6;

	public boolean mAbsorbDamage = false;
	private double mDamageAbsorbed;

	public EarthenWrath(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayMaterial = Material.TURTLE_HELMET;
		mTree = DepthsTree.EARTHBOUND;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.EARTHEN_WRATH;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (mPlayer == null) {
			return;
		}
		event.setCancelled(true);
		putOnCooldown();
		mDamageAbsorbed = 0;

		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 10, 1);

		DepthsParty party = DepthsManager.getInstance().getPartyFromId(DepthsManager.getInstance().mPlayers.get(mPlayer.getUniqueId()));
		if (party == null) {
			return;
		}

		new BukkitRunnable() {
			private int mTicks = 0;
			private float mPitch = 0.5f;

			@Override
			public void run() {
				startWrath();
				mTicks++;

				if (mTicks % 10 == 0) {
					for (DepthsPlayer dp : party.mPlayersInParty) {
						Player p = Bukkit.getPlayer(dp.mPlayerId);
						if (p != null && p.isOnline() && !dp.mPlayerId.equals(mPlayer.getUniqueId())) {
							Location loc = p.getLocation();

							Location tempLoc = loc.clone();
							for (int deg = 0; deg < 360; deg += 40) {
								if (FastUtils.RANDOM.nextDouble() > 0.2) {
									tempLoc.set(loc.getX(), loc.getY(), loc.getZ());
									world.spawnParticle(Particle.SPELL_INSTANT, tempLoc.add(FastUtils.cos(deg), 0.5, FastUtils.sin(deg)), 1, 0, 0, 0, 0);
									world.spawnParticle(Particle.SPELL_INSTANT, tempLoc.add(FastUtils.cos(deg), 1.5, FastUtils.sin(deg)), 1, 0, 0, 0, 0);
								}
							}
						}
					}

					world.spawnParticle(Particle.CRIT_MAGIC, mPlayer.getLocation().add(0, 0.5, 0), 30, 1, 0.5, 1, 0.25);
					world.spawnParticle(Particle.BLOCK_DUST, mPlayer.getLocation().add(0, 0.5, 0), 30, 1, 0.5, 1, 0.25, Material.COARSE_DIRT.createBlockData());

					world.playSound(mPlayer.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 10, mPitch);

					mPitch += 0.1f;
				}

				if (mTicks >= DURATION) {
					this.cancel();
					endWrath();

					Location loc = mPlayer.getLocation();

					if (mDamageAbsorbed > 0) {
						for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, DAMAGE_RADIUS)) {
							DamageUtils.damage(mPlayer, mob, DamageType.OTHER, mDamageAbsorbed * PERCENT_DAMAGE_REFLECTED[mRarity - 1], mInfo.mLinkedSpell);
						}

						world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
						world.spawnParticle(Particle.BLOCK_DUST, loc, 250, 3, 0.1, 3, 0.25, Material.COARSE_DIRT.createBlockData());
						world.spawnParticle(Particle.LAVA, loc, 100, 3, 0.1, 3, 0.25);
						world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 75, 3, 0.1, 3, 0.25);
					} else {
						world.playSound(loc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1f, 2f);
					}

				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	//Returns true if the damage was absorbed
	public boolean damagedEntity(Player otherPlayer, DamageEvent event) {
		DepthsPlayer dp = DepthsManager.getInstance().mPlayers.get(otherPlayer.getUniqueId());

		if (dp != null) {
			EarthenWrath otherWrath = AbilityManager.getManager().getPlayerAbility(otherPlayer, EarthenWrath.class);
			if (otherWrath != null && otherWrath.isWrathing()) {
				return false;
			}
		}

		if (isWrathing() && !otherPlayer.equals(mPlayer)) {
			mDamageAbsorbed += event.getFinalDamage(false);

			Vector velocity = mPlayer.getVelocity();

			// Create a new DamageEvent from the EntityDamageEvent with the same damage and damage type but a different damagee
			DamageUtils.damage(event.getSource(), mPlayer, event.getType(), event.getDamage(), null, false, false, ABILITY_NAME);

			mPlayer.setVelocity(velocity);

			World world = mPlayer.getWorld();
			Location wrathLoc = mPlayer.getLocation();
			Location otherLoc = otherPlayer.getLocation();

			world.playSound(wrathLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1, 2);
			world.playSound(otherLoc, Sound.ITEM_SHIELD_BLOCK, 1, 2);
			world.spawnParticle(Particle.TOTEM, otherLoc, 30, 0.1, 0.1, 0.1, 0.6);

			Location pLoc = otherLoc.clone().add(0, 0.5, 0);
			Vector dir = wrathLoc.toVector().subtract(otherLoc.toVector()).normalize();
			for (int i = 0; i <= wrathLoc.distance(otherLoc); i++) {
				pLoc.add(dir);

				world.spawnParticle(Particle.VILLAGER_HAPPY, pLoc, 3, 0.25, 0.25, 0.25, 0);
				world.spawnParticle(Particle.CLOUD, pLoc, 6, 0.05, 0.05, 0.05, 0.05);
			}

			event.setDamage(0);
			return true;
		}
		return false;
	}

	// Called in DepthsListener
	// Handles all incoming damage events, regardless of if anyone has this skill
	public static void handleDamageEvent(DamageEvent event, Player damagee, DepthsParty party) {
		if (event.isCancelled() || event.isBlocked()) {
			return;
		}

		//Creates a new party list with random order without modifying the normal order of the party
		List<DepthsPlayer> playersInParty = new ArrayList<>(party.mPlayersInParty);
		Collections.shuffle(playersInParty);

		for (DepthsPlayer dp : playersInParty) {
			if (dp == null || dp.mAbilities == null || dp.mAbilities.size() == 0) {
				continue;
			}

			Player p = Bukkit.getPlayer(dp.mPlayerId);
			try {
				if (p != null && p.isOnline() && !p.equals(damagee)) {
					EarthenWrath wrath = AbilityManager.getManager().getPlayerAbility(p, EarthenWrath.class);
					if (wrath != null && wrath.damagedEntity(damagee, event)) {
						break;
					}
				}
			} catch (Exception e) {
				Plugin.getInstance().getLogger().log(Level.INFO, "Exception for depths on entity damage- earthen wrath", e);
			}
		}
	}

	public boolean isWrathing() {
		return mAbsorbDamage;
	}

	private void startWrath() {
		mAbsorbDamage = true;
	}

	private void endWrath() {
		mAbsorbDamage = false;
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && !isOnCooldown() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand());
	}

	@Override
	public String getDescription(int rarity) {
		return "Swap while holding a weapon to redirect all damage allies take from mobs (excluding percent health damage) to you at a " + DepthsUtils.getRarityColor(rarity) + DepthsUtils.roundPercent(PERCENT_DAMAGE_REDUCTION[rarity - 1]) + "%" + ChatColor.WHITE + " damage reduction for " + DURATION / 20 + " seconds, then do a burst damage in a " + DAMAGE_RADIUS + " block radius around you, dealing " + DepthsUtils.getRarityColor(rarity) + (int) DepthsUtils.roundPercent(PERCENT_DAMAGE_REFLECTED[rarity - 1]) + "%" + ChatColor.WHITE + " of original damage absorbed. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.EARTHBOUND;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SWAP;
	}
}
