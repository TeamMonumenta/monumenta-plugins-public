package com.playmonumenta.plugins.abilities.mage;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

/*
 * Channeling: After casting a spell, your next melee hit deals 3 / 6 extra damage.
 * Depending on the spell type cast (fire, ice, arcane), your attack will also
 * set the hit enemy on fire, apply slowness II, or apply weakness I for 4 seconds.
 */

public class Channeling extends Ability {

	private static final Particle.DustOptions CHANNELING_COLOR = new Particle.DustOptions(Color.fromRGB(91, 187, 255), 0.75f);

	private static final int CHANNELING_1_DAMAGE = 3;
	private static final int CHANNELING_2_DAMAGE = 6;
	private static final int CHANNELING_EFFECT_DURATION_SECONDS = 4;
	private static final int CHANNELING_EFFECT_DURATION = CHANNELING_EFFECT_DURATION_SECONDS * 20;

	private final int mDamage;
	private Spells mLastSpellCast;

	public Channeling(Plugin plugin, Player player) {
		super(plugin, player, "Channeling");
		mInfo.mLinkedSpell = Spells.CHANNELING;
		mInfo.mScoreboardId = "Channeling";
		mInfo.mShorthandName = "Ch";
		mInfo.mDescriptions.add(
			String.format(
				"After casting a spell that has a cooldown, attacking an enemy with a wand deals %s extra arcane damage to it. Based on the type of that most recent spell with a cooldown, this skill also applies a %ss effect on that enemy - fire spells set it on fire and ice spells apply slowness II, while other (arcane) spells apply weakness I.",
				CHANNELING_1_DAMAGE,
				CHANNELING_EFFECT_DURATION_SECONDS
			) // Amplifiers have no constants
		);
		mInfo.mDescriptions.add(
			String.format(
				"Extra damage is increased from %s to %s.",
				CHANNELING_1_DAMAGE,
				CHANNELING_2_DAMAGE
			)
		);
		mDamage = getAbilityScore() == 1 ? CHANNELING_1_DAMAGE : CHANNELING_2_DAMAGE;
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		// Replace the previous spell as new ones are cast except if it is Arcane Strike, instead of storing the first (oldest) spell via null comparison.
		// This way applied effect will be of the latest eligible spell's type
		if (!event.getAbility().equals(Spells.ARCANE_STRIKE)) {
			mLastSpellCast = event.getAbility();
		}
		return true;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (mLastSpellCast != null && event.getEntity() instanceof LivingEntity && event.getCause() == DamageCause.ENTITY_ATTACK) {
			LivingEntity mob = (LivingEntity) event.getEntity();

			// Do Channeling damage in a runnable so we can set the damage ticks afterwards.
			new BukkitRunnable() {
				@Override
				public void run() {
					int ticks = mob.getNoDamageTicks();
					mob.setNoDamageTicks(0);
					// Call CustomDamageEvent, but not spellshock.
					EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.ARCANE, true, mInfo.mLinkedSpell, false, false);
					mob.setNoDamageTicks(ticks);
				}
			}.runTaskLater(mPlugin, 1);

			Location loc = mob.getLocation();
			World world = mPlayer.getWorld();
			world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1.5f);
			world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 2f);
			world.spawnParticle(Particle.REDSTONE, loc.clone().add(0, 1, 0), 25, 0.45, 0.65, 0.45, 0, CHANNELING_COLOR);
			world.spawnParticle(Particle.FIREWORKS_SPARK, loc.clone().add(0, 1, 0), 8, 0.25, 0.5, 0.25, 0.05);
			world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 8, 0.35, 0, 0.35, 0.12);

			if (mLastSpellCast == Spells.MAGMA_SHIELD || mLastSpellCast == Spells.STARFALL) {
				world.spawnParticle(Particle.FLAME, loc.clone().add(0, 1, 0), 18, 0.25, 0.5, 0.25, 0.05);
				world.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, 1, 0), 10, 0.25, 0.5, 0.25, 0.05);
				world.spawnParticle(Particle.LAVA, loc.clone().add(0, 1, 0), 4, 0.25, 0.5, 0.25, 0.0);
				world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1f, 0.5f);
				EntityUtils.applyFire(mPlugin, CHANNELING_EFFECT_DURATION, mob, mPlayer);
			} else if (mLastSpellCast == Spells.FROST_NOVA || mLastSpellCast == Spells.BLIZZARD) {
				world.spawnParticle(Particle.FALLING_DUST, loc.clone().add(0, 1, 0), 35, 0.45, 0.5, 0.45, 0, Bukkit.createBlockData("snow_block"));
				world.spawnParticle(Particle.SNOWBALL, loc.clone().add(0, 1, 0), 35, 0.25, 0.5, 0.25, 0.3);
				world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 0.65f);
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 1f, 2f);
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, CHANNELING_EFFECT_DURATION, 1, false, true));
			} else {
				world.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 15, 0.25, 0.5, 0.25, 0.25);
				world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.25f);
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WEAKNESS, CHANNELING_EFFECT_DURATION, 0, false, true));
			}
			mLastSpellCast = null;
		}

		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return InventoryUtils.isWandItem(mainHand);
	}

}
