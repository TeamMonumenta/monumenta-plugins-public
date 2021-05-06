package com.playmonumenta.plugins.abilities.rogue;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.enchantments.BaseAbilityEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.point.Raycast;
import com.playmonumenta.plugins.point.RaycastData;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.abilities.rogue.swordsage.BladeDance;

public class AdvancingShadows extends Ability {

	public static class AdvancingShadowsRadiusEnchantment extends BaseAbilityEnchantment {
		public AdvancingShadowsRadiusEnchantment() {
			super("Advancing Shadows Range", EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR));
		}
	}

	public static class AdvancingShadowsKnockbackRadiusEnchantment extends BaseAbilityEnchantment {
		public AdvancingShadowsKnockbackRadiusEnchantment() {
			super("Advancing Shadows Knockback Range", EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR));
		}

		private static float getKnockbackRadius(Player player, float base) {
			int level = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, AdvancingShadowsKnockbackRadiusEnchantment.class);
			return base * (float) ((level / 100.0) + 1);
		}
	}

	public static class AdvancingShadowsKnockbackSpeedEnchantment extends BaseAbilityEnchantment {
		public AdvancingShadowsKnockbackSpeedEnchantment() {
			super("Advancing Shadows Knockback Speed", EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR));
		}

		private static float getKnockbackSpeed(Player player, float base) {
			int level = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, AdvancingShadowsKnockbackSpeedEnchantment.class);
			return base * (float) ((level / 100.0) + 1);
		}
	}

	public static class AdvancingShadowsCooldownEnchantment extends BaseAbilityEnchantment {
		public AdvancingShadowsCooldownEnchantment() {
			super("Advancing Shadows Cooldown", EnumSet.of(ItemSlot.OFFHAND));
		}
	}

	private static final int ADVANCING_SHADOWS_RANGE_1 = 11;
	private static final int ADVANCING_SHADOWS_RANGE_2 = 16;
	private static final float ADVANCING_SHADOWS_AOE_KNOCKBACKS_SPEED = 0.5f;
	private static final float ADVANCING_SHADOWS_AOE_KNOCKBACKS_RANGE = 4;
	private static final double ADVANCING_SHADOWS_OFFSET = 2.7;
	private static final int DURATION = 5 * 20;
	private static final int EFFECT_LEVEL = 1;
	private static final int ADVANCING_SHADOWS_COOLDOWN = 20 * 20;

	private LivingEntity mTarget = null;
	private BladeDance mBladeDance;

	public AdvancingShadows(Plugin plugin, Player player) {
		super(plugin, player, "Advancing Shadows");
		mInfo.mLinkedSpell = Spells.ADVANCING_SHADOWS;
		mInfo.mScoreboardId = "AdvancingShadows";
		mInfo.mCooldown = ADVANCING_SHADOWS_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mDescriptions.add("While holding two swords and not sneaking, right click to teleport to the target hostile enemy within 10 blocks and gain strength 2 for 5 seconds. Cooldown: 20s.");
		mInfo.mDescriptions.add("Teleport range is increased to 15 blocks and all hostile non-target mobs within 4 blocks are knocked away from the target.");
		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player != null) {
				mBladeDance = AbilityManager.getManager().getPlayerAbility(mPlayer, BladeDance.class);
			}
		});
	}

	@Override
	public void cast(Action action) {

		mInfo.mCooldown = (int) AdvancingShadowsCooldownEnchantment.getCooldown(mPlayer, ADVANCING_SHADOWS_COOLDOWN, AdvancingShadowsCooldownEnchantment.class);

		LivingEntity entity = mTarget;
		if (entity != null) {
			int advancingShadows = getAbilityScore();
			Vector dir = LocationUtils.getDirectionTo(entity.getLocation(), mPlayer.getLocation());
			World world = mPlayer.getWorld();
			Location loc = mPlayer.getLocation();
			while (loc.distance(entity.getLocation()) > ADVANCING_SHADOWS_OFFSET) {
				loc.add(dir.clone().multiply(0.3333));
				world.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 4, 0.3, 0.5, 0.3, 1.0);
				world.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.025);
				if (loc.distance(entity.getLocation()) < ADVANCING_SHADOWS_OFFSET) {
					double multiplier = ADVANCING_SHADOWS_OFFSET - loc.distance(entity.getLocation());
					loc.subtract(dir.clone().multiply(multiplier));
					break;
				}
			}
			loc.add(0, 1, 0);

			// Just in case the player's teleportation loc is in a block.
			while (loc.getBlock().getType().isSolid()) {
				loc.subtract(dir.clone().multiply(1.15));
			}

			// Prevent the player from teleporting over void
			if (loc.getY() < 8) {
				boolean safe = false;
				for (int y = 0; y < loc.getY() - 1; y++) {
					Location tempLoc = loc.clone();
					tempLoc.setY(y);
					if (!tempLoc.getBlock().isPassable()) {
						safe = true;
						break;
					}
				}

				// Maybe void - not worth it
				if (!safe) {
					world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.8f);
					return;
				}

				// Don't teleport players below y = 1.1 to avoid clipping into oblivion
				loc.setY(Math.max(1.1, loc.getY()));
			}

			world.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1.1, 0), 50, 0.35, 0.5, 0.35, 1.0);
			world.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().add(0, 1.1, 0), 12, 0.35, 0.5, 0.35, 0.05);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.1f);

			mPlayer.teleport(loc, TeleportCause.UNKNOWN);

			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, DURATION, EFFECT_LEVEL, true, false));
			float range = AdvancingShadowsKnockbackRadiusEnchantment.getKnockbackRadius(mPlayer, ADVANCING_SHADOWS_AOE_KNOCKBACKS_RANGE);
			float speed = AdvancingShadowsKnockbackSpeedEnchantment.getKnockbackSpeed(mPlayer, ADVANCING_SHADOWS_AOE_KNOCKBACKS_SPEED);
			if (advancingShadows > 1) {
				for (LivingEntity mob : EntityUtils.getNearbyMobs(entity.getLocation(),
				                                                  range, mPlayer)) {
					if (mob != entity) {
						MovementUtils.knockAway(entity, mob, speed);
					}
				}
			}

			world.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1.1, 0), 50, 0.35, 0.5, 0.35, 1.0);
			world.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().add(0, 1.1, 0), 12, 0.35, 0.5, 0.35, 0.05);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.1f);
			mTarget = null;
			putOnCooldown();
		}
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		if (InventoryUtils.rogueTriggerCheck(mainHand, offHand)) {
			if (!mPlayer.isSneaking()) {
				// *TO DO* - Turn into boolean in constructor -or- look at changing trigger entirely
				if (mBladeDance != null && mPlayer.getLocation().getPitch() >= 50) {
					return false;
				}
				int advancingShadows = getAbilityScore();
				int range = (advancingShadows == 1) ? ADVANCING_SHADOWS_RANGE_1 : ADVANCING_SHADOWS_RANGE_2;
				range = (int) AdvancingShadowsRadiusEnchantment.getRadius(mPlayer, range, AdvancingShadowsRadiusEnchantment.class);

				// Basically makes sure if the target is in LoS and if there is
				// a path.
				Location eyeLoc = mPlayer.getEyeLocation();
				Raycast ray = new Raycast(eyeLoc, eyeLoc.getDirection(), range);
				ray.mThroughBlocks = false;
				ray.mThroughNonOccluding = false;
				if (AbilityManager.getManager().isPvPEnabled(mPlayer)) {
					ray.mTargetPlayers = true;
				}

				RaycastData data = ray.shootRaycast();

				List<LivingEntity> rayEntities = data.getEntities();
				if (rayEntities != null && !rayEntities.isEmpty()) {
					for (LivingEntity t : rayEntities) {
						if (!t.getUniqueId().equals(mPlayer.getUniqueId()) && t.isValid() && !t.isDead() && EntityUtils.isHostileMob(t)) {
							mTarget = t;
							return true;
						}
					}
				}
			}
		}
		return false;
	}

}
