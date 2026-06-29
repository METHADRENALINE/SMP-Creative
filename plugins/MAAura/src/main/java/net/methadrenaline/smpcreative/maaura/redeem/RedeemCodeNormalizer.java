package net.methadrenaline.smpcreative.maaura.redeem;

import java.util.Locale;
import java.util.regex.Pattern;

public final class RedeemCodeNormalizer {
    private static final Pattern REDEEM_CODE_PATTERN = Pattern.compile("[A-Z0-9]{5}(-[A-Z0-9]{5}){4}");

    private RedeemCodeNormalizer() {
    }

    public static String normalize(String code) {
        if (code == null) {
            return "";
        }

        String normalized = code.trim()
                .toUpperCase(Locale.ROOT)
                .replace('\u2010', '-')
                .replace('\u2011', '-')
                .replace('\u2012', '-')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replaceAll("[^A-Z0-9-]", "");
        if (normalized.matches("[A-Z0-9]{25}")) {
            return normalized.substring(0, 5) + "-" + normalized.substring(5, 10) + "-"
                    + normalized.substring(10, 15) + "-" + normalized.substring(15, 20) + "-"
                    + normalized.substring(20, 25);
        }
        return REDEEM_CODE_PATTERN.matcher(normalized).matches() ? normalized : "";
    }
}
