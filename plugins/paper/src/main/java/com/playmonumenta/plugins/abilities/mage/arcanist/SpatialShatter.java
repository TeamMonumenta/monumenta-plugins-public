package com.playmonumenta.plugins.abilities.mage.arcanist;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
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
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.abilities.AbilityInfo;


public class SpatialShatter extends Ability {

	private static final int RANGE = 8;
	private static final int RADIUS = 4;
	private static final int DAMAGE_1 = 9;
	private static final int DAMAGE_2 = 15;
	private static final int COOLDOWN = 20 * 6;
	private static final int CDR_CAP_1 = 30;
	private static final int CDR_CAP_2 = 20 * 2;
	private static final double CDR_1 = 0.15;
	private static final double CDR_2 = 0.2;
	private static final double HITBOX_LENGTH = 0.55;

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(16, 144, 192), 1.0f);

	private final int mDamage;
	private final int mCdrCap;
	private final double mCdr;

	public SpatialShatter(Plugin plugin, Player player) {
		super(plugin, player, "Spatial Shatter");
		mInfo.mScoreboardId = "SpatialShatter";
		mInfo.mShorthandName = "SpSh";
		mInfo.mDescriptions.add("Pressing the swap key while holding a wand fires a burst of magic. This projectile travels up to 8 blocks and upon contact with a surface or an enemy, it explodes, dealing 9 damage to all mobs within 4 blocks of the explosion. Additionally, upon explosion, all other skill cooldowns are reduced by 15% (up to a maximum of 1.5s). Cooldown: 6s.");
		mInfo.mDescriptions.add("The damage of your explosion is increased to 15, and your spell cooldown reduction is increased to 20% (up to maximum of 2s).");
		mInfo.mLinkedSpell = Spells.SPATIAL_SHATTER;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.ALL;
		mInfo.mIgnoreCooldown = true;
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
		mCdrCap = getAbilityScore() == 1 ? CDR_CAP_1 : CDR_CAP_2;
		mCdr = getAbilityScore() == 1 ? CDR_1 : CDR_2;
	}
	
	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		ItemStack mainHandItem = mPlayer.getInventory().getItemInMainHand();
		if (InventoryUtils.isWandItem(mainHandItem) && !mPlayer.isSneaking()) {
			event.setCancelled(true);
			if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
				return;
			}
			
			putOnCooldown();
			
			Location loc = mPlayer.getEyeLocation();
			Vector direction = loc.getDirection();
			Vector shift = direction.normalize().multiply(HITBOX_LENGTH);
			BoundingBox box = BoundingBox.of(loc, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
			box.shift(direction);
						
			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.4f, 1.75f);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 0.75f, 1.5f);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.25f, 0.5f);	

			Set<LivingEntity> nearbyMobs = new HashSet<LivingEntity>(EntityUtils.getNearbyMobs(loc, RANGE));
			
			for (double r = 0; r < RANGE; r += HITBOX_LENGTH) {
				Location bLoc = box.getCenter().toLocation(world);
				
				world.spawnParticle(Particle.FIREWORKS_SPARK, bLoc, 5, 0.1, 0.1, 0.1, 0.1);
				world.spawnParticle(Particle.SPELL_WITCH, bLoc, 5, 0, 0, 0, 0.5);
				world.spawnParticle(Particle.REDSTONE, bLoc, 20, 0.2, 0.2, 0.2, 0.1, COLOR);

				if (bLoc.getBlock().getType().isSolid()) {
					bLoc.subtract(direction.multiply(0.5));
					explode(bLoc);
					return;
				}
				
				Iterator<LivingEntity> iter = nearbyMobs.iterator();
				while (iter.hasNext()) {
					LivingEntity mob = iter.next();
					if (mob.getBoundingBox().overlaps(box)) {
						if (EntityUtils.isHostileMob(mob)) {
							explode(bLoc);
							return;
						}
					}
				}
				box.shift(shift);
			}
		}
	}
	
	private void explode(Location loc) {
		double damage = SpellDamage.getSpellDamage(mPlayer, mDamage);
		boolean cdr = true;
		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.CLOUD, loc, 25, 0, 0, 0, 0.125);
		world.spawnParticle(Particle.REDSTONE, loc, 10, 0, 0, 0, 0.1, COLOR);
		
		world.spawnParticle(Particle.REDSTONE, loc, 125, 2.5, 2.5, 2.5, 0.25, COLOR);
		world.spawnParticle(Particle.FALLING_DUST, loc, 150, 2.5, 2.5, 2.5, Material.LIGHT_BLUE_CONCRETE.createBlockData());
		
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.25f, 0.5f);
		
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, RADIUS, mPlayer);
		for (LivingEntity mob : mobs) {
			if (cdr == true) {
				cdr = false;
				updateCooldowns(mCdr);
			}
			EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer, MagicType.ARCANE, true, mInfo.mLinkedSpell);
			MovementUtils.knockAway(loc, mob, 0.3f, 0.3f);
		}
	}
	
	public void updateCooldowns(double percent) {
		for (Ability abil : AbilityManager.getManager().getPlayerAbilities(mPlayer).getAbilities()) {
			AbilityInfo info = abil.getInfo();
			if (info.mLinkedSpell == mInfo.mLinkedSpell) {
				continue;
			}
			int totalCD = info.mCooldown;
			int reducedCD = Math.min((int) (totalCD * percent), mCdrCap);
			mPlugin.mTimers.updateCooldown(mPlayer, info.mLinkedSpell, reducedCD);
		}
	}
}
