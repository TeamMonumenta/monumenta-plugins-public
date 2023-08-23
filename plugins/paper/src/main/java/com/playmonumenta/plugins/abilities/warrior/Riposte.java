package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.RiposteCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class Riposte extends Ability implements AbilityWithDuration {

	private static final int RIPOSTE_1_COOLDOWN = 15 * 20;
	private static final int RIPOSTE_2_COOLDOWN = 12 * 20;
	private static final int RIPOSTE_SWORD_DURATION = 2 * 20;
	private static final int RIPOSTE_AXE_DURATION = 3 * 20;
	private static final float RIPOSTE_KNOCKBACK_SPEED = 0.15f;
	private static final double RIPOSTE_SWORD_BONUS_DAMAGE = 1;
	private static final double ENHANCEMENT_DAMAGE = 15;
	private static final double ENHANCEMENT_RADIUS = 4;
	private static final int ENHANCEMENT_ROOT_DURATION = 30;

	public static final String CHARM_COOLDOWN = "Riposte Cooldown";
	public static final String CHARM_DAMAGE_DURATION = "Riposte Sword Bonus Damage Duration";
	public static final String CHARM_STUN_DURATION = "Riposte Stun Duration";
	public static final String CHARM_KNOCKBACK = "Riposte Knockback";
	public static final String CHARM_BONUS_DAMAGE = "Riposte Sword Bonus Damage";
	public static final String CHARM_DAMAGE = "Riposte Damage";
	public static final String CHARM_RADIUS = "Riposte Range";
	public static final String CHARM_ROOT_DURATION = "Riposte Root Duration";

	private final RiposteCS mCosmetic;

	public static final AbilityInfo<Riposte> INFO =
			new AbilityInfo<>(Riposte.class, "Riposte", Riposte::new)
					.linkedSpell(ClassAbility.RIPOSTE)
					.scoreboardId("Obliteration")
					.shorthandName("Rip")
					.descriptions(
							"While wielding a sword or axe, you block a melee attack that would have hit you. Cooldown: %ss."
									.formatted(StringUtils.ticksToSeconds(RIPOSTE_1_COOLDOWN)),
							("Cooldown lowered to %ss and if you block an attack with Riposte's effect while holding a sword, your next sword attack within %ss deals double damage. " +
									"If you block with Riposte's effect while holding an axe, the attacking mob is stunned for %ss.")
									.formatted(StringUtils.ticksToSeconds(RIPOSTE_2_COOLDOWN), StringUtils.ticksToSeconds(RIPOSTE_SWORD_DURATION), StringUtils.ticksToSeconds(RIPOSTE_AXE_DURATION)),
							"When Riposte activates, deal %s melee damage to all mobs in a %s block radius and root them for %ss."
									.formatted(ENHANCEMENT_DAMAGE, ENHANCEMENT_RADIUS, StringUtils.ticksToSeconds(ENHANCEMENT_ROOT_DURATION)))
					.simpleDescription("While holding a weapon, cancel a mob's melee attack, stunning the mob or gaining damage.")
					.cooldown(RIPOSTE_1_COOLDOWN, RIPOSTE_2_COOLDOWN, CHARM_COOLDOWN)
					.displayItem(Material.SKELETON_SKULL);
	private int mCurrDuration = -1;
	private final int mMaxSwordDuration;

	@Nullable
	private BukkitRunnable mRunnable = null;

	public Riposte(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new RiposteCS());
		mMaxSwordDuration = CharmManager.getDuration(mPlayer, CHARM_DAMAGE_DURATION, RIPOSTE_SWORD_DURATION);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (!isOnCooldown()
				&& source != null
				&& event.getType() == DamageType.MELEE
				&& !event.isBlocked()) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (ItemUtils.isAxe(mainHand) || ItemUtils.isSword(mainHand)) {
				MovementUtils.knockAway(mPlayer, source, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, RIPOSTE_KNOCKBACK_SPEED), true);

				World world = mPlayer.getWorld();
				Location playerLoc = mPlayer.getLocation();

				if (isLevelTwo()) {
					if (ItemUtils.isSword(mainHand)) {
						mCurrDuration = 0;
						mRunnable = new BukkitRunnable() {
							@Override
							public void run() {
								mCurrDuration++;
								if (mCurrDuration >= mMaxSwordDuration) {
									this.cancel();
								}
							}

							@Override
							public synchronized void cancel() {
								super.cancel();
								mCurrDuration = -1;
								ClientModHandler.updateAbility(mPlayer, Riposte.this);
							}
						};
						cancelOnDeath(mRunnable.runTaskTimer(mPlugin, 0, 1));
					} else if (ItemUtils.isAxe(mainHand)) {
						EntityUtils.applyStun(mPlugin, CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, RIPOSTE_AXE_DURATION), source);
						mCosmetic.onAxeStun(world, playerLoc);
					}
				}

				mCosmetic.onParry(mPlayer, world, playerLoc, source);
				putOnCooldown();
				ClientModHandler.updateAbility(mPlayer, this);
				event.setCancelled(true);
				mPlayer.setNoDamageTicks(20);
				mPlayer.setLastDamage(event.getDamage());

				if (isEnhanced()) {
					double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, ENHANCEMENT_DAMAGE);
					int duration = CharmManager.getDuration(mPlayer, CHARM_ROOT_DURATION, ENHANCEMENT_ROOT_DURATION);
					for (LivingEntity mob : new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), CharmManager.getRadius(mPlayer, CHARM_RADIUS, ENHANCEMENT_RADIUS)).getHitMobs()) {
						DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, damage, ClassAbility.RIPOSTE, true, true);
						EntityUtils.applySlow(mPlugin, duration, 1.0f, mob);
					}
					mCosmetic.onEnhancedParry(world, playerLoc);
				}
			}
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE
				&& ItemUtils.isSword(mPlayer.getInventory().getItemInMainHand())
				&& mCurrDuration != -1) {
			event.setDamage(event.getDamage() * (1 + RIPOSTE_SWORD_BONUS_DAMAGE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BONUS_DAMAGE)));
			if (mRunnable != null && !mRunnable.isCancelled()) {
				mRunnable.cancel();
			}
			mCosmetic.onSwordAttack(mPlayer.getWorld(), mPlayer.getLocation());
		}
		return false; // prevents multiple applications itself by clearing mSwordTimer
	}

	@Override
	public int getInitialAbilityDuration() {
		return mMaxSwordDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return mCurrDuration == -1 ? 0 : Math.min(mMaxSwordDuration, mMaxSwordDuration - mCurrDuration);
	}
}
