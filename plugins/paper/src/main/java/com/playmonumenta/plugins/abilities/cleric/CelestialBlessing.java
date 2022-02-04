package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.List;



public class CelestialBlessing extends Ability {

	private static final int CELESTIAL_COOLDOWN = 40 * 20;
	private static final int CELESTIAL_1_DURATION = 10 * 20;
	private static final int CELESTIAL_2_DURATION = 12 * 20;
	private static final double CELESTIAL_RADIUS = 12;
	private static final double CELESTIAL_1_EXTRA_DAMAGE = 0.20;
	private static final double CELESTIAL_2_EXTRA_DAMAGE = 0.35;
	private static final double CELESTIAL_EXTRA_SPEED = 0.20;
	private static final String ATTR_NAME = "CelestialBlessingExtraSpeedAttr";
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
			DamageType.MELEE,
			DamageType.MELEE_SKILL,
			DamageType.MELEE_ENCH,
			DamageType.PROJECTILE
	);
	public static final String DAMAGE_EFFECT_NAME = "CelestialBlessingExtraDamage";

	public CelestialBlessing(Plugin plugin, @Nullable Player player) {
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
		if (mPlayer == null) {
			return;
		}

		int celestial = getAbilityScore();
		int duration = celestial == 1 ? CELESTIAL_1_DURATION : CELESTIAL_2_DURATION;
		double extraDamage = celestial == 1 ? CELESTIAL_1_EXTRA_DAMAGE : CELESTIAL_2_EXTRA_DAMAGE;

		World world = mPlayer.getWorld();

		List<Player> affectedPlayers = PlayerUtils.playersInRange(mPlayer.getLocation(), CELESTIAL_RADIUS, true);

		// Don't buff players that have their class disabled
		affectedPlayers.removeIf(p -> p.getScoreboardTags().contains("disable_class"));

		// Give these players the metadata tag that boosts their damage
		for (Player p : affectedPlayers) {
			mPlugin.mEffectManager.addEffect(p, DAMAGE_EFFECT_NAME, new PercentDamageDealt(duration, extraDamage, AFFECTED_DAMAGE_TYPES));
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
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			cast(Action.LEFT_CLICK_AIR);
		}
		return false;
	}

	@Override
	public boolean runCheck() {
		if (mPlayer == null) {
			return false;
		}
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return (mPlayer.isSneaking() && !ItemUtils.isPickaxe(mainHand));
	}

}
