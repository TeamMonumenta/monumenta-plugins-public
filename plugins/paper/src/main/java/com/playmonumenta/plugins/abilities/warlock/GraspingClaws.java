package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.WeakHashMap;



public class GraspingClaws extends Ability {

	private static final int RADIUS = 8;
	private static final float PULL_SPEED = 0.175f;
	private static final double AMPLIFIER_1 = 0.2;
	private static final double AMPLIFIER_2 = 0.3;
	private static final int DAMAGE_1 = 3;
	private static final int DAMAGE_2 = 8;
	private static final int DURATION = 8 * 20;
	private static final int COOLDOWN_1 = 16 * 20;
	private static final int COOLDOWN_2 = 12 * 20;

	private final double mAmplifier;
	private final int mDamage;
	private WeakHashMap<Arrow, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap;

	public GraspingClaws(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Grasping Claws");
		mInfo.mScoreboardId = "GraspingClaws";
		mInfo.mShorthandName = "GC";
		mInfo.mDescriptions.add("Left-clicking while shifted while holding a bow or crossbow fires an arrow that pulls nearby enemies towards your arrow once it makes contact with a mob or block. Mobs caught in the arrow's 8 block radius are given 20% Slowness for 8 seconds and take 3 magic damage. Cooldown: 16s.");
		mInfo.mDescriptions.add("The pulled enemies now take 8 damage, and their Slowness is increased to 30%.");
		mInfo.mLinkedSpell = ClassAbility.GRASPING_CLAWS;
		mInfo.mCooldown = isLevelOne() ? COOLDOWN_1 : COOLDOWN_2;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.BOW, 1);
		mAmplifier = isLevelOne() ? AMPLIFIER_1 : AMPLIFIER_2;
		mDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
		mPlayerItemStatsMap = new WeakHashMap<>();
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), ClassAbility.GRASPING_CLAWS) && mPlayer.isSneaking() && ItemUtils.isBowOrTrident(inMainHand) && !ItemStatUtils.isShattered(inMainHand)) {
			Arrow arrow = mPlayer.getWorld().spawnArrow(mPlayer.getEyeLocation(), mPlayer.getLocation().getDirection(), 1.5f, 0, Arrow.class);
			arrow.setShooter(mPlayer);
			arrow.setDamage(0);
			arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
			mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.SPELL_WITCH);
			mPlayerItemStatsMap.put(arrow, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));
			putOnCooldown();
		}
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		ItemStatManager.PlayerItemStats playerItemStats = mPlayerItemStatsMap.remove(proj);
		if (mPlayer != null && playerItemStats != null) {
			Location loc = proj.getLocation();
			World world = proj.getWorld();

			world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1.25f, 1.25f);
			world.playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, 1.25f, 1.45f);
			world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, 1.25f, 0.65f);
			world.spawnParticle(Particle.PORTAL, loc, 125, 2, 2, 2, 0.25);
			world.spawnParticle(Particle.PORTAL, loc, 400, 0, 0, 0, 1.45);
			world.spawnParticle(Particle.DRAGON_BREATH, loc, 85, 0, 0, 0, 0.125);
			world.spawnParticle(Particle.FALLING_DUST, loc, 150, 2, 2, 2, Material.ANVIL.createBlockData());

			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, RADIUS, mPlayer)) {
				DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.mLinkedSpell, playerItemStats), mDamage, true, true, false);

				MovementUtils.pullTowards(proj, mob, PULL_SPEED);
				EntityUtils.applySlow(mPlugin, DURATION, mAmplifier, mob);
			}

			proj.remove();
		}
	}
}
