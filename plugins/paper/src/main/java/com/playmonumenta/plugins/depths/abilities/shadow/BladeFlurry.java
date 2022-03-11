package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BladeFlurry extends DepthsAbility {

	public static final String ABILITY_NAME = "Blade Flurry";
	public static final int COOLDOWN = 20 * 6;
	public static final int[] DAMAGE = {8, 10, 12, 14, 16, 20};
	public static final int RADIUS = 3;
	public static final int[] SILENCE_DURATION = {20, 25, 30, 35, 40, 50};

	public BladeFlurry(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayMaterial = Material.IRON_SWORD;
		mTree = DepthsTree.SHADOWS;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.BLADE_FLURRY;
	}

	@Override
	public void cast(Action trigger) {

		putOnCooldown();

		World mWorld = mPlayer.getWorld();
		Location loc = mPlayer.getEyeLocation().add(0, -0.5, 0);
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, 3);
		for (LivingEntity mob : mobs) {
			EntityUtils.applySilence(mPlugin, SILENCE_DURATION[mRarity - 1], mob);
			DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, DAMAGE[mRarity - 1], mInfo.mLinkedSpell);
			MovementUtils.knockAway(mPlayer, mob, 0.8f, true);
		}
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.75f);

		new BukkitRunnable() {
			Vector mEyeDir = loc.getDirection();

			double mStartAngle = Math.atan(mEyeDir.getZ()/mEyeDir.getX());
			int mIncrementDegrees = 0;
			@Override
			public void run() {
				if (mIncrementDegrees == 0) {
					if (mEyeDir.getX() < 0) {
						mStartAngle += Math.PI;
					}
					mStartAngle += Math.PI * 90 / 180;
				}
				Location mLoc = mPlayer.getEyeLocation().add(0, -0.5, 0);
				Vector direction = new Vector(Math.cos(mStartAngle - Math.PI * mIncrementDegrees / 180), 0, Math.sin(mStartAngle - Math.PI * mIncrementDegrees / 180));
				Location bladeLoc = mLoc.clone().add(direction.clone().multiply(3.0));

				mWorld.spawnParticle(Particle.SPELL_WITCH, bladeLoc, 10, 0.35, 0, 0.35, 1);
				mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 0.5f);

				if (mIncrementDegrees >= 360) {
					this.cancel();
				}

				mIncrementDegrees += 30;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean runCheck() {
		return (mPlayer.isSneaking() && !isOnCooldown() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand()));
	}

	@Override
	public String getDescription(int rarity) {
		return "Right click while sneaking and holding a weapon to deal " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " melee damage in a " + RADIUS + " block radius around you. Affected mobs are silenced for " + DepthsUtils.getRarityColor(rarity) + (SILENCE_DURATION[rarity - 1] / 20.0) + ChatColor.WHITE + " seconds and knocked away slightly. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SHADOWS;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_RIGHT_CLICK;
	}
}

