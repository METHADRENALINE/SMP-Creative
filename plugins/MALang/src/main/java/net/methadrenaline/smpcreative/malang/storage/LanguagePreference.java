package net.methadrenaline.smpcreative.malang.storage;

record LanguagePreference(String language, String source) {
    boolean manual() {
        return "manual".equals(source);
    }
}
