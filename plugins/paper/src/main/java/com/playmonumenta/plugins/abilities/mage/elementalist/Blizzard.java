package com.playmonumenta.plugins.abilities.mage.elementalist;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;


public class Blizzard extends Ability {
	public static final String NAME = "Blizzard";
	public static final ClassAbility ABILITY = ClassAbility.BLIZZARD;

	public static final int DAMAGE_1 = 5;
	public static final int DAMAGE_2 = 10;
	public static final int SIZE_1 = 6;
	public static final int SIZE_2 = 8;
	public static final double SLOW_MULTIPLIER_1 = 0.25;
	public static final double SLOW_MULTIPLIER_2 = 0.3;
	public static final int DAMAGE_INTERVAL = 2 * Constants.TICKS_PER_SECOND;
	public static final int SLOW_INTERVAL = (int)(0.5 * Constants.TICKS_PER_SECOND);
	public static final int DURATION_TICKS = 10 * Constants.TICKS_PER_SECOND;
	public static final int SLOW_TICKS = 5 * Constants.TICKS_PER_SECOND;
	public static final int COOLDOWN_TICKS_1 = 30 * Constants.TICKS_PER_SECOND;
	public static final int COOLDOWN_TICKS_2 = 25 * Constants.TICKS_PER_SECOND;

	private final int mLevelDamage;
	private final int mLevelSize;
	private final double mLevelSlowMultiplier;

	public Blizzard(Plugin plugin, @Nullable Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = NAME;
		mInfo.mShorthandName = "Bl";
		mInfo.mDescriptions.add("Press the swap key while shifting and on the ground while holing a wand to create a storm of ice and snow that follows the player, dealing 5 magic damage every 2s to all enemies in a 6 block radius around you. The blizzard last for 10s, and chills enemies within it, slowing them by 25%." +
			" Players in the blizzard are extinguished if they are on fire, and the ability's damage bypasses iframes. This ability does not interact with Spellshock. Cooldown: 30s.");
		mInfo.mDescriptions.add("Damage is increased from 5 to 10, aura size is increased from 6 to 8 blocks, slowness increased to 30%.");
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.SNOWBALL, 1);

		boolean isUpgraded = getAbilityScore() == 2;
		mInfo.mCooldown = isUpgraded ? COOLDOWN_TICKS_2 : COOLDOWN_TICKS_1;

		mLevelDamage = isUpgraded ? DAMAGE_2 : DAMAGE_1;
		mLevelSize = isUpgraded ? SIZE_2 : SIZE_1;
		mLevelSlowMultiplier = isUpgraded ? SLOW_MULTIPLIER_2 : SLOW_MULTIPLIER_1;
	}

	private boolean mActive = false;

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (mPlayer != null &&
			ItemUtils.isWand(mPlayer.getInventory().getItemInMainHand())
		) {
			event.setCancelled(true);

			if (!isTimerActive() && !mActive && mPlayer.isSneaking() && mPlayer.isOnGround()) {
				putOnCooldown();
				mActive = true;
				FixedMetadataValue playerItemStats = new FixedMetadataValue(mPlugin, new ItemStatManager.PlayerItemStats(mPlugin.mItemStatManager.getPlayerItemStats(mPlayer)));

				World world = mPlayer.getWorld();
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 2);
				world.playSound(mPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.75f);

				float spellDamage = SpellPower.getSpellDamage(mPlugin, mPlayer, mLevelDamage);
				new BukkitRunnable() {
					int mTicks = 0;

					@Override
					public void run() {
						Location loc = mPlayer.getLocation();
						List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, mLevelSize, mPlayer);
						mTicks++;
						if (mTicks % SLOW_INTERVAL == 0) {
							for (Player p : PlayerUtils.playersInRange(loc, mLevelSize, true)) {
								if (p.getFireTicks() > 1) {
									p.setFireTicks(1);
								}
							}
							for (LivingEntity mob : mobs) {
								EntityUtils.applySlow(mPlugin, SLOW_TICKS, mLevelSlowMultiplier, mob);
							}
						}

						if (mTicks % DAMAGE_INTERVAL == 0) {
							for (LivingEntity mob : mobs) {
								DamageEvent damageEvent = new DamageEvent(mob, mPlayer, mPlayer, DamageType.MAGIC, mInfo.mLinkedSpell, spellDamage);
								damageEvent.setDelayed(true);
								damageEvent.setPlayerItemStat(playerItemStats);
								DamageUtils.damage(damageEvent, false, true, null);
							}
						}

						world.spawnParticle(Particle.SNOWBALL, loc, 6, 2, 2, 2, 0.1);
						world.spawnParticle(Particle.CLOUD, loc, 4, 2, 2, 2, 0.05);
						world.spawnParticle(Particle.CLOUD, loc, 3, 0.1, 0.1, 0.1, 0.15);
						if (
							mTicks >= DURATION_TICKS
								|| AbilityManager.getManager().getPlayerAbility(mPlayer, Blizzard.class) == null
								|| !mPlayer.isValid() // Ensure player is not dead, is still online?
						) {
							this.cancel();
							mActive = false;
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}
		}
	}
}
