package io.github.lukeeey.factionsmongodb.storage;

import com.massivecraft.factions.data.MemoryFaction;

public class MongoFaction extends MemoryFaction {

    public MongoFaction(MemoryFaction arg0) {
        super(arg0);
    }

    private MongoFaction() {
    }

    public MongoFaction(String id) {
        super(id);
    }
}
