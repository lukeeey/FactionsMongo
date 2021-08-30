package io.github.lukeeey.factionsmongodb.storage;

import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.data.MemoryFaction;
import com.massivecraft.factions.data.MemoryFactions;
import com.massivecraft.factions.data.json.JSONFaction;
import com.massivecraft.factions.data.json.JSONFactions;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Collectors;

public class MongoFactions extends MemoryFactions {
    private static JSONFactions jsonFactionsInstance;
    private MongoCollection<Document> collection;
    // -------------------------------------------- //
    // CONSTRUCTORS
    // -------------------------------------------- //

    public MongoFactions(MongoCollection<Document> collection) {
        this.collection = collection;
        this.nextId = 1;
        jsonFactionsInstance = (JSONFactions) Factions.instance;
        Factions.instance = this;
    }

    @Override
    public void forceSave() {
        forceSave(true);
    }

    @Override
    public void forceSave(boolean sync) {
        ReplaceOptions options = new ReplaceOptions().upsert(true);
        for (Faction entity : this.factions.values()) {
            Document document = Document.parse(FactionsPlugin.getInstance().getGson().toJson((MongoFaction) entity));
            if (document != null) {
                collection.replaceOne(Filters.eq("id", entity.getId()), document, options);
            }
        }
    }

    public void convert() {
        ReplaceOptions options = new ReplaceOptions().upsert(true);
        JSONFactions jsonFactions = jsonFactionsInstance;
        try {
            Method method = jsonFactions.getClass().getDeclaredMethod("loadCore");
            method.setAccessible(true);

            Map<String, JSONFaction> factions = (Map<String, JSONFaction>) method.invoke(jsonFactions);
            factions.forEach((id, faction) -> {
                Document document = Document.parse(FactionsPlugin.getInstance().getGson().toJson(faction));
                if (document != null) {
                    collection.replaceOne(Filters.eq("id", id), document, options);
                }
            });
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int load() {
        MongoCursor<Document> iterator = collection.find().iterator();
        while (iterator.hasNext()) {
            Document doc = iterator.next();
            Faction faction = FactionsPlugin.getInstance().getGson().fromJson(doc.toJson(), new TypeToken<MongoFaction>() {}.getType());

            faction.checkPerms();
            faction.setId(faction.getId());

            this.updateNextIdForId(faction.getId());
            this.factions.put(faction.getId(), faction);
        }
        super.load();
        return factions.size();
    }

    // -------------------------------------------- //
    // ID MANAGEMENT
    // -------------------------------------------- //

    public String getNextId() {
        while (!isIdFree(this.nextId)) {
            this.nextId += 1;
        }
        return Integer.toString(this.nextId);
    }

    public boolean isIdFree(String id) {
        return !this.factions.containsKey(id);
    }

    public boolean isIdFree(int id) {
        return this.isIdFree(Integer.toString(id));
    }

    protected synchronized void updateNextIdForId(int id) {
        if (this.nextId < id) {
            this.nextId = id + 1;
        }
    }

    protected void updateNextIdForId(String id) {
        try {
            int idAsInt = Integer.parseInt(id);
            this.updateNextIdForId(idAsInt);
        } catch (Exception ignored) {
        }
    }

    @Override
    public Faction generateFactionObject() {
        String id = getNextId();
        Faction faction = new MongoFaction(id);
        updateNextIdForId(id);
        return faction;
    }

    @Override
    public Faction generateFactionObject(String id) {
        return new MongoFaction(id);
    }

    @Override
    public void convertFrom(MemoryFactions old) {
        old.factions.forEach((tag, faction) -> this.factions.put(tag, new MongoFaction((MemoryFaction) faction)));
        this.nextId = old.nextId;
        forceSave();
        Factions.instance = this;
    }
}