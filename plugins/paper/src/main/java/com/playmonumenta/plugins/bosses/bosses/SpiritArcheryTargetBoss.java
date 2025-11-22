package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;


public class SpiritArcheryTargetBoss extends BossAbilityGroup implements Listener {
	public static final String identityTag = "boss_spirit_archery_target";
	public boolean mHasBeenHitByDoTRecently;


	public SpiritArcheryTargetBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mHasBeenHitByDoTRecently = false;

		Plugin.getInstance().mEffectManager.addEffect(boss, PercentDamageReceived.GENERIC_NAME,
			new PercentDamageReceived(20 * 60 * 33, -0.9));
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (event.getDamager() instanceof Player player) {
			switch (event.getType()) {
				case PROJECTILE, PROJECTILE_ENCH, TRUE:
					// Projectile shots also deal an instance of True damage
					break;
				case PROJECTILE_SKILL:
					player.sendMessage(Component.text("[Spiritsinger]", NamedTextColor.GOLD)
						.append(Component.text(" I assure you, you cannot dispel them by imitating a bowshot.", NamedTextColor.WHITE)));
					break;
				case MELEE, MELEE_ENCH, MELEE_SKILL:
					player.sendMessage(Component.text("[Spiritsinger]", NamedTextColor.GOLD)
						.append(Component.text(" Their wrath is to arrows, not swords. They will only be calmed by arrows.", NamedTextColor.WHITE)));
					break;
				case MAGIC:
					player.sendMessage(Component.text("[Spiritsinger]", NamedTextColor.GOLD)
						.append(Component.text(" Your arcane arts hold no power over the spirits.", NamedTextColor.WHITE)));
					break;
				case AILMENT, FIRE, POISON:
					if (!mHasBeenHitByDoTRecently) {
						player.sendMessage(Component.text("[Spiritsinger]", NamedTextColor.GOLD)
							.append(Component.text(" It would displease them greatly if they knew their bodies were desecrated.", NamedTextColor.WHITE)));
						mHasBeenHitByDoTRecently = true;
						Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
							// After 4s, allow this message to be sent again. This prevents chat spam.
							mHasBeenHitByDoTRecently = false;
						}, 80);
					}
					break;
				default:
					player.sendMessage(Component.text("[Spiritsinger]", NamedTextColor.GOLD)
						.append(Component.text(" Peace, friend... please cease ere I release thee.", NamedTextColor.WHITE)));
			}
		}
	}

	@Override
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void bossHitByProjectile(ProjectileHitEvent event) {
		Projectile proj = event.getEntity();
		ItemStatManager.PlayerItemStats playerItemStats = DamageListener.getProjectileItemStats(proj);
		if (proj.getShooter() instanceof Player player) {
			if (playerItemStats == null) {
				return;
			}
			ItemStatManager.PlayerItemStats.ItemStatsMap itemStatsMap = playerItemStats.getItemStats();
			if ((int) itemStatsMap.get(EnchantmentType.SPIRITSHOT) != 0) {
				if (EntityUtils.isAbilityTriggeringProjectile(proj, true)) {
					for (Entity passenger : mBoss.getPassengers()) {
						passenger.remove();
					}
					mBoss.setHealth(0);
				}
			} else {
				player.sendMessage(Component.text("[Spiritsinger]", NamedTextColor.GOLD)
					.append(Component.text(" The only bowshot they will respect is mine. Grant them their", NamedTextColor.WHITE))
					.append(Component.text(" Freedom", NamedTextColor.DARK_PURPLE))
					.append(Component.text(".", NamedTextColor.WHITE)));
			}
		}
	}
}
