package com.winthier.connect.message;

import com.cavetale.core.util.Json;
import com.winthier.connect.Connect;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import static net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson;

@Data
public final class PlayerOpenBookMessage {
    public static final String CHANNEL = "connect:player_open_book";

    protected UUID target;
    protected String title;
    protected String author;
    protected List<String> pages;

    public PlayerOpenBookMessage() { }

    public PlayerOpenBookMessage(final UUID target, final ItemStack book) {
        this.target = target;
        if (!(book.getItemMeta() instanceof BookMeta meta)) {
            throw new IllegalArgumentException("not a book");
        }
        this.title = meta.title() != null
            ? gson().serialize(meta.title())
            : null;
        this.author = meta.getAuthor();
        this.pages = new ArrayList<>();
        for (Component page : meta.pages()) {
            pages.add(gson().serialize(page));
        }
    }

    public ItemStack parseBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(m -> {
                if (!(m instanceof BookMeta meta)) return;
                meta.setAuthor(author);
                meta.title(title != null
                           ? gson().deserialize(title)
                           : null);
                List<Component> pages2 = new ArrayList<>(pages.size());
                for (String page : pages) {
                    pages2.add(gson().deserialize(page));
                }
            });
        return book;
    }

    public void send(String targetServer) {
        Connect.getInstance().send(targetServer, CHANNEL, Json.serialize(this));
    }

    public void broadcast() {
        Connect.getInstance().broadcast(CHANNEL, Json.serialize(this));
    }

    public void broadcastAll() {
        Connect.getInstance().broadcastAll(CHANNEL, Json.serialize(this));
    }
}
