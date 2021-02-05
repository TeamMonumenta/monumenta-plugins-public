package com.playmonumenta.plugins.abilities.mage.elementalist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.SpellDamage;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class Blizzard extends Ability {

	private static final int BLIZZARD_1_RADIUS = 6;
	private static final int BLIZZARD_2_RADIUS = 8;
	private static final float BLIZZARD_1_DAMAGE = 1.5f;
	private static final float BLIZZARD_2_DAMAGE = 3.0f;
	private static final int BLIZZARD_1_COOLDOWN = 30;
	private static final int BLIZZARD_2_COOLDOWN = 25;

	public Blizzard(Plugin plugin, Player player) {
		super(plugin, player, "Blizzard");
		mInfo.mScoreboardId = "Blizzard";
		mInfo.mShorthandName = "Bl";
		mInfo.mDescriptions.add("Shift Right Clicking while looking up creates an aura of ice and snow in a radius of 6 blocks that lasts 10 seconds and stays centered on the user. Mobs that enter the aura get 10% Slowness. After three seconds in the aura they get 20% Slowness. After six seconds in the aura enemies are given 40% Slowness (bosses remain at 20%). Enemies take 1.5 damage a second while in the aura. Entities that are on fire within the aura are extinguished. This spell can trigger Spellshock but cannot apply it. Cooldown: 30s (starting after cast).");
		mInfo.mDescriptions.add("The radius is increased to 8 blocks. Mobs take 3 damage a second. Cooldown: 25s.");
		mInfo.mLinkedSpell = Spells.BLIZZARD;
		mInfo.mCooldown = getAbilityScore() == 1 ? 20 * BLIZZARD_1_COOLDOWN : 20 * BLIZZARD_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	private final Map<UUID, Integer> mAffected = new HashMap<>();
	private boolean mActive = false;

	@Override
	public void cast(Action action) {
		if (mActive) {
			return;
		}

		mActive = true;
		putOnCooldown();

		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 2);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.75f);
		float damage = SpellDamage.getSpellDamage(mPlayer, getAbilityScore() == 1 ? BLIZZARD_1_DAMAGE : BLIZZARD_2_DAMAGE);
		double radius = getAbilityScore() == 1 ? BLIZZARD_1_RADIUS : BLIZZARD_2_RADIUS;
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				Location loc = mPlayer.getLocation();
				List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, radius, mPlayer);
				mT++;
				if (mT % 10 == 0) {
					for (Player p : PlayerUtils.playersInRange(loc, radius)) {
						p.setFireTicks(-10);
					}
					for (LivingEntity mob : mobs) {
						if (!mAffected.containsKey(mob.getUniqueId())) {
							EntityUtils.applySlow(mPlugin, 20 * 5, 0.1, mob);
							mAffected.put(mob.getUniqueId(), 1);
						} else {
							int duration = mAffected.get(mob.getUniqueId());
							if (duration >= 12 && !EntityUtils.isBoss(mob)) {
								EntityUtils.applySlow(mPlugin, 20 * 5, 0.4, mob);
							} else if (duration >= 6) {
								EntityUtils.applySlow(mPlugin, 20 * 5, 0.2, mob);
							}
							mAffected.put(mob.getUniqueId(), duration + 1);
						}
					}
				}

				if (mT % 20 == 0) {
					for (LivingEntity mob : mobs) {
						Vector v = mob.getVelocity();
						mob.setNoDamageTicks(0);
						double lastDamage = mob.getLastDamage();
						EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer, MagicType.ICE, true, mInfo.mLinkedSpell, false, true);
						mob.setLastDamage(lastDamage + damage);
						mob.setVelocity(v);
					}
				}

				world.spawnParticle(Particle.SNOWBALL, loc, 6, 2, 2, 2, 0.1);
				world.spawnParticle(Particle.CLOUD, loc, 4, 2, 2, 2, 0.05);
				world.spawnParticle(Particle.CLOUD, loc, 3, 0.1, 0.1, 0.1, 0.15);
				if (mT >= 20 * 10 || mPlayer.isDead() || !mPlayer.isValid()) {
					this.cancel();
					mAffected.clear();
					mActive = false;
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		return !mActive && mPlayer.isSneaking() && mPlayer.getLocation().getPitch() < -50 && (InventoryUtils.isWandItem(mHand));
	}

}
