package pe.bossfights.bosses;

public interface Boss
{
	/* Called only the first time the boss is summoned into the world */
	public void init();

	/* Called when the boss dies */
	public void death();

	/* Called when the chunk the boss is in unloads. Also called after death() */
	public void unload();
}
