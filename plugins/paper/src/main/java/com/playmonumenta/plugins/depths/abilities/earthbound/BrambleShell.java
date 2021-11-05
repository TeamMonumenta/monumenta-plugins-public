package com.playmonumenta.plugins.depths.abilities.earthbound;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

public class BrambleShell extends DepthsAbility {

	public static final String ABILITY_NAME = "Bramble Shell";
	public static final double[] BRAMBLE_DAMAGE = {8, 10, 12, 14, 16, 24};

	public BrambleShell(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.SWEET_BERRIES;
		mTree = DepthsTree.EARTHBOUND;
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		LivingEntity mob = (LivingEntity) event.getDamager();
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			trigger(mob, event);
		}

		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		ProjectileSource source = ((Projectile) event.getDamager()).getShooter();
		if (source instanceof LivingEntity) {
			LivingEntity mob = (LivingEntity) source;
			trigger(mob, event);
		}

		return true;
	}

	private void trigger(LivingEntity mob, EntityDamageByEntityEvent event) {
		if (AbilityUtils.isBlocked(event)) {
			return;
		}
		Location loc = mob.getLocation();
		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.BLOCK_CRACK, loc.add(0, mob.getHeight() / 2, 0), 25, 0.5, 0.5, 0.5, 0.125, Bukkit.createBlockData(Material.SWEET_BERRY_BUSH));
		world.playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, 1, 0.8f);

		if (!mPlayer.isBlocking() || event.getFinalDamage() > 0) {
			EntityUtils.damageEntity(mPlugin, mob, BRAMBLE_DAMAGE[mRarity - 1], mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);
		}
	}

	@Override
	public String getDescription(int rarity) {
		return "Whenever an enemy deals melee or projectile damage to you, they take " + DepthsUtils.getRarityColor(rarity) + BRAMBLE_DAMAGE[rarity - 1] + ChatColor.WHITE + " damage.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.EARTHBOUND;
	}
}
