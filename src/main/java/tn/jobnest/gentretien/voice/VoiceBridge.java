package tn.jobnest.gentretien.voice;

import java.util.function.Consumer;

public class VoiceBridge {

    private static Consumer<String> listener;

    public static void register(Consumer<String> l) {
        listener = l;
    }

    public static void dispatch(String text) {
        if(listener != null)
            listener.accept(text);
    }
}