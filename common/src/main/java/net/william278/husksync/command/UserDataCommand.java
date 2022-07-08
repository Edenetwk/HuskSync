package net.william278.husksync.command;

import net.william278.husksync.HuskSync;
import net.william278.husksync.player.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class UserDataCommand extends CommandBase implements TabCompletable {

    private final String[] COMMAND_ARGUMENTS = {"view", "list", "delete", "restore"};

    public UserDataCommand(@NotNull HuskSync implementor) {
        super("userdata", Permission.COMMAND_USER_DATA, implementor, "playerdata");
    }

    @Override
    public void onExecute(@NotNull OnlineUser player, @NotNull String[] args) {
        if (args.length < 1) {
            plugin.getLocales().getLocale("error_invalid_syntax",
                            "/userdata <view|list|delete|restore> <username> [version_uuid]")
                    .ifPresent(player::sendMessage);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "view" -> {
                if (args.length < 2) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata view <username> [version_uuid]")
                            .ifPresent(player::sendMessage);
                    return;
                }
                final String username = args[1];
                if (args.length >= 3) {
                    try {
                        final UUID versionUuid = UUID.fromString(args[2]);
                        CompletableFuture.runAsync(() -> plugin.getDatabase().getUserByName(username.toLowerCase()).thenAccept(
                                optionalUser -> optionalUser.ifPresentOrElse(
                                        user -> plugin.getDatabase().getUserData(user).thenAccept(
                                                userDataList -> userDataList.stream().filter(versionedUserData -> versionedUserData
                                                                .versionUUID().equals(versionUuid))
                                                        .findFirst().ifPresentOrElse(userData ->
                                                                        plugin.getDataEditor()
                                                                                .displayDataOverview(player, userData, user),
                                                                () -> plugin.getLocales().getLocale("error_invalid_version_uuid")
                                                                        .ifPresent(player::sendMessage))),
                                        () -> plugin.getLocales().getLocale("error_invalid_player")
                                                .ifPresent(player::sendMessage))));
                    } catch (IllegalArgumentException e) {
                        plugin.getLocales().getLocale("error_invalid_syntax",
                                        "/userdata view <username> [version_uuid]")
                                .ifPresent(player::sendMessage);
                    }
                } else {
                    CompletableFuture.runAsync(() -> plugin.getDatabase().getUserByName(username.toLowerCase()).thenAccept(
                            optionalUser -> optionalUser.ifPresentOrElse(
                                    user -> plugin.getDatabase().getCurrentUserData(user).thenAccept(
                                            latestData -> latestData.ifPresentOrElse(
                                                    userData -> plugin.getDataEditor()
                                                            .displayDataOverview(player, userData, user),
                                                    () -> plugin.getLocales().getLocale("error_no_data_to_display")
                                                            .ifPresent(player::sendMessage))),
                                    () -> plugin.getLocales().getLocale("error_invalid_player")
                                            .ifPresent(player::sendMessage))));
                }
            }
            case "list" -> {
                if (args.length < 2) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata list <username>")
                            .ifPresent(player::sendMessage);
                    return;
                }
                final String username = args[1];
                CompletableFuture.runAsync(() -> plugin.getDatabase().getUserByName(username.toLowerCase()).thenAccept(
                        optionalUser -> optionalUser.ifPresentOrElse(
                                user -> plugin.getDatabase().getUserData(user).thenAccept(dataList -> {
                                    if (dataList.isEmpty()) {
                                        plugin.getLocales().getLocale("error_no_data_to_display")
                                                .ifPresent(player::sendMessage);
                                        return;
                                    }
                                    plugin.getDataEditor().displayDataList(player, dataList, user);
                                }),
                                () -> plugin.getLocales().getLocale("error_invalid_player")
                                        .ifPresent(player::sendMessage))));
            }
            case "delete" -> {

            }
            case "restore" -> {

            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull OnlineUser player, @NotNull String[] args) {
        return Arrays.stream(COMMAND_ARGUMENTS)
                .filter(argument -> argument.startsWith(args.length >= 1 ? args[0] : ""))
                .sorted().collect(Collectors.toList());
    }
}