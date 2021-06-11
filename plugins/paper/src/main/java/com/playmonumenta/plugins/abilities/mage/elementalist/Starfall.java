package com.playmonumenta.plugins.abilities.mage.elementalist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.abilities.SpellPower;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;



public class Starfall extends Ability {
	public static final String NAME = "Starfall";
	public static final ClassAbility ABILITY = ClassAbility.STARFALL;

	public static final int DAMAGE_1 = 15;
	public static final int DAMAGE_2 = 27;
	public static final int SIZE = 5;
	public static final int DISTANCE = 25;
	public static final int FIRE_SECONDS = 3;
	public static final int FIRE_TICKS = FIRE_SECONDS * 20;
	public static final float KNOCKBACK = 0.7f;
	public static final int COOLDOWN_SECONDS = 18;
	public static final int COOLDOWN_TICKS = COOLDOWN_SECONDS * 20;

	private final int mLevelDamage;

	public Starfall(Plugin plugin, Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = NAME;
		mInfo.mShorthandName = "SF";
		mInfo.mDescriptions.add(
			String.format(
				"While holding a wand, pressing the swap key marks where you're looking, up to %s blocks away. You summon a falling meteor above the mark that lands strongly, dealing %s fire damage to all enemies in a %s-block cube around it, setting them on fire for %ss, and knocking them away. Swapping hands while holding a wand no longer does its vanilla function. Cooldown: %ss.",
				DISTANCE,
				DAMAGE_1,
				SIZE,
				FIRE_SECONDS,
				COOLDOWN_SECONDS
			)
		);
		mInfo.mDescriptions.add(
			String.format(
				"Damage is increased from %s to %s.",
				DAMAGE_1,
				DAMAGE_2
			)
		);
		mInfo.mCooldown = COOLDOWN_TICKS;
		mInfo.mIgnoreCooldown = true;

		mLevelDamage = getAbilityScore() == 2 ? DAMAGE_2 : DAMAGE_1;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (
			ItemUtils.isWand(
				mPlayer.getInventory().getItemInMainHand()
			)
		) {
			event.setCancelled(true);

			if (
				!isTimerActive()
				&& !mPlayer.isSneaking()
			) {
				putOnCooldown();

				Location loc = mPlayer.getEyeLocation();
				World world = mPlayer.getWorld();
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 0.85f);
				world.spawnParticle(Particle.LAVA, mPlayer.getLocation(), 15, 0.25f, 0.1f, 0.25f);
				world.spawnParticle(Particle.FLAME, mPlayer.getLocation(), 30, 0.25f, 0.1f, 0.25f, 0.15f);
				Vector dir = loc.getDirection().normalize();
				for (int i = 0; i < DISTANCE; i++) {
					loc.add(dir);

					mPlayer.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
					int size = EntityUtils.getNearbyMobs(loc, 2, mPlayer).size();
					if (loc.getBlock().getType().isSolid() || i >= 24 || size > 0) {
						launchMeteor(mPlayer, loc);
						break;
					}
				}
			}
		}
	}

	private void launchMeteor(final Player player, final Location loc) {
		Location ogLoc = loc.clone();
		loc.add(0, 40, 0);
		new BukkitRunnable() {
			double mT = 0;
			@Override
			public void run() {
				mT += 1;
				World world = mPlayer.getWorld();
				for (int i = 0; i < 8; i++) {
					loc.subtract(0, 0.25, 0);
					if (loc.getBlock().getType().isSolid()) {
						if (loc.getY() - ogLoc.getY() <= 2) {
							world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0);
							world.spawnParticle(Particle.FLAME, loc, 175, 0, 0, 0, 0.235F);
							world.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.2F);
							world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0, 0, 0, 0.2F);
							this.cancel();

							float damage = SpellPower.getSpellDamage(mPlayer, mLevelDamage);

							for (LivingEntity e : EntityUtils.getNearbyMobs(loc, SIZE, mPlayer)) {
								EntityUtils.applyFire(mPlugin, FIRE_TICKS, e, mPlayer);
								EntityUtils.damageEntity(mPlugin, e, damage, player, MagicType.FIRE, true, mInfo.mLinkedSpell);
								MovementUtils.knockAway(loc, e, KNOCKBACK);
							}
							break;
						}
					}
				}
				world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1);
				world.spawnParticle(Particle.FLAME, loc, 25, 0.25F, 0.25F, 0.25F, 0.1F);
				world.spawnParticle(Particle.SMOKE_LARGE, loc, 5, 0.25F, 0.25F, 0.25F, 0.1F);

				if (mT >= 50) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}
}