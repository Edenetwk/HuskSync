package net.william278.husksync.editor;

import net.william278.husksync.command.Permission;
import net.william278.husksync.config.Locales;
import net.william278.husksync.data.AdvancementData;
import net.william278.husksync.data.ItemData;
import net.william278.husksync.data.VersionedUserData;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Provides methods for displaying and editing user data
 */
public class DataEditor {

    /**
     * Map of currently open inventory and ender chest data editors
     */
    @NotNull
    protected final HashMap<UUID, ItemEditorMenu> openInventoryMenus;

    private final Locales locales;

    public DataEditor(@NotNull Locales locales) {
        this.openInventoryMenus = new HashMap<>();
        this.locales = locales;
    }

    /**
     * Open an inventory or ender chest editor menu
     *
     * @param user           The online user to open the editor for
     * @param itemEditorMenu The {@link ItemEditorMenu} to open
     * @see ItemEditorMenu#createInventoryMenu(ItemData, User, OnlineUser, Locales, boolean)
     * @see ItemEditorMenu#createEnderChestMenu(ItemData, User, OnlineUser, Locales, boolean)
     */
    public CompletableFuture<ItemData> openItemEditorMenu(@NotNull OnlineUser user,
                                                          @NotNull ItemEditorMenu itemEditorMenu) {
        this.openInventoryMenus.put(user.uuid, itemEditorMenu);
        return itemEditorMenu.showInventory(user);
    }

    /**
     * Close an inventory or ender chest editor menu
     *
     * @param user     The online user to close the editor for
     * @param itemData the {@link ItemData} contained within the menu at the time of closing
     */
    public void closeInventoryMenu(@NotNull OnlineUser user, @NotNull ItemData itemData) {
        if (this.openInventoryMenus.containsKey(user.uuid)) {
            this.openInventoryMenus.get(user.uuid).closeInventory(itemData);
        }
        this.openInventoryMenus.remove(user.uuid);
    }

    /**
     * Returns whether edits to the inventory or ender chest menu are allowed
     *
     * @param user The online user with an inventory open to check
     * @return {@code true} if edits to the inventory or ender chest menu are allowed; {@code false} otherwise, including if they don't have an inventory open
     */
    public boolean cancelInventoryEdit(@NotNull OnlineUser user) {
        if (this.openInventoryMenus.containsKey(user.uuid)) {
            return !this.openInventoryMenus.get(user.uuid).canEdit;
        }
        return false;
    }

    /**
     * Display a chat menu detailing information about {@link VersionedUserData}
     *
     * @param user      The online user to display the message to
     * @param userData  The {@link VersionedUserData} to display information about
     * @param dataOwner The {@link User} who owns the {@link VersionedUserData}
     */
    public void displayDataOverview(@NotNull OnlineUser user, @NotNull VersionedUserData userData,
                                    @NotNull User dataOwner) {
        locales.getLocale("data_manager_title",
                        dataOwner.username, dataOwner.uuid.toString())
                .ifPresent(user::sendMessage);
        locales.getLocale("data_manager_versioning",
                        new SimpleDateFormat("MMM dd yyyy, HH:mm:ss.sss").format(userData.versionTimestamp()),
                        userData.versionUUID().toString().split("-")[0],
                        userData.versionUUID().toString(),
                        userData.cause().name().toLowerCase().replaceAll("_", " "))
                .ifPresent(user::sendMessage);
        locales.getLocale("data_manager_status",
                        Double.toString(userData.userData().getStatusData().health),
                        Double.toString(userData.userData().getStatusData().maxHealth),
                        Double.toString(userData.userData().getStatusData().hunger),
                        Integer.toString(userData.userData().getStatusData().expLevel),
                        userData.userData().getStatusData().gameMode.toLowerCase())
                .ifPresent(user::sendMessage);
        locales.getLocale("data_manager_advancements_statistics",
                        Integer.toString(userData.userData().getAdvancementData().size()),
                        generateAdvancementPreview(userData.userData().getAdvancementData()),
                        String.format("%.2f", (((userData.userData().getStatisticsData().untypedStatistics.getOrDefault(
                                "PLAY_ONE_MINUTE", 0)) / 20d) / 60d) / 60d))
                .ifPresent(user::sendMessage);
        if (user.hasPermission(Permission.COMMAND_INVENTORY.node)
            && user.hasPermission(Permission.COMMAND_ENDER_CHEST.node)) {
            locales.getLocale("data_manager_item_buttons",
                            dataOwner.username, userData.versionUUID().toString())
                    .ifPresent(user::sendMessage);
        }
        if (user.hasPermission(Permission.COMMAND_USER_DATA_MANAGE.node)) {
            locales.getLocale("data_manager_management_buttons",
                            dataOwner.username, userData.versionUUID().toString())
                    .ifPresent(user::sendMessage);
        }
    }

    private @NotNull String generateAdvancementPreview(@NotNull List<AdvancementData> advancementData) {
        final StringJoiner joiner = new StringJoiner("\n");
        final int PREVIEW_SIZE = 8;
        for (int i = 0; i < advancementData.size(); i++) {
            joiner.add(advancementData.get(i).key);
            if (i >= PREVIEW_SIZE) {
                break;
            }
        }
        final int remainingAdvancements = advancementData.size() - PREVIEW_SIZE;
        if (remainingAdvancements > 0) {
            joiner.add(locales.getRawLocale("data_manager_advancement_preview_remaining",
                    Integer.toString(remainingAdvancements)).orElse("+" + remainingAdvancements + "…"));
        }
        return joiner.toString();
    }

    /**
     * Display a chat list detailing a player's saved list of {@link VersionedUserData}
     *
     * @param user         The online user to display the message to
     * @param userDataList The list of {@link VersionedUserData} to display
     * @param dataOwner    The {@link User} who owns the {@link VersionedUserData}
     */
    public void displayDataList(@NotNull OnlineUser user, @NotNull List<VersionedUserData> userDataList,
                                @NotNull User dataOwner) {
        locales.getLocale("data_list_title",
                        dataOwner.username, dataOwner.uuid.toString())
                .ifPresent(user::sendMessage);

        final String[] numberedIcons = "①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳" .split("");
        for (int i = 0; i < Math.min(20, userDataList.size()); i++) {
            final VersionedUserData userData = userDataList.get(i);
            locales.getLocale("data_list_item",
                            numberedIcons[i],
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
                                    .format(userData.versionTimestamp()),
                            userData.versionUUID().toString().split("-")[0],
                            userData.versionUUID().toString(),
                            userData.cause().name().toLowerCase().replaceAll("_", " "),
                            dataOwner.username)
                    .ifPresent(user::sendMessage);
        }
    }

    /**
     * Returns whether the user has an inventory editor menu open
     *
     * @param user {@link OnlineUser} to check
     * @return {@code true} if the user has an inventory editor open; {@code false} otherwise
     */
    public boolean isEditingInventoryData(@NotNull OnlineUser user) {
        return this.openInventoryMenus.containsKey(user.uuid);
    }
}