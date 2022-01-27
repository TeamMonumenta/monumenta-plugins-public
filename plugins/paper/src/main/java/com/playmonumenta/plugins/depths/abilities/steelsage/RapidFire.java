package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.windwalker.Skyhook;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

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
		if (!ItemUtils.isSomeBow(inMainHand) || mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
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
				if (ItemUtils.isSomeBow(inMainHand)) {
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
					if (arrow.hasMetadata(Skyhook.META_DATA_TAG)) {
						arrow.removeMetadata(Skyhook.META_DATA_TAG, mPlugin);
					}
					mCount++;
				} else {
					this.cancel();
					return;
				}
			}
		}.runTaskTimer(mPlugin, 0, 3);
		putOnCooldown();
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
	        cast(Action.LEFT_CLICK_AIR);
	        return;
	    }

		if (event.getDamager() instanceof Arrow arrow && arrow.hasMetadata(RapidFire.META_DATA_TAG)) {
			DamageUtils.damage(mPlayer, enemy, DamageType.PROJECTILE_SKILL, DAMAGE, mInfo.mLinkedSpell, true);
			event.setCancelled(true);
			arrow.remove();
		}
	}

	@Override
	public String getDescription(int rarity) {
		return String.format("Left clicking with a bow shoots a flurry of " + DepthsUtils.getRarityColor(rarity) + ARROWS[rarity - 1] + ChatColor.WHITE + " arrows in the direction that you are looking that deal " + DAMAGE + " projectile damage, bypassing iframes. Cooldown: " + COOLDOWN / 20 + "s.");
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.METALLIC;
	}
}

