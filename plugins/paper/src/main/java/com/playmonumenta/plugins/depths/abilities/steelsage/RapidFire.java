package com.playmonumenta.plugins.depths.abilities.steelsage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.windwalker.Skyhook;
import com.playmonumenta.plugins.enchantments.PointBlank;
import com.playmonumenta.plugins.enchantments.Sniper;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

import net.md_5.bungee.api.ChatColor;

public class RapidFire extends DepthsAbility {

	public static final String ABILITY_NAME = "Rapid Fire";
	public static final int[] ARROWS = {4, 5, 6, 7, 8, 10};
	public static final int DAMAGE = 12;
	public static final int COOLDOWN = 18 * 20;
	public static final String META_DATA_TAG = "RapidFireArrow";

	public RapidFire(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.REPEATER;
		mInfo.mLinkedSpell = ClassAbility.RAPIDFIRE;
		mTree = DepthsTree.METALLIC;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
	}

	@Override
	public void cast(Action action) {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		if (!InventoryUtils.isBowItem(inMainHand) || mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			return;
		}
		new BukkitRunnable() {
			int mCount = 0;
			@Override
			public void run() {

				if (mCount >= ARROWS[mRarity - 1]) {
					this.cancel();
					return;
				}

				ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
				if (InventoryUtils.isBowItem(inMainHand)) {
					Arrow arrow = mPlayer.getWorld().spawnArrow(mPlayer.getEyeLocation(), mPlayer.getLocation().getDirection(), 3.0f, 0, Arrow.class);
					arrow.setCritical(true);
					arrow.setMetadata(META_DATA_TAG, new FixedMetadataValue(mPlugin, 0));
					arrow.setShooter(mPlayer);
					arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);

					mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.ASH);
					Location loc = mPlayer.getLocation().add(0, 1, 0);
					loc.getWorld().playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, 1, 0.65f);
					loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1, 0.45f);
					ProjectileLaunchEvent eventLaunch = new ProjectileLaunchEvent(arrow);
					Bukkit.getPluginManager().callEvent(eventLaunch);
					if (arrow.hasMetadata(Sniper.LEVEL_METAKEY)) {
						arrow.removeMetadata(Sniper.LEVEL_METAKEY, mPlugin);
					}
					if (arrow.hasMetadata(PointBlank.LEVEL_METAKEY)) {
						arrow.removeMetadata(PointBlank.LEVEL_METAKEY, mPlugin);
					}
					if (arrow.hasMetadata(Skyhook.META_DATA_TAG)) {
						arrow.removeMetadata(Skyhook.META_DATA_TAG, mPlugin);
					}
					mCount++;
				}
			}
		}.runTaskTimer(mPlugin, 0, 3);
		putOnCooldown();
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
	    if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
	        cast(Action.LEFT_CLICK_AIR);
	    }

	    return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity le, EntityDamageByEntityEvent event) {
		ProjectileSource shooter = proj.getShooter();
		if (proj instanceof Arrow && shooter instanceof Player && le instanceof LivingEntity && proj.hasMetadata(RapidFire.META_DATA_TAG)) {
			EntityUtils.damageEntity(mPlugin, le, DAMAGE, (Player) shooter, MagicType.PHYSICAL, true, mInfo.mLinkedSpell, true, true, true, false);
			event.setCancelled(true);
			proj.remove();
		}
		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return String.format("Left clicking with a bow shoots a flurry of " + DepthsUtils.getRarityColor(rarity) + ARROWS[rarity - 1] + ChatColor.WHITE + " arrows in the direction that you are looking that deal " + DAMAGE + " damage, bypassing iframes. Cooldown: " + COOLDOWN / 20 + "s.");
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.METALLIC;
	}
}

