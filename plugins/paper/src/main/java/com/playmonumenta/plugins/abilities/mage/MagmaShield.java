package com.playmonumenta.plugins.abilities.mage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.SpellDamage;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import com.playmonumenta.plugins.abilities.mage.elementalist.Blizzard;

public class MagmaShield extends Ability {

	private static final int COOLDOWN = 12 * 20;
	private static final int RADIUS = 6;
	private static final int FIRE_DURATION = 4 * 20;
	private static final int DAMAGE_1 = 6;
	private static final int DAMAGE_2 = 12;
	private static final float KNOCKBACK_SPEED = 0.5f;
	private static final double DOT_ANGLE = 0.33;
	private static final double BLIZZARD_ANGLE = 50.0;

	private final int mDamage;
	
	private Blizzard mBlizzard;
	private boolean mBlizzardAngle = false;

	public MagmaShield(Plugin plugin, Player player) {
		super(plugin, player, "Magma Shield");
		mInfo.mLinkedSpell = Spells.MAGMA_SHIELD;
		mInfo.mScoreboardId = "Magma";
		mInfo.mShorthandName = "MS";
		mInfo.mDescriptions.add("When you right click while you are sneaking, you summon a torrent of flames, knocking all enemies within 6 blocks that are in front of you away, dealing 6 damage and setting them on fire. You must hold a wand to trigger this effect. Cooldown: 12s.");
		mInfo.mDescriptions.add("The damage is increased to 12.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player != null) {
				mBlizzard = AbilityManager.getManager().getPlayerAbility(mPlayer, Blizzard.class);
			}
		});
	}

	@Override
	public void cast(Action action) {
		putOnCooldown();

		float damage = SpellDamage.getSpellDamage(mPlayer, mDamage);
		Vector playerDir = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RADIUS, mPlayer)) {
			Vector toMobVector = mob.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();
			if (playerDir.dot(toMobVector) > DOT_ANGLE) {
				float kb = (mob instanceof Player) ? 0.3f : KNOCKBACK_SPEED;
				MovementUtils.knockAway(mPlayer, mob, kb);
				EntityUtils.applyFire(mPlugin, FIRE_DURATION, mob, mPlayer);
				EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer, MagicType.FIRE, true, mInfo.mLinkedSpell);
			}
		}

		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation(), 15, 0.05, 0.05, 0.05, 0.1);
		new BukkitRunnable() {
			final Location mLoc = mPlayer.getLocation();
			double mRadius = 0;

			@Override
			public void run() {
				if (mRadius == 0) {
					mLoc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				mRadius += 1.25;
				for (double degree = 30; degree <= 150; degree += 10) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * mRadius, 0.125, FastUtils.sin(radian1) * mRadius);
					vec = VectorUtils.rotateXAxis(vec, -mLoc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().add(0, 0.1, 0).add(vec);
					world.spawnParticle(Particle.FLAME, l, 2, 0.15, 0.15, 0.15, 0.15);
					world.spawnParticle(Particle.SMOKE_NORMAL, l, 3, 0.15, 0.15, 0.15, 0.1);
				}

				if (mRadius >= RADIUS + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1f, 1.25f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
	}

	@Override
	public boolean runCheck() {
		if (mBlizzard != null) {
			mBlizzardAngle = true;
		}
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		double pitch = mPlayer.getLocation().getPitch();
		if (InventoryUtils.isWandItem(mainHand)) {
			if (mBlizzardAngle) {
				return mPlayer.isSneaking() && pitch > -BLIZZARD_ANGLE;
			}
			return mPlayer.isSneaking();
		}
		return false;
	}

}
