package jp.evolvegame.core;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProjectEvolvePlugin extends JavaPlugin {
    private MatchManager matchManager;
    private TestMonsterController testMonsterController;
    private MonsterGameplayController monsterGameplayController;
    private MonsterCameraController monsterCameraController;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.matchManager = new MatchManager(this);
        this.testMonsterController = new TestMonsterController(this, matchManager);
        this.monsterCameraController = new MonsterCameraController(this, matchManager, testMonsterController);
        this.testMonsterController.setCameraController(monsterCameraController);
        this.monsterGameplayController = new MonsterGameplayController(this, matchManager, testMonsterController);

        EvolveCommand command = new EvolveCommand(this, matchManager, testMonsterController, monsterGameplayController);
        PluginCommand evolve = getCommand("evolve");
        if (evolve != null) {
            evolve.setExecutor(command);
            evolve.setTabCompleter(command);
        }

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this, matchManager), this);
        Bukkit.getPluginManager().registerEvents(testMonsterController, this);
        Bukkit.getPluginManager().registerEvents(monsterGameplayController, this);
        getLogger().info("Project EVOLVE v0.3.2 enabled (experimental Monster TPS camera). ");
    }

    @Override
    public void onDisable() {
        if (monsterGameplayController != null) monsterGameplayController.clearAll();
        if (monsterCameraController != null) monsterCameraController.shutdown();
        if (testMonsterController != null) testMonsterController.shutdown();
        if (matchManager != null) matchManager.shutdown();
    }
}
