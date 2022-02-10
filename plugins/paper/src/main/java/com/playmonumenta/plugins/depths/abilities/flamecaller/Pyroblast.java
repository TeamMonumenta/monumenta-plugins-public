package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.WeakHashMap;

public class Pyroblast extends DepthsAbility {

	public static final String ABILITY_NAME = "Pyroblast";

	public static final int COOLDOWN = 12 * 20;
	public static final int[] DAMAGE = {20, 25, 30, 35, 40, 50};
	private static final int RADIUS = 4;
	private static final int DURATION = 4 * 20;

	private WeakHashMap<AbstractArrow, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap;

	public Pyroblast(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.TNT_MINECART;
		mTree = DepthsTree.FLAMECALLER;
		mInfo.mLinkedSpell = ClassAbility.PYROBLAST;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mPlayerItemStatsMap = new WeakHashMap<>();
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		Entity damager = event.getDamager();
		if (event.getType() == DamageType.PROJECTILE && damager != null && damager instanceof AbstractArrow arrow && mPlayerItemStatsMap.containsKey(arrow)) {
			explode(arrow, enemy.getLocation());
		}
		return false; // prevents multiple calls itself by removing the arrow
	}

	private void explode(AbstractArrow arrow, Location loc) {
		ItemStatManager.PlayerItemStats playerItemStats = mPlayerItemStatsMap.remove(arrow);
		if (mPlayer != null && playerItemStats != null) {
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, RADIUS);
			for (LivingEntity mob : mobs) {
				EntityUtils.applyFire(mPlugin, DURATION, mob, mPlayer);
				DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.mLinkedSpell, playerItemStats), DAMAGE[mRarity - 1], false, true, null);
			}
			World world = arrow.getWorld();
			world.spawnParticle(Particle.EXPLOSION_HUGE, loc, 1, 0, 0, 0);
			world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 40, 2, 2, 2, 0);
			world.spawnParticle(Particle.FLAME, loc, 40, 2, 2, 2, 0);
			world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
			arrow.remove();
		}
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (mPlayer == null || mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			return true;
		}

		if (mPlayer.isSneaking()) {
			mInfo.mCooldown = (int) (COOLDOWN * BowAspect.getCooldownReduction(mPlayer));
			putOnCooldown();
			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.4f);

			arrow.setPierceLevel(0);
			arrow.setCritical(true);
			arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
			arrow.setVelocity(mPlayer.getLocation().getDirection().multiply(2.0));

			mPlayerItemStatsMap.put(arrow, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));

			mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.SOUL_FIRE_FLAME);
			mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.CAMPFIRE_SIGNAL_SMOKE);

			new BukkitRunnable() {
				int mT = 0;
				@Override
				public void run() {

					if (mT > COOLDOWN || !mPlayerItemStatsMap.containsKey(arrow)) {
						arrow.remove();

						this.cancel();
					}
					if (arrow.getVelocity().length() < .05 || arrow.isOnGround()) {
						explode(arrow, arrow.getLocation());

						this.cancel();
					}
					mT++;
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "Shooting a bow or trident while sneaking fires an exploding arrow, which deals " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " magic damage within a " + RADIUS + " block radius of it and sets nearby mobs on fire for " + DURATION / 20 + " seconds upon impact. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FLAMECALLER;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_BOW;
	}
}

