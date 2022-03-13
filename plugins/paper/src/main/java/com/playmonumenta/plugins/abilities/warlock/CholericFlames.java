package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;



public class CholericFlames extends Ability {

	private static final int RADIUS = 8;
	private static final int DAMAGE_1 = 3;
	private static final int DAMAGE_2 = 5;
	private static final int DURATION = 7 * 20;
	private static final int COOLDOWN = 10 * 20;

	private final int mDamage;

	public CholericFlames(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Choleric Flames");
		mInfo.mScoreboardId = "CholericFlames";
		mInfo.mShorthandName = "CF";
		mInfo.mDescriptions.add("Sneaking and right-clicking while not looking down while holding a scythe knocks back and ignites mobs within 8 blocks of you for 7s, additionally dealing 3 magic damage. Cooldown: 10s.");
		mInfo.mDescriptions.add("The damage is increased to 5, and also afflict mobs with Hunger I.");
		mInfo.mLinkedSpell = ClassAbility.CHOLERIC_FLAMES;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.FIRE_CHARGE, 1);
		mDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		Location loc = mPlayer.getLocation();

		World world = mPlayer.getWorld();
		new BukkitRunnable() {
			double mRadius = 0;
			final Location mLoc = mPlayer.getLocation();

			@Override
			public void run() {
				mRadius += 1.25;
				for (double j = 0; j < 360; j += 18) {
					double radian1 = Math.toRadians(j);
					mLoc.add(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
					world.spawnParticle(Particle.FLAME, mLoc, 2, 0, 0, 0, 0.125);
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, mLoc, 2, 0, 0, 0, 0.125);
					world.spawnParticle(Particle.SMOKE_NORMAL, mLoc, 1, 0, 0, 0, 0.15);
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
				}

				if (mRadius >= RADIUS + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		world.spawnParticle(Particle.SMOKE_LARGE, loc, 30, 0, 0, 0, 0.15);
		world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.35f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.35f);

		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RADIUS, mPlayer)) {
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, mInfo.mLinkedSpell, true);
			EntityUtils.applyFire(mPlugin, DURATION, mob, mPlayer);

			if (isLevelTwo()) {
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.HUNGER, DURATION, 0, false, true));
			}
		}

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())
		       && mPlayer.getLocation().getPitch() < 50;
	}
}
