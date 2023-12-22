package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.WeakHashMap;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RapidFire extends DepthsAbility {

	public static final String ABILITY_NAME = "Rapid Fire";
	public static final int[] ARROWS = {4, 5, 6, 7, 8, 10};
	public static final int DAMAGE = 10;
	public static final int COOLDOWN = 22 * 20;
	public static final String META_DATA_TAG = "RapidFireArrow";

	public static final String CHARM_COOLDOWN = "Rapid Fire Cooldown";

	public static final DepthsAbilityInfo<RapidFire> INFO =
		new DepthsAbilityInfo<>(RapidFire.class, ABILITY_NAME, RapidFire::new, DepthsTree.STEELSAGE, DepthsTrigger.PASSIVE)
			.linkedSpell(ClassAbility.RAPIDFIRE)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", RapidFire::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).fallThrough(),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(Material.REPEATER)
			.descriptions(RapidFire::getDescription)
			.priorityAmount(950); // Needs to trigger before a few things like Focused Combos since it cancels damage

	private final int mArrows;
	private final double mDamage;

	private final WeakHashMap<Projectile, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap;

	public RapidFire(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mArrows = ARROWS[mRarity - 1] + (int) CharmManager.getLevel(mPlayer, CharmEffects.RAPID_FIRE_ARROWS.mEffectName);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.RAPID_FIRE_DAMAGE.mEffectName, DAMAGE);
		mPlayerItemStatsMap = new WeakHashMap<>();
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		boolean encoreProjectileOverride = !ItemUtils.isProjectileWeapon(mPlayer.getInventory().getItemInMainHand());

		World world = mPlayer.getWorld();
		cancelOnDeath(new BukkitRunnable() {
			int mCount = 0;

			@Override
			public void run() {

				if (mCount >= mArrows) {
					this.cancel();
					return;
				}

				ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
				if (ItemUtils.isProjectileWeapon(inMainHand) || encoreProjectileOverride) {
					Location eyeLoc = mPlayer.getEyeLocation();
					Vector direction = mPlayer.getLocation().getDirection();
					AbstractArrow arrow = world.spawnArrow(eyeLoc, direction, 3.0f, 0, Arrow.class);
					arrow.setCritical(true);
					arrow.setPierceLevel(0);
					arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);

					arrow.setShooter(mPlayer);

					mPlayerItemStatsMap.put(arrow, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));
					arrow.setMetadata(META_DATA_TAG, new FixedMetadataValue(mPlugin, 0));

					mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.ASH);
					Location loc = mPlayer.getLocation().add(0, 1, 0);
					loc.getWorld().playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1, 0.65f);
					loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 0.45f);
					ProjectileLaunchEvent eventLaunch = new ProjectileLaunchEvent(arrow);
					Bukkit.getPluginManager().callEvent(eventLaunch);
					mCount++;
				} else {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 3));
		putOnCooldown();
		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getDamager() instanceof Projectile proj) {
			ItemStatManager.PlayerItemStats playerItemStats = mPlayerItemStatsMap.remove(proj);
			if (playerItemStats != null) {
				DamageUtils.damage(mPlayer, enemy, new DamageEvent.Metadata(DamageType.PROJECTILE_SKILL, mInfo.getLinkedSpell(), playerItemStats), mDamage, true, true, false);
				event.setCancelled(true);
				proj.remove();
			}
		}
		return false; // prevents multiple calls itself
	}

	private static Description<RapidFire> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<RapidFire>(color)
			.add("Left clicking with a projectile weapon shoots a flurry of ")
			.add(a -> a.mArrows, ARROWS[rarity - 1], false, null, true)
			.add(" projectiles in the direction that you are looking that deal ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE, false)
			.add(" projectile damage.")
			.addCooldown(COOLDOWN);
	}
}

