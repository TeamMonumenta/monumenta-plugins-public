package com.playmonumenta.plugins.abilities.scout.hunter;

import java.util.Iterator;
import java.util.List;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.scout.BowMastery;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;



public class EnchantedShot extends Ability {

	private static final int ENCHANTED_1_DAMAGE = 25;
	private static final int ENCHANTED_2_DAMAGE = 40;
	private static final int ENCHANTED_1_COOLDOWN = 20 * 20;
	private static final int ENCHANTED_2_COOLDOWN = 20 * 16;
	private static final Particle.DustOptions ENCHANTED_ARROW_COLOR = new Particle.DustOptions(Color.fromRGB(225, 255, 219), 2.0f);
	private static final Particle.DustOptions ENCHANTED_ARROW_FRINGE_COLOR = new Particle.DustOptions(Color.fromRGB(168, 255, 252), 2.0f);

	private final int mDamage;

	private boolean mActive = false;

	public EnchantedShot(Plugin plugin, Player player) {
		super(plugin, player, "Enchanted Arrow");
		mInfo.mScoreboardId = "EnchantedArrow";
		mInfo.mShorthandName = "EA";
		mInfo.mDescriptions.add("Left-clicking with a bow while not sneaking, will prime an enchanted arrow that unprimes after 5 seconds. When you fire a critical arrow, it will instantaneously travel in a straight line for up to 30 blocks or until it hits a block. All targets hit take 25 damage, affected by Bow Mastery and Sharpshooter. Hit targets contribute to Sharpshooter stacks. Cooldown: 20s.");
		mInfo.mDescriptions.add("Every enemy hit takes 40 damage instead. Cooldown: 16s.");
		mInfo.mLinkedSpell = ClassAbility.ENCHANTED_ARROW;
		mInfo.mCooldown = getAbilityScore() == 1 ? ENCHANTED_1_COOLDOWN : ENCHANTED_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;

		mDamage = getAbilityScore() == 1 ? ENCHANTED_1_DAMAGE : ENCHANTED_2_DAMAGE;
	}

	@Override
	public void cast(Action action) {
		if (!mActive && !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), ClassAbility.ENCHANTED_ARROW)) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (ItemUtils.isSomeBow(mainHand)) {
				Player player = mPlayer;
				mActive = true;
				World world = mPlayer.getWorld();
				world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1.45f);
				new BukkitRunnable() {
					int mTicks = 0;
					@Override
					public void run() {
						mTicks++;
						world.spawnParticle(Particle.SPELL_INSTANT, player.getLocation(), 4, 0.25, 0, 0.25, 0);
						if (!mActive || mTicks >= 20 * 5) {
							mActive = false;
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}
		}
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (mActive && arrow.isCritical()) {
			arrow.remove();
			mPlugin.mProjectileEffectTimers.removeEntity(arrow);
			mActive = false;
			BoundingBox box = BoundingBox.of(mPlayer.getEyeLocation(), 1.5, 1.5, 1.5);

			Player player = mPlayer;
			Location loc = player.getEyeLocation();
			Vector dir = loc.getDirection().normalize();
			World world = mPlayer.getWorld();
			world.playSound(loc, Sound.ENTITY_PUFFER_FISH_DEATH, 0.7f, 0.8f);
			world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, 1f, 1.5f);
			world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.75f);
			world.playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 1f);
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 1f, 0.75f);
			world.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(dir), 20, 0.2, 0.2, 0.2, 0.15);
			world.spawnParticle(Particle.CRIT_MAGIC, loc.clone().add(dir), 10, 0.2, 0.2, 0.2, 0.15);

			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getEyeLocation(), 30, mPlayer);

			double damage = mDamage * BowMastery.getDamageMultiplier(mPlayer) * Sharpshooter.getDamageMultiplier(mPlayer);
			int hits = 0;

			Location pLoc = mPlayer.getEyeLocation();
			pLoc.setPitch(pLoc.getPitch() + 90);
			Vector pVec = new Vector(pLoc.getDirection().getX(), pLoc.getDirection().getY(), pLoc.getDirection().getZ());
			pVec = pVec.normalize();

			for (int i = 0; i < 30; i++) {
				box.shift(dir);
				Location bLoc = box.getCenter().toLocation(world);
				world.spawnParticle(Particle.SMOKE_NORMAL, bLoc, 5, 0.1, 0.1, 0.1, 0.2);
				world.spawnParticle(Particle.FALLING_DUST, bLoc, 5, 0.1, 0.1, 0.1, 0, Bukkit.createBlockData("light_gray_glazed_terracotta"));
				world.spawnParticle(Particle.SPELL_INSTANT, bLoc, 6, 0.2, 0.2, 0.2, 0.15);
				world.spawnParticle(Particle.SMOKE_NORMAL, bLoc, 3, 0.1, 0.1, 0.1, 0);
				world.spawnParticle(Particle.REDSTONE, bLoc, 3, 0.1, 0.1, 0.1, 0, ENCHANTED_ARROW_COLOR);
				world.spawnParticle(Particle.CRIT, bLoc, 5, 0.15, 0.15, 0.15, 0.4);
				world.spawnParticle(Particle.REDSTONE, bLoc.clone().add(pVec), 1, 0, 0, 0, 0, ENCHANTED_ARROW_FRINGE_COLOR);
				world.spawnParticle(Particle.END_ROD, bLoc.clone().add(pVec), 1, 0, 0, 0, 0.02);
				world.spawnParticle(Particle.REDSTONE, bLoc.clone().add(dir.clone().multiply(0.5)).add(pVec.clone().rotateAroundAxis(dir, Math.PI / 12)), 1, 0, 0, 0, 0, ENCHANTED_ARROW_FRINGE_COLOR);
				Iterator<LivingEntity> iterator = mobs.iterator();
				while (iterator.hasNext()) {
					LivingEntity mob = iterator.next();
					if (mob.getBoundingBox().overlaps(box)) {
						world.spawnParticle(Particle.CRIT_MAGIC, mob.getLocation().add(0, 1, 0), 15, 0.1, 0.2, 0.1, 0.15);
						world.spawnParticle(Particle.SPELL_INSTANT, mob.getLocation().add(0, 1, 0), 20, 0.1, 0.2, 0.1, 0.15);
						world.spawnParticle(Particle.FIREWORKS_SPARK, mob.getLocation().add(0, 1, 0), 10, 0.1, 0.2, 0.1, 0.1);
						EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer, MagicType.ARCANE, true, mInfo.mLinkedSpell);
						/* Prevent mob from being hit twice in one shot */
						iterator.remove();
						hits++;
					}
				}
				if (bLoc.getBlock().getType().isSolid()) {
					world.spawnParticle(Particle.SMOKE_LARGE, bLoc, 80, 0.1, 0.1, 0.1, 0.2);
					world.spawnParticle(Particle.CLOUD, bLoc, 80, 0.1, 0.1, 0.1, 0.2);
					world.spawnParticle(Particle.FIREWORKS_SPARK, bLoc, 50, 0.1, 0.1, 0.1, 0.3);
					world.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.85f);
					break;
				}

				pVec.rotateAroundAxis(dir, Math.PI / 6);
			}

			Sharpshooter.addStacks(mPlugin, mPlayer, hits);
			putOnCooldown();
			return false;
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		return !mPlayer.isSneaking();
	}
}