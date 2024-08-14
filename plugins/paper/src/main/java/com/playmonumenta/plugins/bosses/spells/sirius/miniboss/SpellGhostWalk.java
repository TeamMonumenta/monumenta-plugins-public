package com.playmonumenta.plugins.bosses.spells.sirius.miniboss;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.managers.GlowingManager;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

public class SpellGhostWalk extends Spell {

	private boolean mGhost;
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private boolean mOnCooldown;
	private static final int COOLDOWN = 10 * 20;
	private static final int DURATION = 3 * 20;
	private List<Player> mWarned;

	public SpellGhostWalk(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mGhost = false;
		mOnCooldown = false;
		mWarned = new ArrayList<>();
	}


	@Override
	public void run() {
		mOnCooldown = true;
		mWarned = new ArrayList<>();
		Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> mOnCooldown = false, COOLDOWN + 20);
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 1, 1);
		new BukkitRunnable() {
			int mTicks = 0;
			final Team mPrevious = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(mBoss.getUniqueId().toString());

			@Override
			public void run() {
				if (mTicks == 0) {
					mGhost = true;
					GlowingManager.startGlowing(mBoss, NamedTextColor.LIGHT_PURPLE, DURATION, GlowingManager.BOSS_SPELL_PRIORITY);
					World world = mBoss.getWorld();
					world.playSound(mBoss.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.HOSTILE, 1f, 0.4f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_ALLAY_DEATH, SoundCategory.HOSTILE, 0.2f, 0.4f);
					mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, DURATION, 1, false, true));
				}
				if (mTicks >= DURATION) {
					mGhost = false;
					World world = mBoss.getWorld();
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_HURT, SoundCategory.HOSTILE, 0.4f, 1.2f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_ALLAY_DEATH, SoundCategory.HOSTILE, 0.2f, 0.4f);
					this.cancel();
					if (mPrevious != null) {
						mPrevious.addEntity(mBoss);
					}
				}
				mTicks += 20;
			}
		}.runTaskTimer(mPlugin, 0, 20);
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown || !mBoss.isGlowing();
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mGhost) {
			if (event.getType() == DamageEvent.DamageType.MAGIC || (event.getType() == DamageEvent.DamageType.TRUE && event.getDamager() == null)) {
				super.onHurt(event);
			} else {
				event.setCancelled(true);
				if (event.getDamager() != null && event.getDamager() instanceof Player player && !mWarned.contains(player)) {
					mWarned.add(player);
					player.sendMessage(Component.text("Your attacks phase through its ethereal form.", NamedTextColor.GRAY));
				}
			}
		} else {
			super.onHurt(event);
		}
	}
}
