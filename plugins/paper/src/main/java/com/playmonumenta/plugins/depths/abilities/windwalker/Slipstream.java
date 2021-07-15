package com.playmonumenta.plugins.depths.abilities.windwalker;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

import net.md_5.bungee.api.ChatColor;

public class Slipstream extends DepthsAbility {

	public static final String ABILITY_NAME = "Slipstream";
	public static final int[] COOLDOWN = {16, 14, 12, 10, 8};
	private static final int DURATION = 8 * 20;
	private static final double SPEED_AMPLIFIER = 0.2;
	private static final String PERCENT_SPEED_EFFECT_NAME = "SlipstreamSpeedEffect";
	private static final int JUMP_AMPLIFIER = 2;
	private static final int RADIUS = 3;
	private static final float KNOCKBACK_SPEED = 0.7f;

	public Slipstream(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.PHANTOM_MEMBRANE;
		mTree = DepthsTree.WINDWALKER;
		mInfo.mCooldown = (mRarity == 0) ? 16 * 20 : COOLDOWN[mRarity - 1] * 20;
		mInfo.mLinkedSpell = ClassAbility.SLIPSTREAM;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action trigger) {
		putOnCooldown();

		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, SPEED_AMPLIFIER, PERCENT_SPEED_EFFECT_NAME));
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, DURATION, JUMP_AMPLIFIER));

		Location loc = mPlayer.getEyeLocation();
		Location pLoc = loc;
		pLoc.add(0, -0.75, 0);
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, 1.0f, 0.25f);

		new BukkitRunnable() {
			double mRadius = 0;
			@Override
			public void run() {
				mRadius += 1.25;
				for (double j = 0; j < 360; j += 18) {
					double radian1 = Math.toRadians(j);
					pLoc.add(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
					world.spawnParticle(Particle.CLOUD, pLoc, 3, 0, 0, 0, 0.125);
					world.spawnParticle(Particle.EXPLOSION_NORMAL, pLoc, 1, 0, 0, 0, 0.15);
					pLoc.subtract(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
				}
				if (mRadius >= RADIUS + 1) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, RADIUS, mPlayer)) {
			MovementUtils.knockAway(mPlayer.getLocation(), mob, KNOCKBACK_SPEED, KNOCKBACK_SPEED / 2);
		}
	}

	@Override
	public String getDescription(int rarity) {
		return "Right click to knock all enemies within " + RADIUS + " blocks away from you and gain Jump Boost " + (JUMP_AMPLIFIER + 1) + " and " + DepthsUtils.roundPercent(SPEED_AMPLIFIER) + "% speed for " + DURATION / 20 + " seconds. Cooldown: " + DepthsUtils.getRarityColor(rarity) + COOLDOWN[rarity - 1] + "s" + ChatColor.WHITE + ".";
	}

	@Override
	public boolean runCheck() {
		return (!mPlayer.isSneaking() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand()));
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.WINDWALKER;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.RIGHT_CLICK;
	}
}

