package com.playmonumenta.plugins.depths.abilities.earthbound;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

import net.md_5.bungee.api.ChatColor;

public class EarthenWrath extends DepthsAbility {

	public static final String ABILITY_NAME = "Earthen Wrath";
	private static final double[] PERCENT_DAMAGE_REFLECTED = {0.8, 1.0, 1.2, 1.4, 1.6};
	private static final double[] PERCENT_DAMAGE_RECEIVED = {0.55, 0.575, 0.6, 0.625, 0.65};
	private static final int COOLDOWN = 20 * 30;
	private static final int DURATION = 6 * 20;
	private static final int DAMAGE_RADIUS = 6;

	public boolean mAbsorbDamage = false;
	private double mDamageAbsorbed;

	public EarthenWrath(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.TURTLE_HELMET;
		mTree = DepthsTree.EARTHBOUND;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.EARTHEN_WRATH;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {

		event.setCancelled(true);
		putOnCooldown();
		mDamageAbsorbed = 0;

		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 10, 1);

		DepthsParty party = DepthsManager.getInstance().getPartyFromId(DepthsManager.getInstance().mPlayers.get(mPlayer.getUniqueId()));

		new BukkitRunnable() {
			private int mTicks = 0;
			private float mPitch = 0.5f;
			@Override
			public void run() {
				mAbsorbDamage = true;
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
					mAbsorbDamage = false;

					Location loc = mPlayer.getLocation();

					if (mDamageAbsorbed > 0) {
						for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, DAMAGE_RADIUS)) {
							if (!(mob instanceof Player)) {
								EntityUtils.damageEntity(mPlugin, mob, mDamageAbsorbed * PERCENT_DAMAGE_REFLECTED[mRarity - 1], mPlayer, MagicType.PHYSICAL, true, mInfo.mLinkedSpell);
							}
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

	public void damagedEntity(Player player, EntityDamageEvent event) {
		if (AbilityUtils.isBlocked(event)) {
			return;
		}

		DepthsPlayer dp = DepthsManager.getInstance().mPlayers.get(player.getUniqueId());

		if (dp != null) {
			EarthenWrath otherWrath = AbilityManager.getManager().getPlayerAbility(player, EarthenWrath.class);
			if (otherWrath != null) {
				if (otherWrath.mAbsorbDamage) {
					return;
				}
			}
		}
		if (mAbsorbDamage && !player.equals(mPlayer)) {
			mDamageAbsorbed += event.getDamage();

			EntityUtils.damageEntity(mPlugin, mPlayer, event.getDamage() * PERCENT_DAMAGE_RECEIVED[mRarity - 1], null, MagicType.PHYSICAL, true, mInfo.mLinkedSpell);

			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1, 2);
			world.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 2);
			world.spawnParticle(Particle.TOTEM, player.getLocation(), 30, 0.1, 0.1, 0.1, 0.6);

			Location pLoc = player.getLocation().add(0, 0.5, 0);
			Vector dir = mPlayer.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
			for (int i = 0; i <= mPlayer.getLocation().distance(player.getLocation()); i++) {
				pLoc.add(dir);

				world.spawnParticle(Particle.VILLAGER_HAPPY, pLoc, 3, 0.25, 0.25, 0.25, 0);
				world.spawnParticle(Particle.CLOUD, pLoc, 6, 0.05, 0.05, 0.05, 0.05);
			}

			event.setDamage(0);
		}
	}

	@Override
	public boolean runCheck() {
		return (!isOnCooldown() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand()));
	}

	@Override
	public String getDescription(int rarity) {
		return "Swap while holding a weapon to redirect damage taken by allies to you at a " + DepthsUtils.getRarityColor(rarity) + DepthsUtils.roundPercent(PERCENT_DAMAGE_RECEIVED[rarity - 1]) + "%" + ChatColor.WHITE + " damage reduction for " + DURATION / 20 + " seconds, then do a burst damage in a " + DAMAGE_RADIUS + " block radius around you, dealing " + DepthsUtils.getRarityColor(rarity) + DepthsUtils.roundPercent(PERCENT_DAMAGE_REFLECTED[rarity - 1]) + "%" + ChatColor.WHITE + " of damage absorbed. Cooldown: " + COOLDOWN / 20 + "s.";
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
