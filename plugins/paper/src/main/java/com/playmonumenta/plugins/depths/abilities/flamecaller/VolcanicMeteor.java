package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class VolcanicMeteor extends DepthsAbility {
	public static final String ABILITY_NAME = "Volcanic Meteor";
	public static final int[] DAMAGE = {40, 50, 60, 70, 80, 100};
	public static final int COOLDOWN_TICKS = 25 * 20;
	public static final int DISTANCE = 25;
	public static final int SIZE = 6;
	public static final int FIRE_TICKS = 3 * 20;

	public VolcanicMeteor(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mInfo.mCooldown = COOLDOWN_TICKS;
		mInfo.mIgnoreCooldown = true;
		mInfo.mLinkedSpell = ClassAbility.VOLCANIC_METEOR;
		mTree = DepthsTree.FLAMECALLER;
		mDisplayItem = Material.MAGMA_BLOCK;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (mPlayer == null) {
			return;
		}
		//sets swap event to cancelled so this doesn't become annoying
		event.setCancelled(true);
		if (!isTimerActive()) {
			putOnCooldown();

			FixedMetadataValue playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsMetadata(mPlayer);

			Location loc = mPlayer.getEyeLocation();
			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 0.85f);
			world.spawnParticle(Particle.LAVA, mPlayer.getLocation(), 15, 0.25f, 0.1f, 0.25f);
			world.spawnParticle(Particle.FLAME, mPlayer.getLocation(), 30, 0.25f, 0.1f, 0.25f, 0.15f);
			world.spawnParticle(Particle.SOUL_FIRE_FLAME, mPlayer.getLocation(), 30, 0.25f, 0.1f, 0.25f, 0.15f);
			Vector dir = loc.getDirection().normalize();
			for (int i = 0; i < DISTANCE; i++) {
				loc.add(dir);

				mPlayer.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
				int size = EntityUtils.getNearbyMobs(loc, 2, mPlayer).size();
				if (loc.getBlock().getType().isSolid() || i >= 24 || size > 0) {
					launchMeteor(loc, playerItemStats);
					break;
				}
			}
		}
	}


	private void launchMeteor(final Location loc, final FixedMetadataValue playerItemStats) {
		Location ogLoc = loc.clone();
		loc.add(0, 30, 0);
		new BukkitRunnable() {
			double mT = 0;
			@Override
			public void run() {
				if (mPlayer == null) {
					return;
				}
				mT += 1;
				World world = mPlayer.getWorld();
				for (int i = 0; i < 8; i++) {
					loc.subtract(0, 0.2, 0);
					if (loc.getBlock().getType().isSolid()) {
						if (loc.getY() - ogLoc.getY() <= 2) {
							world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0);

							world.spawnParticle(Particle.FLAME, loc, 175, 0, 0, 0, 0.235F);
							world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 175, 0, 0, 0, 0.235F);

							world.spawnParticle(Particle.SMOKE_LARGE, loc, 75, 0.25, 0.25, 0.25, 0.2F);
							world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 75, 0.25, 0.25, 0.25, 0.2F);
							this.cancel();

							double damage = DAMAGE[mRarity - 1];

							for (LivingEntity e : EntityUtils.getNearbyMobs(loc, SIZE, mPlayer)) {
								double distance = loc.distance(e.getLocation());
								double mult = Math.min(Math.max(0, (6 - distance) / 4), 1);

								EntityUtils.applyFire(mPlugin, FIRE_TICKS, e, mPlayer);
								DamageEvent damageEvent = new DamageEvent(e, mPlayer, mPlayer, DamageType.MAGIC, mInfo.mLinkedSpell, damage * mult);
								damageEvent.setDelayed(true);
								damageEvent.setPlayerItemStat(playerItemStats);
								DamageUtils.damage(damageEvent, false, true, null);
							}
							break;
						}
					}
				}
				world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1);
				world.spawnParticle(Particle.FLAME, loc, 25, 0.25F, 0.25F, 0.25F, 0.1F);
				world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 25, 0.25F, 0.25F, 0.25F, 0.1F);
				world.spawnParticle(Particle.SMOKE_LARGE, loc, 5, 0.25F, 0.25F, 0.25F, 0.1F);

				if (mT >= 150) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public String getDescription(int rarity) {
		return "Swap hands to summon a falling meteor location where you are looking, up to " + DISTANCE + " blocks away. When the meteor lands, it deals " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " magic damage in a " + SIZE + " block radius, but the damage is reduced depending on the distance from the center. Cooldown: " + COOLDOWN_TICKS / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FLAMECALLER;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SWAP;
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && !isOnCooldown() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand());
	}
}

