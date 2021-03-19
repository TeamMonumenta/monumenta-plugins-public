package com.playmonumenta.plugins.abilities.mage.elementalist;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.SpellDamage;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class Starfall extends Ability {
	private static final double STARFALL_ANGLE = 70.0;

	private static final int STARFALL_COOLDOWN = 20 * 18;
	private static final int STARFALL_1_DAMAGE = 15;
	private static final int STARFALL_2_DAMAGE = 27;
	private static final int STARFALL_FIRE_DURATION = 20 * 3;
	private static final double STARFALL_RADIUS = 5;
	private static final float STARFALL_KNOCKAWAY_SPEED = 0.7f;

	private final int mDamage;

	/* The player's getTicksLived() when the skill was last primed or cast */
	private int mPrimedTick = -1;

	public Starfall(Plugin plugin, Player player) {
		super(plugin, player, "Starfall");
		mInfo.mLinkedSpell = Spells.STARFALL;
		mInfo.mScoreboardId = "Starfall";
		mInfo.mShorthandName = "SF";
		mInfo.mDescriptions.add("Press the swap key while holding a wand to summon a meteor where the player is looking (up to 25 blocks). It deals 15 damage in a 5 block radius and sets enemies on fire for 3 seconds. Cooldown: 18s.");
		mInfo.mDescriptions.add("Damage is increased to 27.");
		mInfo.mCooldown = STARFALL_COOLDOWN;
		mDamage = getAbilityScore() == 1 ? STARFALL_1_DAMAGE : STARFALL_2_DAMAGE;
	}
	
	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		ItemStack mainHandItem = mPlayer.getInventory().getItemInMainHand();
		if (InventoryUtils.isWandItem(mainHandItem) && !mPlayer.isSneaking()) {
			event.setCancelled(true);
			
			Location loc = mPlayer.getEyeLocation();
			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 0.85f);
			world.spawnParticle(Particle.LAVA, mPlayer.getLocation(), 15, 0.25f, 0.1f, 0.25f);
			world.spawnParticle(Particle.FLAME, mPlayer.getLocation(), 30, 0.25f, 0.1f, 0.25f, 0.15f);
			Vector dir = loc.getDirection().normalize();
			for (int i = 0; i < 25; i++) {
				loc.add(dir);

				mPlayer.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
				int size = EntityUtils.getNearbyMobs(loc, 2, mPlayer).size();
				if (loc.getBlock().getType().isSolid() || i >= 24 || size > 0) {
					launchMeteor(mPlayer, loc);
					break;
				}
			}
			
			putOnCooldown();
		}
	}

	private void launchMeteor(final Player player, final Location loc) {
		Location ogLoc = loc.clone();
		loc.add(0, 40, 0);
		new BukkitRunnable() {
			double mT = 0;
			@Override
			public void run() {
				mT += 1;
				World world = mPlayer.getWorld();
				for (int i = 0; i < 8; i++) {
					loc.subtract(0, 0.25, 0);
					if (loc.getBlock().getType().isSolid()) {
						if (loc.getY() - ogLoc.getY() <= 2) {
							world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0);
							world.spawnParticle(Particle.FLAME, loc, 175, 0, 0, 0, 0.235F);
							world.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.2F);
							world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0, 0, 0, 0.2F);
							this.cancel();

							float damage = SpellDamage.getSpellDamage(mPlayer, mDamage);

							for (LivingEntity e : EntityUtils.getNearbyMobs(loc, STARFALL_RADIUS, mPlayer)) {
								EntityUtils.damageEntity(mPlugin, e, damage, player, MagicType.FIRE, true, mInfo.mLinkedSpell);
								EntityUtils.applyFire(mPlugin, STARFALL_FIRE_DURATION, e, mPlayer);
								MovementUtils.knockAway(loc, e, STARFALL_KNOCKAWAY_SPEED);
							}
							break;
						}
					}
				}
				world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1);
				world.spawnParticle(Particle.FLAME, loc, 25, 0.25F, 0.25F, 0.25F, 0.1F);
				world.spawnParticle(Particle.SMOKE_LARGE, loc, 5, 0.25F, 0.25F, 0.25F, 0.1F);

				if (mT >= 50) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}
}
