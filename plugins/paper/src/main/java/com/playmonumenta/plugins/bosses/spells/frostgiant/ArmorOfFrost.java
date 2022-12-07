package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/*
Armor of Frost - Once every 60 seconds or phase change
the boss gains a shield of permafrost
that prevents the boss from taking damage as long as
the shield is up. Players can break the shield
by positioning the boss underneath an icicle above and knocking it down to hit him.
 */
public class ArmorOfFrost extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final List<Player> mWarned = new ArrayList<Player>();
	//Gets frostArmorActive, which determines whether or not the permafrost armor is up
	private final FrostGiant mBossClass;
	private @Nullable
	BukkitRunnable mCooldown;
	private final int mMaxLevel;
	private int mLevel;
	//Whether or not the immune armor regenerates after 45 seconds
	//ONLY FALSE IF FINAL 10% PHASE
	private final boolean mRegen;

	public ArmorOfFrost(Plugin plugin, LivingEntity boss, FrostGiant giantClass, int max, boolean regen) {
		mPlugin = plugin;
		mBoss = boss;
		mBossClass = giantClass;
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
		}.runTaskTimer(mPlugin, 0, 20 * 5);
	}

	public ArmorOfFrost(Plugin plugin, LivingEntity boss, FrostGiant giantClass, int max) {
		this(plugin, boss, giantClass, max, true);
	}

	@Override
	public void run() {
		if (mBossClass.mFrostArmorActive) {
			runAnimation();
		}
	}

	/*
	 * Boss was damaged, check if permafrost armor is up or not
	 */
	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (mBossClass.mFrostArmorActive) {
			if (source instanceof Player player) {
				player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 3, 0);
				if (!mWarned.contains(player)) {
					player.sendMessage(Component.text("The armor absorbs the damage you dealt.", NamedTextColor.GOLD));
					mWarned.add(player);
				}

				mWarned.add(player);
			}
			event.setDamage(0.0001);
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
		World world = mBoss.getWorld();
		// For Icicle Crash advancement
		CommandUtils.runCommandViaConsole("function monumenta:frost_giant/fight/icicles_hit_count");

		if (mBossClass.mFrostArmorActive) {
			world.playSound(mBoss.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 3, 0);
			new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation(), 40, 0, 0, 0, 1).spawnAsEntityActive(mBoss);
			new PartialParticle(Particle.CRIT, mBoss.getLocation(), 40, 0, 0, 0, 1).spawnAsEntityActive(mBoss);
			new PartialParticle(Particle.BLOCK_CRACK, mBoss.getLocation(), 40, 0, 0, 0, 1, Bukkit.createBlockData(Material.ICE)).spawnAsEntityActive(mBoss);
			mLevel -= 1;
			if (mLevel <= 0) {
				new PartialParticle(Particle.SOUL_FIRE_FLAME, mBoss.getLocation().add(0, 4, 0), 50, 0.5, 0.5, 0.5, 0.3).spawnAsEntityActive(mBoss);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.HOSTILE, 3, 2);
				world.playSound(mBoss.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 3, 2);
				PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), FrostGiant.detectionRange, "tellraw @s [\"\",{\"text\":\"The icicle pierces the armor, shattering it.\",\"color\":\"aqua\"}]");
				mBossClass.mFrostArmorActive = false;
				if (mRegen) {
					runCooldown();
				}
				mLevel = mMaxLevel;
			} else if (mLevel == 1) {
				PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), FrostGiant.detectionRange, "tellraw @s [\"\",{\"text\":\"The armor begins to falter from the icicle barrage.\",\"color\":\"aqua\"}]");
			} else {
				PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), FrostGiant.detectionRange, "tellraw @s [\"\",{\"text\":\"The armor cracks from the icicle.\",\"color\":\"aqua\"}]");
			}
		} else {
			//If permafrost shield already down, do normal damage and reset countdown for permafrost armor
			mBoss.damage(70);
			mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 1));
			runCooldown();
		}
	}

	private void runCooldown() {
		//Re-run cooldown whenver called
		if (mCooldown != null) {
			mCooldown.cancel();
		}
		mCooldown = new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

				mBossClass.mFrostArmorActive = true;
				PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), FrostGiant.detectionRange, "tellraw @s [\"\",{\"text\":\"The armor reforms once again.\",\"color\":\"aqua\"}]");
				mWarned.clear();
				FrostGiant.changeArmorPhase(mBoss.getEquipment(), false);
			}
		};
		mCooldown.runTaskLater(mPlugin, 20 * 45);
		FrostGiant.changeArmorPhase(mBoss.getEquipment(), true);
	}

	private void runAnimation() {

		Location loc = mBoss.getLocation();
		Location tempLoc = loc.clone();

		for (int deg = 0; deg < 360; deg += 10) {
			if (FastUtils.RANDOM.nextDouble() > 0.4) {
				//Each level adds one ring up to level 3. At level 3, all three rings
				if (mLevel >= 1) {
					tempLoc.set(loc.getX(), loc.getY(), loc.getZ());
					new PartialParticle(Particle.SOUL_FIRE_FLAME, tempLoc.add(3 * FastUtils.cos(deg), 4, 3 * FastUtils.sin(deg)), 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
				}
				if (mLevel >= 2) {
					tempLoc.set(loc.getX(), loc.getY(), loc.getZ());
					new PartialParticle(Particle.SOUL_FIRE_FLAME, tempLoc.add(3 * FastUtils.cos(deg), 2, 3 * FastUtils.sin(deg)), 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
				}
				if (mLevel >= 3) {
					tempLoc.set(loc.getX(), loc.getY(), loc.getZ());
					new PartialParticle(Particle.SOUL_FIRE_FLAME, tempLoc.add(3 * FastUtils.cos(deg), 6, 3 * FastUtils.sin(deg)), 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
				}
			}
		}
	}
}
