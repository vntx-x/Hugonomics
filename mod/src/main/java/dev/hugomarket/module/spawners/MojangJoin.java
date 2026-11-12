package dev.hugomarket.module.spawners;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.util.UUID;

record MojangJoin(MinecraftSessionService service, UUID profileId, String accessToken, String name) {
    static MojangJoin of(Minecraft client) {
        //? if >=1.21.9 {
        /*MinecraftSessionService service = client.services().sessionService();
        *///?} else {
        MinecraftSessionService service = client.getMinecraftSessionService();
        //?}
        User user = client.getUser();
        return new MojangJoin(service, user.getProfileId(), user.getAccessToken(), user.getName());
    }

    // Same proof a vanilla server asks for on login; the access token only goes to Mojang.
    void join(String serverId) throws AuthenticationException {
        service.joinServer(profileId, accessToken, serverId);
    }
}
