package io.github.lukeeey.factionsmongodb.storage;

import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.data.MemoryFPlayer;
import com.massivecraft.factions.data.MemoryFPlayers;
import com.massivecraft.factions.data.json.JSONFPlayer;
import com.massivecraft.factions.data.json.JSONFPlayers;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class MongoFPlayers extends MemoryFPlayers {
    private static JSONFPlayers jsonFPlayersInstance;
    private MongoCollection<Document> collection;

    public MongoFPlayers(MongoCollection<Document> collection) {
        this.collection = collection;
        jsonFPlayersInstance = (JSONFPlayers) FPlayers.instance;
        FPlayers.instance = this;
    }

    public void convertFrom(MemoryFPlayers old) {
        old.fPlayers.forEach((id, faction) -> this.fPlayers.put(id, new MongoFPlayer((MemoryFPlayer) faction)));
        forceSave();
        FPlayers.instance = this;
    }

    public void forceSave() {
        forceSave(true);
    }

    public void forceSave(boolean sync) {
        ReplaceOptions options = new ReplaceOptions().upsert(true);
        for (FPlayer entity : this.fPlayers.values()) {
            Document document = Document.parse(FactionsPlugin.getInstance().getGson().toJson((MongoFPlayer) entity));
            if (document != null) {
                collection.replaceOne(Filters.eq("id", entity.getId()), document, options);
            }
        }
    }

    public void convert() {
        ReplaceOptions options = new ReplaceOptions().upsert(true);
        JSONFPlayers jsonFPlayers = jsonFPlayersInstance;
        try {
            Method method = jsonFPlayers.getClass().getDeclaredMethod("loadCore");
            method.setAccessible(true);

            Map<String, JSONFPlayer> fplayers = (Map<String, JSONFPlayer>) method.invoke(jsonFPlayers);
            fplayers.forEach((id, fplayer) -> {
                Document document = Document.parse(FactionsPlugin.getInstance().getGson().toJson(fplayer));
                if (document != null) {
                    collection.replaceOne(Filters.eq("id", id), document, options);
                }
            });
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public int load() {
        this.fPlayers.clear();
        MongoCursor<Document> iterator = collection.find().iterator();
        while (iterator.hasNext()) {
            Document doc = iterator.next();
            FPlayer fplayer = FactionsPlugin.getInstance().getGson().fromJson(doc.toJson(), new TypeToken<MongoFPlayer>() { }.getType());

            this.fPlayers.put(fplayer.getId(), fplayer);
        }
        return fPlayers.size();
    }

    @Override
    public FPlayer generateFPlayer(String id) {
        FPlayer player = new MongoFPlayer(id);
        this.fPlayers.put(player.getId(), player);
        return player;
    }
}
