package com.mycompany.prog5121_poe;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// JSON reading and writing uses the Gson library.
// Reference: Google (n.d.) Gson User Guide. Available at:
// https://github.com/google/gson/blob/main/UserGuide.md
// * (Accessed: 24 October 2026).   


// Messages that the user has chosen to send (option 1), shared across
// every Message instance so printMessages()/returnTotalMessages() can
// report on the whole running session.

public class Message {

    private static final Path DEFAULT_STORAGE_PATH = Path.of("storedMessages.json");
    private static Path storagePath = DEFAULT_STORAGE_PATH;

    // ---- Arrays for Part 3 ----
    
    

    private String messageID;
    private String recipient;
    private String messageText;
    private String messageHash;
    private final int messageNumber;

   
    public Message(String recipient, String messageText, int messageNumber) {
        this.recipient = recipient;
        this.messageText = messageText;
        this.messageNumber = messageNumber;
        this.messageID = generateMessageID();
        this.messageHash = createMessageHash();
    }

    // Generates a ten-character numeric ID via string manipulation: a longer
    // random digit string is generated, then cut down to exactly ten
    // characters with substring(), rather than being hard-coded.
    
    private String generateMessageID() {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            digits.append((int) (Math.random() * 10));
        }
        return digits.substring(0, 10);
    }

    // Makes sure that the message ID is not more than ten characters.
    
    public boolean checkMessageID() {
        return messageID != null && messageID.length() <= 10;
    }

    // Confirms the auto-generated ID.
    
    public String announceMessageID() {
        return "Message ID generated: " + messageID;
    }

    // Makes sure that the recipient cell number is no more than 
    // ten characters and starts with an international code.
    
    public String checkRecipientCell() {
        if (recipient != null && Login.CELL_PHONE_PATTERN.matcher(recipient).matches()) {
            return "Cell phone number successfully captured.";
        }
        return "Cell phone number is incorrectly formatted or does not contain "
                + "an international code. Please correct the number and try again.";
    }

    // Ensures the message is no more than 250 characters.
    
    public static String checkMessageLength(String messageText) {
        if (messageText != null && messageText.length() > 250) {
            int excess = messageText.length() - 250;
            return "Message exceeds 250 characters by " + excess + "; please reduce the size.";
        }
        return "Message ready to send.";
    }

    // Builds the message hash: the first two digits of the message ID, a
    //colon, the message's position number in the batch, a colon, then
    //the first and last words of the message.
    
    public String createMessageHash() {
        String idPrefix = (messageID != null && messageID.length() >= 2)
                ? messageID.substring(0, 2) : "";

        String[] words = messageText.trim().split("\\s+");
        String firstWord = stripPunctuation(words[0]);
        String lastWord = stripPunctuation(words[words.length - 1]);

        return (idPrefix + ":" + messageNumber + ":" + firstWord + lastWord).toUpperCase();
    }

    private String stripPunctuation(String word) {
        return word.replaceAll("[^a-zA-Z0-9]", "");
    }

    // Records the user's choice for this message: send, disregard, or
    // store it for later.
    
    public String sentMessage(int choice) {
        switch (choice) {
            case 1:
                sentMessages.add(this);
                registerIdentifiers();
                return "Message successfully sent.";
            case 2:
                disregardedMessages.add(this);
                registerIdentifiers();
                return "Press 0 to delete the message.";
            case 3:
                if (storeMessage()) {
                    // Re-read the JSON file so the stored array always mirrors the
                    // file; this also registers this message's ID and hash.
                    refreshStoredMessages();
                    return "Message successfully stored.";
                }
                return "Message could not be stored.";
            default:
                return "Invalid option selected.";
        }
    }

    

    // ---------- Report / display helpers ----------

    //Returns the full details of every message that has been sent during the run.
    
    public static String printMessages() {
        StringBuilder sb = new StringBuilder();
        for (Message m : sentMessages) {
            sb.append(m.getFullMessageDetails()).append(System.lineSeparator());
        }
        return sb.toString();
    }

    // Report of all sent messages including hash, recipient and message. 
    
    public static String sentMessagesReport() {
        return sentMessages.isEmpty() ? "No messages have been sent." : printMessages();
    }

    // Report that lists the full details of all stored messages.
    
    public static String storedMessagesReport() {
        if (storedMessages.isEmpty()) {
            return "No stored messages found.";
        }
        List<String> details = new ArrayList<>();
        for (Message m : storedMessages) {
            details.add(m.getFullMessageDetails());
        }
        return String.join(System.lineSeparator() + System.lineSeparator(), details);
    }

    // Full details for one message.
    
    public String getFullMessageDetails() {
        return "Message ID: " + messageID + System.lineSeparator()
                + "Message Hash: " + messageHash + System.lineSeparator()
                + "Recipient: " + recipient + System.lineSeparator()
                + "Message: " + messageText;
    }

    public static int returnTotalMessages() {
        return sentMessages.size();
    }

    
    // ---------- Getters ----------

    public String getMessageID() {
        return messageID;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getMessageText() {
        return messageText;
    }

    public String getMessageHash() {
        return messageHash;
    }

    // Test-only: override the auto-generated ID so hash tests are deterministic.
    
    public void setMessageID(String messageID) {
        this.messageID = messageID;
        this.messageHash = createMessageHash();
    }

    // Test-only: point storage at a temporary file.
    
    public static void setStoragePath(Path path) {
        storagePath = path;
    }

    // Test-only: clear session state (and restore the default file) between tests.
    
    public static void resetSession() {
        sentMessages.clear();
        disregardedMessages.clear();
        storedMessages.clear();
        messageHashes.clear();
        messageIDs.clear();
        storagePath = DEFAULT_STORAGE_PATH;
    }
}
