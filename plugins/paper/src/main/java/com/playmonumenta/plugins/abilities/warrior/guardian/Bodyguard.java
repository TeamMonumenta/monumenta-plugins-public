package com.playmonumenta.plugins.abilities.warrior.guardian;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class Bodyguard extends Ability {

	private static final double BODYGUARD_1_DAMAGE_RESISTANCE = 0.05;
	private static final double BODYGUARD_2_DAMAGE_RESISTANCE = 0.1;
	private static final int BODYGUARD_COOLDOWN = 30 * 20;
	private static final int BODYGUARD_RANGE = 25;
	private static final int BODYGUARD_RADIUS = 4;
	private static final int BODYGUARD_1_ARMOR = 2;
	private static final int BODYGUARD_2_ARMOR = 4;
	private static final int BODYGUARD_1_ABSORPTION_AMPLIFIER = 0;
	private static final int BODYGUARD_2_ABSORPTION_AMPLIFIER = 1;
	private static final int BODYGUARD_BUFF_DURATION = 8 * 20;
	private static final int BODYGUARD_STUN_DURATION = 3 * 20;

	private final double mDamageResistance;
	private final int mArmor;
	private final int mAbsorptionAmplifier;

	private int mLeftClicks = 0;

	public Bodyguard(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Bodyguard");
		mInfo.mScoreboardId = "Bodyguard";
		mInfo.mShorthandName = "Bg";
		mInfo.mDescriptions.add("Passively gain 5% damage resistance. Left-click twice without hitting a mob while looking directly at another player without hitting any mobs makes you charge to them (max range: 25 blocks). You are immune to damage and knockback during the charge. Upon arriving you knockback all mobs within 4 blocks. Both you and the other player get +2 armor and Absorption 1 for 8s. Left-click twice while looking down to cast on yourself. Cooldown: 30s.");
		mInfo.mDescriptions.add("Passively gain 10% damage resistance. Both you and the other player gain +4 armor and Absorption 2 for 8s instead. Additionally affected mobs are stunned for 3s.");
		mInfo.mLinkedSpell = Spells.BODYGUARD;
		mInfo.mCooldown = BODYGUARD_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mDamageResistance = getAbilityScore() == 1 ? BODYGUARD_1_DAMAGE_RESISTANCE : BODYGUARD_2_DAMAGE_RESISTANCE;
		mArmor = getAbilityScore() == 1 ? BODYGUARD_1_ARMOR : BODYGUARD_2_ARMOR;
		mAbsorptionAmplifier = getAbilityScore() == 1 ? BODYGUARD_1_ABSORPTION_AMPLIFIER : BODYGUARD_2_ABSORPTION_AMPLIFIER;
	}

	@Override
	public void cast(Action action) {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)
				|| ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return;
		}

		BoundingBox box = BoundingBox.of(mPlayer.getEyeLocation(), 1, 1, 1);
		Location oLoc = mPlayer.getLocation();
		boolean lookingDown = oLoc.getPitch() > 50;
		Vector dir = oLoc.getDirection();
		List<Player> players = PlayerUtils.playersInRange(mPlayer.getEyeLocation(), BODYGUARD_RANGE);
		players.remove(mPlayer);
		for (int i = 0; i < BODYGUARD_RANGE; i++) {
			box.shift(dir);
			Location bLoc = box.getCenter().toLocation(mWorld);
			if (bLoc.getBlock().getType().isSolid()) {
				if (lookingDown) {
					mLeftClicks++;
					new BukkitRunnable() {
						@Override
						public void run() {
							if (mLeftClicks > 0) {
								mLeftClicks--;
							}
							this.cancel();
						}
					}.runTaskLater(mPlugin, 5);
				}

				break;
			}
			for (Player player : players) {
				// If looking at another player, or reached the end of range check and looking down
				if (player.getBoundingBox().overlaps(box)) {
					// Double LClick detection
					mLeftClicks++;
					new BukkitRunnable() {
						@Override
						public void run() {
							if (mLeftClicks > 0) {
								mLeftClicks--;
							}
							this.cancel();
						}
					}.runTaskLater(mPlugin, 5);
					if (mLeftClicks < 2) {
						return;
					}
					// Don't set mLeftClicks to 0, self cast below handles that

					Location loc = mPlayer.getEyeLocation();
					for (int j = 0; j < 45; j++) {
						loc.add(dir.clone().multiply(0.33));
						mWorld.spawnParticle(Particle.FLAME, loc, 4, 0.25, 0.25, 0.25, 0f);
						if (loc.distance(bLoc) < 1) {
							break;
						}
					}

					//Flame
					for (int k = 0; k < 120; k++) {
						double x = FastUtils.randomDoubleInRange(-3, 3);
						double z = FastUtils.randomDoubleInRange(-3, 3);
						Location to = player.getLocation().add(x, 0.15, z);
						Vector pdir = LocationUtils.getDirectionTo(to, player.getLocation().add(0, 0.15, 0));
						mWorld.spawnParticle(Particle.FLAME, player.getLocation().add(0, 0.15, 0), 0, (float) pdir.getX(), 0f, (float) pdir.getZ(), FastUtils.randomDoubleInRange(0.1, 0.4));
					}

					//Explosion_Normal
					for (int k = 0; k < 60; k++) {
						double x = FastUtils.randomDoubleInRange(-3, 3);
						double z = FastUtils.randomDoubleInRange(-3, 3);
						Location to = player.getLocation().add(x, 0.15, z);
						Vector pdir = LocationUtils.getDirectionTo(to, player.getLocation().add(0, 0.15, 0));
						mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, player.getLocation().add(0, 0.15, 0), 0, (float) pdir.getX(), 0f, (float) pdir.getZ(), FastUtils.randomDoubleInRange(0.15, 0.5));
					}

					if (mPlayer.getLocation().distance(player.getLocation()) > 1) {
						mPlayer.teleport(player.getLocation().clone().subtract(dir.clone().multiply(0.5)).add(0, 0.5, 0));
					}

					Location tloc = player.getLocation().clone().subtract(dir.clone().multiply(0.5)).add(0, 0.5, 0);
					mWorld.playSound(tloc, Sound.ENTITY_BLAZE_SHOOT, 1, 0.75f);
					mWorld.playSound(tloc, Sound.ENTITY_ENDER_DRAGON_HURT, 1, 0.9f);

					mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER,
							new PotionEffect(PotionEffectType.ABSORPTION, BODYGUARD_BUFF_DURATION, mAbsorptionAmplifier, false, true));

					AttributeInstance armor = player.getAttribute(Attribute.GENERIC_ARMOR);
					if (armor != null) {
						armor.setBaseValue(armor.getBaseValue() + mArmor);
						new BukkitRunnable() {
							@Override
							public void run() {
								armor.setBaseValue(armor.getBaseValue() - mArmor);
							}
						}.runTaskLater(mPlugin, BODYGUARD_BUFF_DURATION);
					}
				}
			}
		}

		// Self trigger
		if (mLeftClicks < 2) {
			return;
		}
		mLeftClicks = 0;
		putOnCooldown();

		mWorld.playSound(oLoc, Sound.ENTITY_BLAZE_SHOOT, 1, 0.75f);
		mWorld.spawnParticle(Particle.FLAME, oLoc.add(0, 0.15, 0), 25, 0.2, 0, 0.2, 0.1);

		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
				new PotionEffect(PotionEffectType.ABSORPTION, BODYGUARD_BUFF_DURATION, mAbsorptionAmplifier, false, true));

		AttributeInstance armor = mPlayer.getAttribute(Attribute.GENERIC_ARMOR);
		if (armor != null) {
			armor.setBaseValue(armor.getBaseValue() + mArmor);
			new BukkitRunnable() {
				@Override
				public void run() {
					armor.setBaseValue(armor.getBaseValue() - mArmor);
				}
			}.runTaskLater(mPlugin, BODYGUARD_BUFF_DURATION);
		}
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), BODYGUARD_RADIUS)) {
			MovementUtils.knockAway(mPlayer, mob, 0.45f);
			if (getAbilityScore() > 1) {
				EntityUtils.applyStun(mPlugin, BODYGUARD_STUN_DURATION, mob);
			}
		}
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(event, 1 - mDamageResistance));
		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(event, 1 - mDamageResistance));
		return true;
	}

}
