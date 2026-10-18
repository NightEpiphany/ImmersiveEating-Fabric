package io.github.memorytoame.immersiveeating.fabric.additions.sound;

import com.google.gson.JsonObject;
import io.github.memorytoame.immersiveeating.fabric.KaleidoscopeImmersiveEating;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class FoodDynamicPack implements PackResources {

   private static final PackMetadataSection METADATA = new PackMetadataSection(Component.literal("Provides models and animations."), 15);

   @Override

   public IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull ResourceLocation location) {
      if (type != PackType.CLIENT_RESOURCES) {
         return null;
      }
      if (!location.getNamespace().equals(KaleidoscopeImmersiveEating.MOD_ID)) {
         return null;
      }
      if (!location.getPath().equals("sounds.json")) {
         return null;
      }
      String content = createSoundsJson().toString();

      return () -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)
      );
   }

   @Override

   public @NotNull Set<String> getNamespaces(@NotNull PackType type) {
      if (type == PackType.CLIENT_RESOURCES) {
         return Set.of(KaleidoscopeImmersiveEating.MOD_ID);
      }
      return Set.of();
   }

   @Override

   public void listResources(@NotNull PackType type, @NotNull String namespace, @NotNull String prefix, @NotNull ResourceOutput output) {
      if (type != PackType.CLIENT_RESOURCES) {
         return;
      }
      if (!namespace.equals(KaleidoscopeImmersiveEating.MOD_ID)) {
         return;
      }
      if ("sounds.json".startsWith(prefix)) {
         output.accept(new ResourceLocation(KaleidoscopeImmersiveEating.MOD_ID, "sounds.json"),
                 () -> {
                    JsonObject json = createSoundsJson();
                    return new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)
                    );
                 }
         );
      }
   }

   @Override

   public IoSupplier<InputStream> getRootResource(String... path) {
      if (path.length == 1 && "pack.png".equals(path[0])) {
         return () -> {
             return FoodDynamicPack.class.getResourceAsStream("/pack.png");
         };
      }
      return null;
   }

   @Override

   public <T> T getMetadataSection(@NotNull MetadataSectionSerializer<T> serializer) {
      if (serializer == PackMetadataSection.TYPE) {
         return (T) METADATA;
      }
      return null;
   }
   @Override

   public @NotNull String packId() {
      return "food_dynamic";
   }
   private static JsonObject createSoundsJson() {
      return SoundJson.generate(Minecraft.getInstance().getResourceManager());
   }
   @Override

   public void close() {
   }
}

