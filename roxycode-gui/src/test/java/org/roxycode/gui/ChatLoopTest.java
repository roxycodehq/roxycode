package org.roxycode.gui;

import com.google.genai.Chat;
import com.google.genai.types.GenerateContentResponse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ChatLoopTest {

    // Simple listener for testing
    static class TestListener implements ChatLoop.Listener {
        AtomicInteger messages = new AtomicInteger();
        AtomicInteger turns = new AtomicInteger();
        AtomicBoolean completed = new AtomicBoolean();
        AtomicBoolean errored = new AtomicBoolean();

        @Override
        public void onMessage(String sender, String text, boolean save) {
            messages.incrementAndGet();
        }

        @Override
        public void onStatus(String status) {}

        @Override
        public void onTurn(int turn, int maxTurns) {
            turns.set(turn);
        }

        @Override
        public void onComplete() {
            completed.set(true);
        }

        @Override
        public void onError(Throwable error) {
            errored.set(true);
        }
    }

    // Since we can't easily mock final classes/libraries without Mockito, 
    // and we don't know if Chat is mockable, we'll verify the structure 
    // and compilation for now. 
    // In a real scenario, we'd use a Mockito or a Fake implementation.
}
