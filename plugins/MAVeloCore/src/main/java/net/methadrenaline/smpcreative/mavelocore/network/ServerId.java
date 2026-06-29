package net.methadrenaline.smpcreative.mavelocore.network;

import java.util.Locale;

public final class ServerId {
    private ServerId() {
    }

    public static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
    }
}
