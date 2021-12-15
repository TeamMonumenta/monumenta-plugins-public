package com.playmonumenta.plugins.depths.abilities.windwalker;

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

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;

import net.md_5.bungee.api.ChatColor;

public class HowlingWinds extends DepthsAbility {

	public static final String ABILITY_NAME = "Howling Winds";
	public static final double[] DAMAGE = {1.0, 1.25, 1.5, 1.75, 2.0, 2.5};
	public static final int COOLDOWN = 25 * 20;
	public static final int DAMAGE_RADIUS = 4;
	public static final int PULL_RADIUS = 16;
	public static final int DISTANCE = 6;
	public static final int DAMAGE_INTERVAL = 20;
	public static final int[] PULL_INTERVAL = {20, 18, 16, 14, 12, 8};
	public static final int DURATION_TICKS = 6 * 20;
	public static final double PULL_VELOCITY = 0.6;
	public static final double BASE_RATIO = 0.15;

	public HowlingWinds(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.HOPPER;
		mTree = DepthsTree.WINDWALKER;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mInfo.mLinkedSpell = ClassAbility.HOWLINGWINDS;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);
		if (mPlayer != null && !isTimerActive() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand())) {
			putOnCooldown();

			Location loc = mPlayer.getEyeLocation();
			World world = mPlayer.getWorld();
			world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, 0.8f, 0.25f);
			world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.0f, 1.2f);
			world.spawnParticle(Particle.CLOUD, mPlayer.getLocation(), 15, 0.25f, 0.1f, 0.25f);
			Vector dir = loc.getDirection().normalize();
			for (int i = 0; i < DISTANCE; i++) {
				loc.add(dir);

				world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 5, 0.1, 0.1, 0.1, 0.1);
				world.spawnParticle(Particle.CLOUD, loc, 5, 0.1, 0.1, 0.1, 0.1);
				int size = EntityUtils.getNearbyMobs(loc, 2, mPlayer).size();
				if (loc.getBlock().getType().isSolid() || i >= DISTANCE - 1 || size > 0) {
					explode(loc);
					break;
				}
			}
		}
	}

	private void explode(Location loc) {
		if (mPlayer == null) {
			return;
		}
		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.CLOUD, loc, 35, 4, 4, 4, 0.125);
		world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 25, 2, 2, 2, 0.125);
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1f);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				if (mTicks % DAMAGE_INTERVAL == 0) {
					for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, DAMAGE_RADIUS)) {
						EntityUtils.damageEntity(mPlugin, mob, DAMAGE[mRarity - 1], mPlayer, MagicType.ICE, true, mInfo.mLinkedSpell, false, false, true, true);
					}
				}
				if (mTicks % PULL_INTERVAL[mRarity - 1] == 0) {
					for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, PULL_RADIUS)) {
						if (!(EntityUtils.isBoss(mob) || DepthsUtils.isPlant(mob) || mob.getScoreboardTags().contains(CrowdControlImmunityBoss.identityTag))) {
							Vector vector = mob.getLocation().toVector().subtract(loc.toVector());
							double ratio = BASE_RATIO + vector.length() / PULL_RADIUS;
							mob.setVelocity(mob.getVelocity().add(vector.normalize().multiply(PULL_VELOCITY).multiply(-ratio).add(new Vector(0, 0.1 + 0.2 * ratio, 0))));
						}
					}
					if (mTicks <= DURATION_TICKS - 5 * 20) {
						world.playSound(loc, Sound.ITEM_ELYTRA_FLYING, 0.8f, 1);
					}
				}
				world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 6, 2, 2, 2, 0.1);
				world.spawnParticle(Particle.CLOUD, loc, 4, 2, 2, 2, 0.05);
				world.spawnParticle(Particle.CLOUD, loc, 3, 0.1, 0.1, 0.1, 0.15);
				if (mTicks >= DURATION_TICKS) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public String getDescription(int rarity) {
		return "Swap hands to summon a hurricane that lasts " + DURATION_TICKS / 20 + " seconds at the location you are looking at, up to " + DISTANCE + " blocks away. The hurricane pulls enemies within " + PULL_RADIUS + " blocks towards its center every " + DepthsUtils.getRarityColor(rarity) + PULL_INTERVAL[rarity - 1] / 20.0 + ChatColor.WHITE + " seconds and deals " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " damage to enemies within " + DAMAGE_RADIUS + " blocks once a second. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.WINDWALKER;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SWAP;
	}
}

