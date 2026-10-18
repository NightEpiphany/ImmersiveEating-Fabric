package io.github.memorytoame.immersiveeating.fabric.additions.sound;

import com.google.gson.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("unchecked")
public class FoodDynamicPack implements PackResources {
   private static final PackMetadataSection METADATA = new PackMetadataSection(
           Component.literal("Provides models and animations."), PackFormat.of(97).minorRange()
   );
   private static final PackLocationInfo LOCATION = new PackLocationInfo(
           "food_dynamic", Component.literal("Kaleidoscope ImmersiveEating Resources"), PackSource.BUILT_IN, Optional.empty()
   );

   public IoSupplier<InputStream> getResource(@NonNull PackType type, @NonNull Identifier location) {
      if (type != PackType.CLIENT_RESOURCES) {
         return null;
      }

      if (!location.getNamespace().equals("food")) {
         return null;
      }

      if (!location.getPath().equals("sounds.json")) {
         return null;
      }

      String content = createSoundsJson().toString();
      return () -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
   }

   @NotNull
   public Set<String> getNamespaces(@NotNull PackType type) {
      return type == PackType.CLIENT_RESOURCES ? Set.of("food") : Set.of();
   }

   public void listResources(@NonNull PackType type, @NotNull String namespace, @NonNull String prefix, @NonNull ResourceOutput output) {
      if (type == PackType.CLIENT_RESOURCES) {
         if (namespace.equals("food")) {
            if ("sounds.json".startsWith(prefix)) {
               output.accept(Identifier.fromNamespaceAndPath("food", "sounds.json"), (IoSupplier)() -> {
                  JsonObject json = createSoundsJson();
                  return new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8));
               });
            }
         }
      }
   }

   public IoSupplier<InputStream> getRootResource(String... path) {
      return path.length == 1 && path[0].equals("pack.png") ? () ->
              Objects.requireNonNull(FoodDynamicPack.class.getResourceAsStream("/assets/food/pack.png")) : null;
   }

   @Override
   public @Nullable <T> T getMetadataSection(@NonNull MetadataSectionType<T> serializer) throws IOException {
      return (T)(serializer == PackMetadataSection.CLIENT_TYPE ? METADATA : null);
   }

   @Override
   public @NonNull PackLocationInfo location() {
      return LOCATION;
   }

   @Override
   public @NonNull String packId() {
      return "food_dynamic";
   }

   private static JsonObject createSoundsJson() {
      return SoundJson.generate(Minecraft.getInstance().getResourceManager());
   }

   public void close() {
   }
}

