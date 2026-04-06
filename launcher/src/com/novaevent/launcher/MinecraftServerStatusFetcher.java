package com.novaevent.launcher;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class MinecraftServerStatusFetcher {
    private static final Gson GSON = new Gson();

    public record ServerAddress(String host, int port) {
    }

    public record ServerStatus(
            String displayName,
            int onlinePlayers,
            int maxPlayers,
            long pingMillis,
            BufferedImage icon,
            String rawDescription
    ) {
    }

    public static ServerAddress parseAddress(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) {
            return null;
        }
        if (value.startsWith("[")) {
            int closing = value.indexOf(']');
            if (closing > 0) {
                String host = value.substring(1, closing);
                int port = 25565;
                if (closing + 1 < value.length() && value.charAt(closing + 1) == ':') {
                    port = parsePort(value.substring(closing + 2), 25565);
                }
                return new ServerAddress(host, port);
            }
        }
        int firstColon = value.indexOf(':');
        int lastColon = value.lastIndexOf(':');
        if (firstColon > 0 && firstColon == lastColon) {
            return new ServerAddress(value.substring(0, firstColon).trim(), parsePort(value.substring(firstColon + 1), 25565));
        }
        return new ServerAddress(value, 25565);
    }

    public ServerStatus fetch(String rawAddress, int connectTimeoutMillis, int readTimeoutMillis) throws Exception {
        ServerAddress address = parseAddress(rawAddress);
        if (address == null) {
            throw new IOException("Server address is empty.");
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(address.host(), address.port()), connectTimeoutMillis);
            socket.setSoTimeout(readTimeoutMillis);

            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            DataInputStream input = new DataInputStream(socket.getInputStream());

            ByteArrayOutputStream handshakeBytes = new ByteArrayOutputStream();
            DataOutputStream handshake = new DataOutputStream(handshakeBytes);
            writeVarInt(handshake, 0);
            writeVarInt(handshake, 760);
            writeString(handshake, address.host());
            handshake.writeShort(address.port());
            writeVarInt(handshake, 1);
            writePacket(output, handshakeBytes.toByteArray());

            writePacket(output, new byte[]{0x00});

            byte[] responsePacket = readPacket(input);
            DataInputStream response = new DataInputStream(new ByteArrayInputStream(responsePacket));
            int packetId = readVarInt(response);
            if (packetId != 0) {
                throw new IOException("Unexpected server status packet: " + packetId);
            }
            String json = readString(response);
            JsonObject status = GSON.fromJson(json, JsonObject.class);

            long now = System.currentTimeMillis();
            ByteArrayOutputStream pingBytes = new ByteArrayOutputStream();
            DataOutputStream ping = new DataOutputStream(pingBytes);
            writeVarInt(ping, 1);
            ping.writeLong(now);
            writePacket(output, pingBytes.toByteArray());
            readPacket(input);
            long pingMillis = Math.max(0L, System.currentTimeMillis() - now);

            JsonObject players = status.has("players") && status.get("players").isJsonObject()
                    ? status.getAsJsonObject("players")
                    : new JsonObject();
            int online = players.has("online") ? players.get("online").getAsInt() : 0;
            int max = players.has("max") ? players.get("max").getAsInt() : 0;
            String rawDescription = extractRawDescription(status.get("description"));
            String displayName = stripFormatting(rawDescription);
            if (displayName.isBlank()) {
                displayName = address.host() + ":" + address.port();
            }

            BufferedImage icon = decodeIcon(status.has("favicon") ? status.get("favicon").getAsString() : "");
            return new ServerStatus(displayName, online, max, pingMillis, icon, rawDescription);
        }
    }

    private static String extractDescription(JsonElement element) {
        return stripFormatting(extractRawDescription(element));
    }

    private static String extractRawDescription(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            StringBuilder builder = new StringBuilder();
            appendTextObject(builder, object);
            return builder.toString();
        }
        return element.toString();
    }

    private static void appendTextObject(StringBuilder builder, JsonObject object) {
        if (object.has("text")) {
            builder.append(object.get("text").getAsString());
        }
        if (object.has("extra") && object.get("extra").isJsonArray()) {
            JsonArray extra = object.getAsJsonArray("extra");
            for (JsonElement child : extra) {
                if (child.isJsonObject()) {
                    appendTextObject(builder, child.getAsJsonObject());
                } else if (child.isJsonPrimitive()) {
                    builder.append(child.getAsString());
                }
            }
        }
    }

    private static BufferedImage decodeIcon(String favicon) {
        if (favicon == null || favicon.isBlank()) {
            return null;
        }
        int commaIndex = favicon.indexOf(',');
        if (commaIndex < 0) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(favicon.substring(commaIndex + 1));
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void writePacket(DataOutputStream output, byte[] packetData) throws IOException {
        ByteArrayOutputStream lengthBytes = new ByteArrayOutputStream();
        DataOutputStream lengthOut = new DataOutputStream(lengthBytes);
        writeVarInt(lengthOut, packetData.length);
        output.write(lengthBytes.toByteArray());
        output.write(packetData);
        output.flush();
    }

    private static byte[] readPacket(DataInputStream input) throws IOException {
        int length = readVarInt(input);
        byte[] packet = new byte[length];
        input.readFully(packet);
        return packet;
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(output, bytes.length);
        output.write(bytes);
    }

    private static String readString(DataInputStream input) throws IOException {
        int length = readVarInt(input);
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeVarInt(DataOutputStream output, int value) throws IOException {
        int current = value;
        do {
            byte temp = (byte) (current & 0b0111_1111);
            current >>>= 7;
            if (current != 0) {
                temp |= (byte) 0b1000_0000;
            }
            output.writeByte(temp);
        } while (current != 0);
    }

    private static int readVarInt(DataInputStream input) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            int current = input.read();
            if (current < 0) {
                throw new EOFException("Unexpected end of stream while reading VarInt");
            }
            read = (byte) current;
            int value = (read & 0b0111_1111);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) {
                throw new IOException("VarInt too large");
            }
        } while ((read & 0b1000_0000) != 0);
        return result;
    }

    private static int parsePort(String raw, int fallback) {
        try {
            int port = Integer.parseInt(raw.trim());
            return port > 0 && port <= 65535 ? port : fallback;
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static String stripFormatting(String value) {
        return value == null ? "" : value.replaceAll("\u00A7.", "").trim();
    }
}
