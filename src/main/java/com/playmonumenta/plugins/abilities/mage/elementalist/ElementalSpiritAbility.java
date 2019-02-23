package com.playmonumenta.plugins.abilities.mage.elementalist;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.CustomDamageEvent;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.specializations.objects.ElementalSpirit;

public class ElementalSpiritAbility extends Ability {

	public ElementalSpiritAbility(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "ElementalSpirit";
		mInfo.linkedSpell = Spells.ELEMENTAL_SPIRIT;
		mInfo.cooldown = 20 * 8;
	}

	private ElementalSpirit spirit = null;

	@Override
	public void PlayerDealtCustomDamageEvent(CustomDamageEvent event) {
		if (spirit != null) {
			MagicType type = event.getMagicType();
			if (type == MagicType.FIRE || type == MagicType.ARCANE || type == MagicType.ICE) {
				spirit.setMagicType(type);
				spirit.getHurt().add(event.getDamaged());
			}
		}
	}

	@Override
	public void PeriodicTrigger(boolean twoHertz, boolean oneSecond, boolean twoSeconds, boolean fourtySeconds, boolean sixtySeconds, int originalTime) {
		Player player = mPlayer;
		if (oneSecond) {
			int elementalSpirit = getAbilityScore();

			if (elementalSpirit > 0) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.ELEMENTAL_SPIRIT)) {
					if (spirit == null) {
						spirit = new ElementalSpirit(player);
						new BukkitRunnable() {
							float t = 0f;
							double rotation = 0;

							@Override
							public void run() {
								Location loc = player.getLocation().add(0, 1, 0);
								t += 0.1f;
								rotation += 10;
								double radian1 = Math.toRadians(rotation);
								loc.add(Math.cos(radian1), Math.sin(t) * 0.5, Math.sin(radian1));
								mWorld.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0.01);
								mWorld.spawnParticle(Particle.SNOWBALL, loc, 1, 0, 0, 0, 0);

								if (AbilityManager.getManager().getPlayerAbility(mPlayer, ElementalSpiritAbility.class) == null ||
									!mPlayer.isOnline() || mPlayer == null || spirit == null ||
									mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.ELEMENTAL_SPIRIT)) {
									this.cancel();
									spirit = null;
								}

								if (spirit != null && spirit.getHurt().size() > 0) {
									List<LivingEntity> list = spirit.getHurt();

									LivingEntity target = null;
									if (list.size() > 0) {
										double dist = 100;
										for (LivingEntity e : list) {
											if (e.getLocation().distance(player.getLocation()) < dist) {
												dist = e.getLocation().distance(player.getLocation());
												target = e;
											}
										}
									}
									if (target != null) {
										spirit.damage(player, target, loc.clone());
										putOnCooldown();
									}
									spirit = null;
									this.cancel();
								}
								loc.subtract(Math.cos(radian1), Math.sin(t) * 0.5, Math.sin(radian1));
							}

						}.runTaskTimer(mPlugin, 0, 1);
					}
				}
			}
		}
	}
}
