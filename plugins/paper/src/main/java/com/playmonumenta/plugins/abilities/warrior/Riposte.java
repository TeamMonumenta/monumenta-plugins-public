package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.StringUtils;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Riposte extends Ability {

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
			.cooldown(RIPOSTE_1_COOLDOWN, RIPOSTE_2_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(new ItemStack(Material.SKELETON_SKULL, 1));

	private int mSwordTimer = Integer.MIN_VALUE;

	public Riposte(Plugin plugin, Player player) {
		super(plugin, player, INFO);
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
				if (isLevelTwo()) {
					if (ItemUtils.isSword(mainHand)) {
						int duration = CharmManager.getDuration(mPlayer, CHARM_DAMAGE_DURATION, RIPOSTE_SWORD_DURATION);
						mSwordTimer = Bukkit.getServer().getCurrentTick() + duration;
					} else if (ItemUtils.isAxe(mainHand)) {
						EntityUtils.applyStun(mPlugin, CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, RIPOSTE_AXE_DURATION), source);
					}
				}

				World world = mPlayer.getWorld();
				Location playerLoc = mPlayer.getLocation();
				world.playSound(playerLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 1.2f);
				world.playSound(playerLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.75f, 1.8f);
				Vector dir = LocationUtils.getDirectionTo(playerLoc.clone().add(0, 1, 0), source.getLocation().add(0, source.getHeight() / 2, 0));
				Location loc = mPlayer.getLocation().add(0, 1, 0).subtract(dir);
				new PartialParticle(Particle.SWEEP_ATTACK, loc, 8, 0.75, 0.5, 0.75, 0.001).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.FIREWORKS_SPARK, loc, 20, 0.75, 0.5, 0.75, 0.1).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CRIT, loc, 75, 0.1, 0.1, 0.1, 0.6).spawnAsPlayerActive(mPlayer);
				putOnCooldown();
				event.setCancelled(true);

				if (isEnhanced()) {
					double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, ENHANCEMENT_DAMAGE);
					int duration = CharmManager.getDuration(mPlayer, CHARM_ROOT_DURATION, ENHANCEMENT_ROOT_DURATION);
					for (LivingEntity mob : new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), CharmManager.getRadius(mPlayer, CHARM_RADIUS, ENHANCEMENT_RADIUS)).getHitMobs()) {
						DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, damage, ClassAbility.RIPOSTE, true, true);
						EntityUtils.applySlow(mPlugin, duration, 1.0f, mob);
					}
					world.playSound(playerLoc, Sound.BLOCK_ANVIL_FALL, SoundCategory.PLAYERS, 0.5f, 0.5f);
				}
			}
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE
			    && ItemUtils.isSword(mPlayer.getInventory().getItemInMainHand())
			    && Bukkit.getServer().getCurrentTick() <= mSwordTimer) {
			event.setDamage(event.getDamage() * (1 + RIPOSTE_SWORD_BONUS_DAMAGE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BONUS_DAMAGE)));
			mSwordTimer = Integer.MIN_VALUE;
		}
		return false; // prevents multiple applications itself by clearing mSwordTimer
	}

}
