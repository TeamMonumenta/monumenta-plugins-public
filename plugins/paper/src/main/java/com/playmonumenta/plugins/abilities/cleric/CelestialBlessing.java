package com.playmonumenta.plugins.abilities.cleric;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;



public class CelestialBlessing extends Ability {

	public static class CelestialCooldownEnchantment extends BaseAbilityEnchantment {
		public CelestialCooldownEnchantment() {
			super("Celestial Blessing Cooldown", EnumSet.of(ItemSlot.ARMOR));
		}
	}

	private static final int CELESTIAL_COOLDOWN = 40 * 20;
	private static final int CELESTIAL_1_DURATION = 10 * 20;
	private static final int CELESTIAL_2_DURATION = 12 * 20;
	private static final double CELESTIAL_RADIUS = 12;
	private static final double CELESTIAL_1_EXTRA_DAMAGE = 0.20;
	private static final double CELESTIAL_2_EXTRA_DAMAGE = 0.35;
	private static final double CELESTIAL_EXTRA_SPEED = 0.20;
	private static final String ATTR_NAME = "CelestialBlessingExtraSpeedAttr";
	private static final EnumSet<DamageCause> AFFECTED_DAMAGE_CAUSES = EnumSet.of(
			DamageCause.ENTITY_ATTACK,
			DamageCause.ENTITY_SWEEP_ATTACK,
			DamageCause.PROJECTILE
	);
	public static final String DAMAGE_EFFECT_NAME = "CelestialBlessingExtraDamage";

	public CelestialBlessing(Plugin plugin, Player player) {
		super(plugin, player, "Celestial Blessing");
		mInfo.mLinkedSpell = ClassAbility.CELESTIAL_BLESSING;
		mInfo.mScoreboardId = "Celestial";
		mInfo.mShorthandName = "CB";
		mInfo.mDescriptions.add("When you strike while sneaking, you and all other players in a 12 block radius gain +20% melee and projectile damage and +20% speed for 10 s. Cooldown: 40s.");
		mInfo.mDescriptions.add("Increases the buff to +35% damage for 12 s.");
		mInfo.mCooldown = CELESTIAL_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDisplayItem = new ItemStack(Material.SUGAR, 1);
	}

	@Override
	public void cast(Action action) {
		int celestial = getAbilityScore();

		World world = mPlayer.getWorld();
		int duration = celestial == 1 ? CELESTIAL_1_DURATION : CELESTIAL_2_DURATION;
		double extraDamage = celestial == 1 ? CELESTIAL_1_EXTRA_DAMAGE : CELESTIAL_2_EXTRA_DAMAGE;

		List<Player> affectedPlayers = PlayerUtils.playersInRange(mPlayer.getLocation(), CELESTIAL_RADIUS, true);

		// Don't buff players that have their class disabled
		affectedPlayers.removeIf(p -> p.getScoreboardTags().contains("disable_class"));

		// Give these players the metadata tag that boosts their damage
		for (Player p : affectedPlayers) {
			mPlugin.mEffectManager.addEffect(p, DAMAGE_EFFECT_NAME, new PercentDamageDealt(duration, extraDamage, AFFECTED_DAMAGE_CAUSES));
			mPlugin.mEffectManager.addEffect(p, "CelestialBlessingExtraSpeed", new PercentSpeed(duration, CELESTIAL_EXTRA_SPEED, ATTR_NAME));
			mPlugin.mEffectManager.addEffect(p, "CelestialBlessingParticles", new Aesthetics(duration,
				(entity, fourHertz, twoHertz, oneHertz) -> {
					// Tick effect
					Location loc = p.getLocation().add(0, 1, 0);
					world.spawnParticle(Particle.SPELL_INSTANT, loc, 2, 0.25, 0.25, 0.25, 0.1);
					world.spawnParticle(Particle.SPELL_INSTANT, loc, 2, 0.5, 0.5, 0.5, 0);
					world.spawnParticle(Particle.VILLAGER_HAPPY, loc, 2, 0.5, 0.5, 0.5, 0.1);
				}, (entity) -> {
					// Lose effect
					Location loc = p.getLocation();
					world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 0.65f);
					world.spawnParticle(Particle.SPELL_INSTANT, loc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
					world.spawnParticle(Particle.SPELL_INSTANT, loc.clone().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0);
					world.spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0.1);
				})
			);
			// Start effect
			Location loc = p.getLocation();
			world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.75f);
			world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.75f, 1.25f);
			world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 0.75f, 1.1f);
		}

		putOnCooldown();
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
	    if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
	        cast(Action.LEFT_CLICK_AIR);
	    }

	    return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return (mPlayer.isSneaking() && !ItemUtils.isPickaxe(mainHand));
	}

	@Override
	public Class<? extends BaseAbilityEnchantment> getCooldownEnchantment() {
		return CelestialCooldownEnchantment.class;
	}
}