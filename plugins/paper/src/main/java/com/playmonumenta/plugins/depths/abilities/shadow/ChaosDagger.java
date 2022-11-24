package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ChaosDagger extends DepthsAbility {

	public static final String ABILITY_NAME = "Chaos Dagger";
	public static final int COOLDOWN = 22 * 20;
	public static final double[] DAMAGE = {2.0, 2.25, 2.5, 2.75, 3.0, 3.5};
	private static final double VELOCITY = 0.5;
	public static final int STUN_DURATION = 3 * 20;
	public static final int DAMAGE_DURATION = 5 * 20;
	private static final int TARGET_RADIUS = 20;
	private static final int ELITE_RADIUS = 5;
	private static final int STEALTH_DURATION = 30;

	public static final DepthsAbilityInfo<ChaosDagger> INFO =
		new DepthsAbilityInfo<>(ChaosDagger.class, ABILITY_NAME, ChaosDagger::new, DepthsTree.SHADOWDANCER, DepthsTrigger.SWAP)
			.linkedSpell(ClassAbility.CHAOS_DAGGER)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ChaosDagger::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP), HOLDING_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.ITEM_FRAME))
			.descriptions(ChaosDagger::getDescription, MAX_RARITY);

	private @Nullable Entity mHitMob;

	public ChaosDagger(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown() ||
			    EntityUtils.getNearestMob(mPlayer.getLocation(), 20.0) == null) {
			return;
		}
		putOnCooldown();
		mHitMob = null;

		Location loc = mPlayer.getEyeLocation();
		ItemStack itemDagger = new ItemStack(Material.NETHERITE_SWORD);
		ItemUtils.setPlainName(itemDagger, "Chaos Dagger");
		ItemMeta tinctureMeta = itemDagger.getItemMeta();
		tinctureMeta.displayName(Component.text("Chaos Dagger", NamedTextColor.WHITE)
			                         .decoration(TextDecoration.ITALIC, false));
		itemDagger.setItemMeta(tinctureMeta);
		World world = mPlayer.getWorld();
		Item dagger = world.dropItem(loc, itemDagger);
		dagger.setPickupDelay(Integer.MAX_VALUE);

		Vector vel = mPlayer.getEyeLocation().getDirection().normalize();
		vel.multiply(VELOCITY);

		dagger.setVelocity(vel);
		dagger.setGlowing(true);
		world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.4f, 2.5f);

		new BukkitRunnable() {

			int mExpire = 0;
			final World mWorld = mPlayer.getWorld();
			@Nullable LivingEntity mTarget = EntityUtils.getNearestMob(dagger.getLocation(), 20.0);
			@Nullable Location mLastLocation = null;

			@Override
			public void run() {
				mExpire++;
				if (mExpire >= 10 * 20) {
					dagger.remove();
					// Take the skill off cooldown (by setting to 0)
					mPlugin.mTimers.addCooldown(mPlayer, mInfo.getLinkedSpell(), 0);
					this.cancel();
				}
				Location tLoc = dagger.getLocation();

				List<LivingEntity> veryNearbyMobs = EntityUtils.getNearbyMobs(tLoc, ELITE_RADIUS);
				veryNearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
				veryNearbyMobs.removeIf(mob -> !(EntityUtils.isBoss(mob) || EntityUtils.isElite(mob)));
				mTarget = EntityUtils.getNearestMob(tLoc, veryNearbyMobs);

				if (mTarget == null) {
					List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(tLoc, TARGET_RADIUS);
					nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
					mTarget = EntityUtils.getNearestMob(tLoc, nearbyMobs);
				}

				if (mTarget == null) {
					dagger.remove();
					this.cancel();
					return;
				}

				if (mTarget.getBoundingBox().overlaps(dagger.getBoundingBox()) && !mTarget.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
					mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, tLoc, 30, 2, 0, 2);
					world.playSound(tLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.15f);
					mHitMob = mTarget;
					if (EntityUtils.isBoss(mTarget) || mTarget.getScoreboardTags().contains("boss_ccimmune")) {
						EntityUtils.applySlow(mPlugin, STUN_DURATION, 0.99f, mTarget);
					} else {
						EntityUtils.applyStun(mPlugin, STUN_DURATION, mTarget);
					}
					mTarget.setGlowing(true);

					new BukkitRunnable() {
						@Override
						public void run() {
							if (mHitMob != null && !(mHitMob instanceof MagmaCube && mHitMob.getName().contains("Gyrhaeddant"))) {
								mHitMob.setGlowing(false);
							}
							mHitMob = null;
							this.cancel();
						}
					}.runTaskLater(mPlugin, DAMAGE_DURATION);

					dagger.remove();
					this.cancel();
				} else {
					loc.getWorld().spawnParticle(Particle.SPELL_WITCH, tLoc, 5, 0.2, 0.2, 0.2, 0.65);

					Vector dir = tLoc.subtract(mTarget.getLocation().toVector().clone().add(new Vector(0, 0.5, 0))).toVector();

					if (dir.length() < 0.001) {
						/* If the direction magnitude is too small, escape, rather than divide by zero / infinity */
						dagger.remove();
						this.cancel();
						return;
					}

					dir = dir.normalize().multiply(VELOCITY * -1.0);

					if (mLastLocation != null && tLoc.distance(mLastLocation) < 0.05) {
						dir.setY(dir.getY() + 1.0);
					}

					dagger.setVelocity(dir);
					mLastLocation = tLoc;

				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (enemy == mHitMob && (event.getType() == DamageType.MELEE || event.getType() == DamageType.PROJECTILE)) {
			event.setDamage(event.getDamage() * DAMAGE[mRarity - 1]);
			mHitMob = null;
			if (!enemy.isInvisible()) {
				enemy.setGlowing(false);
			}
			new BukkitRunnable() {

				@Override
				public void run() {
					if (enemy.isDead() || enemy.getHealth() < 0) {
						AbilityUtils.applyStealth(mPlugin, mPlayer, STEALTH_DURATION);
					}
				}

			}.runTaskLater(mPlugin, 1);
		}
		return false; // only changes event damage, and also prevents multiple calls itself by clearing mHitMob
	}

	private static String getDescription(int rarity) {
		return "Swap hands to throw a cursed dagger that stuns an enemy for " + STUN_DURATION / 20 + " seconds (rooting bosses instead). The next instance of melee or projectile damage you deal to this mob within " + DAMAGE_DURATION / 20 + " seconds is multiplied by " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + ". If this damage kills the target, gain stealth for 1.5s. The dagger prioritizes nearby Elites and Bosses but can hit any mob in its path. Cooldown: " + COOLDOWN / 20 + "s.";
	}


}

