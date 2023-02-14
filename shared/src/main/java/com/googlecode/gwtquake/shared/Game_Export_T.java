package com.googlecode.gwtquake.shared;

import com.googlecode.gwtquake.shared.game.Entity;
import com.googlecode.gwtquake.shared.game.UserCommand;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class Game_Export_T {

    public int apiversion = 20;
    public Runnable Init;
    public Runnable Shutdown;
    public SpawnEntitiesFN SpawnEntities;

    public BiConsumer<String, Boolean> WriteGame;
    public Consumer<String> ReadGame;
    public Consumer<String> WriteLevel;
    public Consumer<String> ReadLevel;

    public BiFunction<Entity, String, Boolean> ClientConnect;
    public Consumer<Entity> ClientBegin;
    public BiFunction<Entity, String, String> ClientUserinfoChanged;

    public Consumer<Entity> ClientDisconnect;
    public Consumer<Entity> ClientCommand;
    public BiConsumer<Entity, UserCommand> ClientThink;

    public Consumer<Entity> RunEntity;

    public Runnable RunFrame;


    @FunctionalInterface
    public interface SpawnEntitiesFN {
        void apply(String mapname, String entities, String spawnpoint);
    }

}
