package com.playmonumenta.plugins.abilities.cleric;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.enchantments.BaseAbilityEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Celestial extends Ability {

	public static class CelestialCooldownEnchantment extends BaseAbilityEnchantment {
		public CelestialCooldownEnchantment() {
			super("Celestial Blessing Cooldown", EnumSet.of(ItemSlot.ARMOR));
		}
	}

	private static final int CELESTIAL_COOLDOWN = 40 * 20;
	private static final int CELESTIAL_1_DURATION = 10 * 20;
	private static final int CELESTIAL_2_DURATION = 12 * 20;
	private static final double CELESTIAL_RADIUS = 12;
	private static final String CELESTIAL_1_TAGNAME = "Celestial_1";
	private static final String CELESTIAL_2_TAGNAME = "Celestial_2";
	private static final String CELESTIAL_MULTIPLE_TAGNAME = "CelestialAlreadyHasBuff";
	private static final double CELESTIAL_1_DAMAGE_MULTIPLIER = 1.20;
	private static final double CELESTIAL_2_DAMAGE_MULTIPLIER = 1.35;

	public Celestial(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Celestial Blessing");
		mInfo.mLinkedSpell = Spells.CELESTIAL_BLESSING;
		mInfo.mScoreboardId = "Celestial";
		mInfo.mShorthandName = "CB";
		mInfo.mDescriptions.add("When you strike while sneaking (regardless of whether you hit anything), while on the ground, you and all other players in a 12 block radius gain +20% attack damage and +20% speed for 10 s. (Cooldown: 40 s)");
		mInfo.mDescriptions.add("Increases the buff to +35% attack damage for 12 s.");
		mInfo.mCooldown = CELESTIAL_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public void cast(Action action) {
		mInfo.mCooldown = (int) CelestialCooldownEnchantment.getCooldown(mPlayer, CELESTIAL_COOLDOWN, CelestialCooldownEnchantment.class);
		int celestial = getAbilityScore();

		World world = mPlayer.getWorld();
		int duration = celestial == 1 ? CELESTIAL_1_DURATION : CELESTIAL_2_DURATION;
		String tagName = celestial == 1 ? CELESTIAL_1_TAGNAME : CELESTIAL_2_TAGNAME;

		List<Player> affectedPlayers = PlayerUtils.playersInRange(mPlayer, CELESTIAL_RADIUS, true);

		// Don't buff players that have their class disabled
		affectedPlayers.removeIf(p -> p.getScoreboardTags().contains("disable_class"));

		// Give these players the metadata tag that boosts their damage
		for (Player p : affectedPlayers) {
			// This workaround means that up to two Celestial Blessing clerics can use this ability without interfering with the other
			// Does not solve anything for 3+ clerics using the ability at the same time, but this would be a rare edge case
			if (!p.hasMetadata(CELESTIAL_MULTIPLE_TAGNAME) && (p.hasMetadata(CELESTIAL_1_TAGNAME) || p.hasMetadata(CELESTIAL_2_TAGNAME))) {
				p.setMetadata(CELESTIAL_MULTIPLE_TAGNAME, new FixedMetadataValue(mPlugin, 0));
			} else if (!p.hasMetadata(CELESTIAL_1_TAGNAME) && !p.hasMetadata(CELESTIAL_2_TAGNAME)) {
				p.setMetadata(tagName, new FixedMetadataValue(mPlugin, 0));
				AttributeInstance speed = p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
				if (speed != null) {
					speed.setBaseValue(speed.getBaseValue() + 0.02);
				}
				Location loc = p.getLocation();
				world.spawnParticle(Particle.SPELL_INSTANT, loc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
				world.spawnParticle(Particle.SPELL_INSTANT, loc.clone().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0);
				world.spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0.1);
				world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.75f);
				world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.75f, 1.25f);
				world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 0.75f, 1.1f);
			}
		}


		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				mT += 2;
				for (Player p : affectedPlayers) {
					Location loc = p.getLocation();
					world.spawnParticle(Particle.SPELL_INSTANT, loc.clone().add(0, 1, 0), 1, 0.25, 0.25, 0.25, 0.1);
					world.spawnParticle(Particle.SPELL_INSTANT, loc.clone().add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0);
					world.spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0.1);
				}

				if (mT >= duration) {
					for (Player p : affectedPlayers) {
						if (p.hasMetadata(CELESTIAL_MULTIPLE_TAGNAME)) {
							p.removeMetadata(CELESTIAL_MULTIPLE_TAGNAME, mPlugin);
						} else {
							p.removeMetadata(tagName, mPlugin);
							AttributeInstance speed = p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
							if (speed != null) {
								speed.setBaseValue(speed.getBaseValue() - 0.02);
							}
						}
						Location loc = p.getLocation();
						world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 0.65f);
						world.spawnParticle(Particle.SPELL_INSTANT, loc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
						world.spawnParticle(Particle.SPELL_INSTANT, loc.clone().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0);
						world.spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0.1);
					}
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 2);

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		if (mPlayer.isOnGround()) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			return (mPlayer.isSneaking() && !InventoryUtils.isPickaxeItem(mainHand));
		}
		return false;
	}

	/**
	 * A static method that is run every time a player does damage (regardless of class) to
	 * check for this ability.
	 */
	public static void modifyDamage(Player player, EntityDamageByEntityEvent event) {
		if (player.hasMetadata(CELESTIAL_1_TAGNAME)) {
			event.setDamage(event.getDamage() * CELESTIAL_1_DAMAGE_MULTIPLIER);
		} else if (player.hasMetadata(CELESTIAL_2_TAGNAME)) {
			event.setDamage(event.getDamage() * CELESTIAL_2_DAMAGE_MULTIPLIER);
		}
	}
}
