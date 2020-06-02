package com.playmonumenta.plugins.abilities.mage.elementalist;

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
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;

public class Starfall extends Ability {
	private static final int STARFALL_PRIMED_TICKS = 20 * 10;
	private static final double STARFALL_ANGLE = 70.0;

	private static final int STARFALL_COOLDOWN = 20 * 18;
	private static final int STARFALL_1_DAMAGE = 20;
	private static final int STARFALL_2_DAMAGE = 36;
	private static final int STARFALL_FIRE_DURATION = 20 * 3;
	private static final double STARFALL_RADIUS = 5;

	private final int mDamage;

	/* The player's getTicksLived() when the skill was last primed or cast */
	private int mPrimedTick = -1;

	public Starfall(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Starfall");
		mInfo.linkedSpell = Spells.STARFALL;
		mInfo.scoreboardId = "Starfall";
		mInfo.mShorthandName = "SF";
		mInfo.mDescriptions.add("Right click while looking up to prime the next right click within 10s to summon a meteor where the player is looking (up to 25 blocks). It deals 20 damage in a 5 block radius and sets enemies on fire for 3 seconds. This spell can trigger SpellShock and will not trigger Mana Lance. Cooldown: 18s.");
		mInfo.mDescriptions.add("Damage is increased to 36.");
		mInfo.cooldown = STARFALL_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
		mDamage = getAbilityScore() == 1 ? STARFALL_1_DAMAGE : STARFALL_2_DAMAGE;
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		return InventoryUtils.isWandItem(mHand);
	}

	public boolean shouldCancelManaLance() {
		// If cast or primed this tick, don't run mana lance
		return mPrimedTick == mPlayer.getTicksLived();
	}

	@Override
	public void cast(Action action) {
		int ticksSincePrimed = mPlayer.getTicksLived() - mPrimedTick;
		boolean lookingUp = mPlayer.getLocation().getPitch() < -STARFALL_ANGLE;
		if (ticksSincePrimed > STARFALL_PRIMED_TICKS || ticksSincePrimed < 0 || lookingUp) {
			if (lookingUp) {
				// Looking up - prime starfall
				mPrimedTick = mPlayer.getTicksLived();
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1.1f);
				MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Starfall primed!");
			}

			// Either just primed or wasn't primed to begin with - abort
			return;
		}

		// Set this again when cast - this allows easy testing for whether Mana Lance should trigger
		mPrimedTick = mPlayer.getTicksLived();

		Location loc = mPlayer.getEyeLocation();
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 0.85f);
		mWorld.spawnParticle(Particle.LAVA, mPlayer.getLocation(), 15, 0.25f, 0.1f, 0.25f);
		mWorld.spawnParticle(Particle.FLAME, mPlayer.getLocation(), 30, 0.25f, 0.1f, 0.25f, 0.15f);
		Vector dir = loc.getDirection().normalize();
		for (int i = 0; i < 25; i++) {
			loc.add(dir);

			mPlayer.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
			int size = EntityUtils.getNearbyMobs(loc, 2, mPlayer).size();
			if (loc.getBlock().getType().isSolid() || i >= 24 || size > 0) {
				launchMeteor(mPlayer, loc);
				break;
			}
		}

		putOnCooldown();
	}

	private void launchMeteor(final Player player, final Location loc) {
		Location ogLoc = loc.clone();
		loc.add(0, 40, 0);
		new BukkitRunnable() {
			double mT = 0;
			@Override
			public void run() {
				mT += 1;
				for (int i = 0; i < 8; i++) {
					loc.subtract(0, 0.25, 0);
					if (loc.getBlock().getType().isSolid()) {
						if (loc.getY() - ogLoc.getY() <= 2) {
							loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0);
							mWorld.spawnParticle(Particle.FLAME, loc, 175, 0, 0, 0, 0.235F);
							mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.2F);
							mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0, 0, 0, 0.2F);
							this.cancel();

							for (LivingEntity e : EntityUtils.getNearbyMobs(loc, STARFALL_RADIUS, mPlayer)) {
								if (e instanceof Player) {
									EntityUtils.damageEntity(mPlugin, e, mDamage * 0.75, player, MagicType.FIRE, true, mInfo.linkedSpell);
								} else {
									EntityUtils.damageEntity(mPlugin, e, mDamage, player, MagicType.FIRE, true, mInfo.linkedSpell);
								}
								EntityUtils.applyFire(mPlugin, STARFALL_FIRE_DURATION, e, mPlayer);

								Vector v = e.getLocation().toVector().subtract(loc.toVector()).normalize();
								v.add(new Vector(0, 0.2, 0));
								e.setVelocity(v);
							}
							break;
						}
					}
				}
				loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1);
				mWorld.spawnParticle(Particle.FLAME, loc, 25, 0.25F, 0.25F, 0.25F, 0.1F);
				mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 5, 0.25F, 0.25F, 0.25F, 0.1F);

				if (mT >= 50) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}
}
