package com.playmonumenta.plugins.abilities.warlock.reaper;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class DeathsTouch extends Ability {

	public static final String DEATHS_TOUCH_BUFF_DURATION = "DeathsTouchBuffDuration";
	public static final String DEATHS_TOUCH_AMPLIFIER_CAP = "DeathsTouchAmplifierCap";
	public static final String DEATHS_TOUCH_CASTER = "DeathsTouchCaster";

	private static final String CHECK_ONCE_THIS_TICK_METAKEY = "DeathsTouchTickRightClicked";

	private static final int DEATHS_TOUCH_1_COOLDOWN = 25 * 20;
	private static final int DEATHS_TOUCH_2_COOLDOWN = 15 * 20;
	private static final int DEATHS_TOUCH_1_BUFF_DURATION = 15 * 20;
	private static final int DEATHS_TOUCH_2_BUFF_DURATION = 20 * 20;
	private static final int DEATHS_TOUCH_1_AMPLIFIER_CAP = 0;
	private static final int DEATHS_TOUCH_2_AMPLIFIER_CAP = 1;
	private static final int DEATHS_TOUCH_RANGE = 20;

	private final int mBuffDuration;
	private final int mAmplifierCap;

	// Although we now track the mob buffs on kill with metadata,
	// We still need this variable to easily apply particle effects
	private LivingEntity mTarget = null;
	private int mRightClicks = 0;

	public DeathsTouch(Plugin plugin, Player player) {
		super(plugin, player, "Death's Touch");
		mInfo.mLinkedSpell = Spells.DEATHS_TOUCH;
		mInfo.mScoreboardId = "DeathsTouch";
		mInfo.mShorthandName = "DT";
		mInfo.mDescriptions.add("Double right-clicking marks the enemy you are looking at as the reaper's next victim. If you do not correctly aim at a mob this skill goes on cooldown for 5s and it does nothing. If you or another player kills that enemy, the player that killed it and the warlock who marked the mob are granted 15s of level 1 buffs contrary to the debuffs affecting it. Weakness > Strength. Slowness (and custom percent Slowness) > Speed. Fire > Fire Resistance. Vulnerability > Resistance. Wither/Poison > Regeneration. Mining Fatigue > Haste. Blindness > Night Vision. Cooldown: 25s.");
		mInfo.mDescriptions.add("The killing player gets buffs for 20 seconds instead, and the level of the buff is preserved, up to level 2, except for Resistance and Regeneration. If the killing player is not the caster, the caster still recieves level 1 buffs. Cooldown: 15s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? DEATHS_TOUCH_1_COOLDOWN : DEATHS_TOUCH_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mBuffDuration = getAbilityScore() == 1 ? DEATHS_TOUCH_1_BUFF_DURATION : DEATHS_TOUCH_2_BUFF_DURATION;
		mAmplifierCap = getAbilityScore() == 1 ? DEATHS_TOUCH_1_AMPLIFIER_CAP : DEATHS_TOUCH_2_AMPLIFIER_CAP;
	}

	@Override
	public boolean runCheck() {
		return InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand());
	}

	@Override
	public void cast(Action action) {
		if (MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, CHECK_ONCE_THIS_TICK_METAKEY)) {
			mRightClicks++;
			new BukkitRunnable() {
				@Override
				public void run() {
					if (mRightClicks > 0) {
						mRightClicks--;
					}
					this.cancel();
				}
			}.runTaskLater(mPlugin, 5);
		}
		if (mRightClicks < 2) {
			return;
		}
		mRightClicks = 0;

		Location loc = mPlayer.getEyeLocation();
		Vector dir = loc.getDirection();
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.5f, 0.25f);

		// Get a list of mobs that can possibly be hit - so we don't have to ask the game for nearby mobs every time
		List<LivingEntity> mobsInRange = EntityUtils.getNearbyMobs(loc, DEATHS_TOUCH_RANGE, mPlayer);
		BoundingBox box = BoundingBox.of(loc, 1, 1, 1);
		for (int i = 0; i < DEATHS_TOUCH_RANGE; i++) {
			box.shift(dir);
			Location bloc = box.getCenter().toLocation(world);
			world.spawnParticle(Particle.SPELL_MOB, bloc, 5, 0.15, 0.15, 0.15, 0);
			for (LivingEntity mob : mobsInRange) {
				if (mob.getBoundingBox().overlaps(box)) {
					mTarget = mob;
					mob.setMetadata(DEATHS_TOUCH_BUFF_DURATION, new FixedMetadataValue(mPlugin, mBuffDuration));
					mob.setMetadata(DEATHS_TOUCH_AMPLIFIER_CAP, new FixedMetadataValue(mPlugin, mAmplifierCap));
					mob.setMetadata(DEATHS_TOUCH_CASTER, new FixedMetadataValue(mPlugin, mPlayer.getName()));
					world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.25f, 1f);

					new BukkitRunnable() {
						final int mRunnableDuration = getAbilityScore() == 1 ? DEATHS_TOUCH_1_COOLDOWN : DEATHS_TOUCH_2_COOLDOWN;
						final double mWidth = mob.getWidth() / 2;
						int mT = 0;

						@Override
						public void run() {
							mT++;
							if (mTarget != null) {
								mPlayer.spawnParticle(Particle.SPELL_MOB, mTarget.getLocation().add(0, mob.getHeight() / 2, 0), 1, mWidth, mWidth, mWidth, 0);
								mPlayer.spawnParticle(Particle.SPELL_WITCH, mTarget.getLocation().add(0, mob.getHeight() / 2, 0), 1, mWidth, mWidth, mWidth, 0);
							}
							if (mT >= mRunnableDuration || (mTarget != null && (mTarget.isDead() || !mTarget.isValid()))) {
								this.cancel();
								if (mTarget != null) {
									mTarget.removeMetadata(DEATHS_TOUCH_BUFF_DURATION, mPlugin);
									mTarget.removeMetadata(DEATHS_TOUCH_AMPLIFIER_CAP, mPlugin);
								}
								mTarget = null;
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);

					// This loop only runs at most once!
					putOnCooldown();
				}
			}
		}
		if (mInfo.mLinkedSpell != null) {
			if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
				mPlugin.mTimers.addCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell, 20 * 5);
				PlayerUtils.callAbilityCastEvent(mPlayer, mInfo.mLinkedSpell);
			}
		}
	}

	// The buffs will be applied in the DeathsTouchNonReaper ability for all players

}
