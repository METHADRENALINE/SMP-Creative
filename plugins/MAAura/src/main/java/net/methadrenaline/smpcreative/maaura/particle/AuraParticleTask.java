package net.methadrenaline.smpcreative.maaura.particle;

import net.methadrenaline.smpcreative.maaura.MAAuraPlugin;

public final class AuraParticleTask implements Runnable {
    private final MAAuraPlugin plugin;

    public AuraParticleTask(MAAuraPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.tickAuras();
    }
}
