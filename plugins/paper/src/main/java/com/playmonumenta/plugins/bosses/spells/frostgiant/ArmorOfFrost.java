package com.playmonumenta.plugins.bosses.spells.frostgiant;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
Armor of Frost - Once every 60 seconds or phase change
the boss gains a shield of permafrost
that prevents the boss from taking damage as long as
the shield is up. Players can break the shield
by positioning the boss underneath an icicle above and knocking it down to hit him.
 */
public class ArmorOfFrost extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private List<Player> mWarned = new ArrayList<Player>();
	//Gets frostArmorActive, which determines whether or not the permafrost armor is up
	private FrostGiant mBossClass;
	private BukkitRunnable mCooldown;
	private int mMaxLevel;
	private int mLevel;
	//Whether or not the immune armor regenerates after 45 seconds
	//ONLY FALSE IF FINAL 10% PHASE
	private boolean mRegen;

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
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (mBossClass.mFrostArmorActive) {
			if (event.getDamager() instanceof Player) {

				//Debug purposes
				ItemStack item = ((Player)event.getDamager()).getInventory().getItemInMainHand();
				if (item != null && ItemUtils.getPlainName(item).equals("Frost Giant Damager")) {
					return;
				}

				Player player = (Player) event.getDamager();
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
	public void bossHitByProjectile(ProjectileHitEvent event) {
		if (mBossClass.mFrostArmorActive) {
			if (event.getEntity().getShooter() instanceof Player) {
				Player player = (Player) event.getEntity().getShooter();
				player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 3, 0);
				if (!mWarned.contains(player)) {
					player.sendMessage(Component.text("The armor absorbs the damage you dealt.", NamedTextColor.GOLD));
					mWarned.add(player);
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
		World world = mBoss.getWorld();

		if (mBossClass.mFrostArmorActive) {
			world.playSound(mBoss.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 3, 0);
			world.spawnParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation(), 40, 0, 0, 0, 1);
			world.spawnParticle(Particle.CRIT, mBoss.getLocation(), 40, 0, 0, 0, 1);
			world.spawnParticle(Particle.BLOCK_CRACK, mBoss.getLocation(), 40, 0, 0, 0, 1, Bukkit.createBlockData(Material.ICE));
			mLevel -= 1;
			if (mLevel <= 0) {
				world.spawnParticle(Particle.SOUL_FIRE_FLAME, mBoss.getLocation().add(0, 4, 0), 50, 0.5, 0.5, 0.5, 0.3);
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
			mBoss.damage(40);
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
//				FrostGiant.changeArmorPhase(mBoss.getEquipment(), false);
			}
		};
		mCooldown.runTaskLater(mPlugin, 20 * 45);
//		FrostGiant.changeArmorPhase(mBoss.getEquipment(), true);
	}

	private void runAnimation() {

		World world = mBoss.getWorld();
		Location loc = mBoss.getLocation();
		Location tempLoc = loc.clone();

		for (int deg = 0; deg < 360; deg += 10) {
			if (FastUtils.RANDOM.nextDouble() > 0.4) {
				//Each level adds one ring up to level 3. At level 3, all three rings
				if (mLevel >= 1) {
					tempLoc.set(loc.getX(), loc.getY(), loc.getZ());
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, tempLoc.add(3 * FastUtils.cos(deg), 4, 3 * FastUtils.sin(deg)), 1, 0, 0, 0, 0);
				}
				if (mLevel >= 2) {
					tempLoc.set(loc.getX(), loc.getY(), loc.getZ());
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, tempLoc.add(3 * FastUtils.cos(deg), 2, 3 * FastUtils.sin(deg)), 1, 0, 0, 0, 0);
				}
				if (mLevel >= 3) {
					tempLoc.set(loc.getX(), loc.getY(), loc.getZ());
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, tempLoc.add(3 * FastUtils.cos(deg), 6, 3 * FastUtils.sin(deg)), 1, 0, 0, 0, 0);
				}
			}
		}
	}
}
