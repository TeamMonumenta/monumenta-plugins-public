package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

/*
Armor of Frost - Once every 60 seconds or on phase change the boss gains permafrost armor that prevents the boss
from taking damage as long as the shield is up. Players can break the shield by positioning the boss underneath an
icicle above and knocking it down to hit him.
 */
public final class ArmorOfFrost extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final List<UUID> mWarned = new ArrayList<>();
	private final FrostGiant mFrostGiant;
	private @Nullable BukkitRunnable mCooldown;
	private final int mMaxLevel;
	private final boolean mRegen;
	private int mLevel;

	public ArmorOfFrost(final Plugin plugin, final FrostGiant frostGiant, final int max, final boolean regen) {
		mPlugin = plugin;
		mFrostGiant = frostGiant;
		mBoss = mFrostGiant.mBoss;
		mRegen = regen;
		mMaxLevel = max;
		mLevel = mMaxLevel;

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				mWarned.clear();
			}
		}.runTaskTimer(mPlugin, 0, Constants.TICKS_PER_SECOND * 20);
	}

	@Override
	public void run() {
		if (mFrostGiant.mFrostArmorActive && !mFrostGiant.getArenaParticipants().isEmpty()) {
			runAnimation();
		}
	}

	/* Boss was damaged, check if permafrost armor is up or not */
	@Override
	public void onHurtByEntityWithSource(final DamageEvent event, final Entity damager, final LivingEntity source) {
		if (mFrostGiant.mFrostArmorActive) {
			event.setFlatDamage(0.0001);
			if (source instanceof final Player player) {
				player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 2.0f, 0.5f);
				if (!mWarned.contains(player.getUniqueId())) {
					player.sendMessage(Component.text("The armor absorbs the damage you dealt.", NamedTextColor.AQUA));
					mWarned.add(player.getUniqueId());
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	//Cancels the cooldown so it does not interfere later on
	public void stopSkill() {
		if (mCooldown != null) {
			mCooldown.cancel();
		}
	}

	//Set armor from icicle hit
	public void hitByIcicle() {
		final World world = mBoss.getWorld();
		// For Icicle Crash advancement
		NmsUtils.getVersionAdapter().runConsoleCommandSilently("function monumenta:frost_giant/fight/icicles_hit_count");

		if (mFrostGiant.mFrostArmorActive) {
			world.playSound(mBoss.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 3, 0);
			new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation(), 40, 0, 0, 0, 1).spawnAsEntityActive(mBoss);
			new PartialParticle(Particle.CRIT, mBoss.getLocation(), 40, 0, 0, 0, 1).spawnAsEntityActive(mBoss);
			new PartialParticle(Particle.BLOCK_CRACK, mBoss.getLocation(), 40, 0, 0, 0, 1, Bukkit.createBlockData(Material.ICE)).spawnAsEntityActive(mBoss);
			mLevel--;

			switch (mLevel) {
				case 2 -> mFrostGiant.sendDialogue("The armor cracks from the icicle.", NamedTextColor.AQUA, false);
				case 1 -> mFrostGiant.sendDialogue("The armor begins to falter from the icicle barrage.", NamedTextColor.AQUA, false);
				default -> {
					new PartialParticle(Particle.SOUL_FIRE_FLAME, mBoss.getLocation().add(0, 4, 0), 50, 0.5, 0.5, 0.5, 0.3).spawnAsEntityActive(mBoss);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.HOSTILE, 3, 2);
					world.playSound(mBoss.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 3, 2);
					mFrostGiant.sendDialogue("The icicle pierces the armor, shattering it.", NamedTextColor.AQUA, false);
					mFrostGiant.mFrostArmorActive = false;
					if (mRegen) {
						runCooldown();
					}
					mLevel = mMaxLevel;
				}
			}
		} else {
			//If permafrost shield already down, do normal damage and reset countdown for permafrost armor
			DamageUtils.damage(null, mBoss, DamageEvent.DamageType.TRUE, 70);
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, BaseMovementSpeedModifyEffect.GENERIC_NAME,
				new BaseMovementSpeedModifyEffect(Constants.TICKS_PER_SECOND * 3, -0.3));
			runCooldown();
		}
	}

	private void runCooldown() {
		//Re-run cooldown whenever called
		if (mCooldown != null) {
			mCooldown.cancel();
		}
		mCooldown = new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

				mFrostGiant.mFrostArmorActive = true;
				mFrostGiant.sendDialogue("The armor reforms once again.", NamedTextColor.AQUA, false);
				mWarned.clear();
				mFrostGiant.changeArmorPhase(mBoss.getEquipment(), false);
			}
		};
		mCooldown.runTaskLater(mPlugin, Constants.TICKS_PER_SECOND * 45);
		mFrostGiant.changeArmorPhase(mBoss.getEquipment(), true);
	}

	private void runAnimation() {
		final double bossCurrentY = mBoss.getLocation().getY();
		final Location particleLoc = mBoss.getLocation();
		final PPCircle armorCircle = new PPCircle(Particle.SOUL_FIRE_FLAME, particleLoc, 3.0).count(12);

		if (mLevel >= 1) {
			particleLoc.setY(bossCurrentY + 3);
			armorCircle.location(particleLoc).spawnAsEntityActive(mBoss);
		}
		if (mLevel >= 2) {
			particleLoc.setY(bossCurrentY + 5);
			armorCircle.location(particleLoc).spawnAsEntityActive(mBoss);
		}
		if (mLevel >= 3) {
			particleLoc.setY(bossCurrentY + 7);
			armorCircle.location(particleLoc).spawnAsEntityActive(mBoss);
		}
	}
}
