package com.playmonumenta.plugins.abilities.mage.elementalist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;


public class Starfall extends Ability {
	public static final String NAME = "Starfall";
	public static final ClassAbility ABILITY = ClassAbility.STARFALL;

	public static final int DAMAGE_1 = 13;
	public static final int DAMAGE_2 = 23;
	public static final int SIZE = 5;
	public static final int DISTANCE = 25;
	public static final int FIRE_SECONDS = 3;
	public static final int FIRE_TICKS = FIRE_SECONDS * 20;
	public static final float KNOCKBACK = 0.7f;
	public static final int COOLDOWN_SECONDS = 18;
	public static final int COOLDOWN_TICKS = COOLDOWN_SECONDS * 20;

	private final int mLevelDamage;

	public Starfall(Plugin plugin, @Nullable Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = NAME;
		mInfo.mShorthandName = "SF";
		mInfo.mDescriptions.add(
			String.format(
				"While holding a wand, pressing the swap key marks where you're looking, up to %s blocks away. You summon a falling meteor above the mark that lands strongly, dealing %s magic damage to all enemies in a %s-block cube around it, setting them on fire for %ss, and knocking them away. Cooldown: %ss.",
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
		mDisplayItem = new ItemStack(Material.MAGMA_BLOCK, 1);

		mLevelDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
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

				ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
				float damage = SpellPower.getSpellDamage(mPlugin, mPlayer, mLevelDamage);

				world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 0.85f);
				new PartialParticle(Particle.LAVA, mPlayer.getLocation(), 15, 0.25f, 0.1f, 0.25f).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.FLAME, mPlayer.getLocation(), 30, 0.25f, 0.1f, 0.25f, 0.15f).spawnAsPlayerActive(mPlayer);
				Vector dir = loc.getDirection().normalize();
				for (int i = 0; i < DISTANCE; i++) {
					loc.add(dir);

					new PartialParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer);
					int size = EntityUtils.getNearbyMobs(loc, 2, mPlayer).size();
					if (!loc.isChunkLoaded() || loc.getBlock().getType().isSolid() || i >= 24 || size > 0) {
						launchMeteor(loc, playerItemStats, damage);
						break;
					}
				}
			}
		}
	}

	private void launchMeteor(final Location loc, final ItemStatManager.PlayerItemStats playerItemStats, final float damage) {
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
					if (!loc.isChunkLoaded() || loc.getBlock().getType().isSolid()) {
						if (loc.getY() - ogLoc.getY() <= 2) {
							world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0);
							new PartialParticle(Particle.FLAME, loc, 175, 0, 0, 0, 0.235F).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.2F).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0, 0, 0, 0.2F).spawnAsPlayerActive(mPlayer);
							this.cancel();

							for (LivingEntity e : EntityUtils.getNearbyMobs(loc, SIZE, mPlayer)) {
								EntityUtils.applyFire(mPlugin, FIRE_TICKS, e, mPlayer, playerItemStats);
								DamageUtils.damage(mPlayer, e, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.mLinkedSpell, playerItemStats), damage, true, true, false);
								MovementUtils.knockAway(loc, e, KNOCKBACK, true);
							}
							break;
						}
					}
				}
				world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1);
				new PartialParticle(Particle.FLAME, loc, 25, 0.25F, 0.25F, 0.25F, 0.1F).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SMOKE_LARGE, loc, 5, 0.25F, 0.25F, 0.25F, 0.1F).spawnAsPlayerActive(mPlayer);

				if (mT >= 50) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}
}
