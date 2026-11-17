package com.mycompany.prog5121_poe;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
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
    
    private static final List<Message> sentMessages = new ArrayList<>();
    private static final List<Message> disregardedMessages = new ArrayList<>();
    private static final List<Message> storedMessages = new ArrayList<>(); // read from the JSON file
    // messageHashes and messageIDs are parallel: index i in one matches index i in the other.
    private static final List<String> messageHashes = new ArrayList<>();
    private static final List<String> messageIDs = new ArrayList<>();

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

    // Adds this message's ID and hash to the two parallel arrays (once only).
    
    private void registerIdentifiers() {
        if (!messageIDs.contains(messageID)) {
            messageIDs.add(messageID);
            messageHashes.add(messageHash);
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

    // ---------- Part 3: reading the arrays back out ----------

    public static String[] getSentMessageTexts() {
        return textsOf(sentMessages);
    }

    public static String[] getDisregardedMessageTexts() {
        return textsOf(disregardedMessages);
    }

    public static String[] getStoredMessageTexts() {
        return textsOf(storedMessages);
    }

    public static String[] getMessageHashes() {
        return messageHashes.toArray(new String[0]);
    }

    public static String[] getMessageIDs() {
        return messageIDs.toArray(new String[0]);
    }

    private static String[] textsOf(List<Message> messages) {
        String[] texts = new String[messages.size()];
        for (int i = 0; i < texts.length; i++) {
            texts[i] = messages.get(i).messageText;
        }
        return texts;
    }

    // ---------- Part 3: Stored Messages menu features ----------

    // Sender and recipient of every stored message.
    
    public static String displayStoredSendersAndRecipients(String sender) {
        if (storedMessages.isEmpty()) {
            return "No stored messages found.";
        }
        StringBuilder sb = new StringBuilder();
        for (Message m : storedMessages) {
            sb.append("Sender: ").append(sender)
                    .append(" | Recipient: ").append(m.recipient)
                    .append(System.lineSeparator());
        }
        return sb.toString();
    }

    // The longest stored message.
    
    public static String longestStoredMessage() {
        Message longest = null;
        for (Message m : storedMessages) {
            if (longest == null || m.messageText.length() > longest.messageText.length()) {
                longest = m;
            }
        }
        return longest == null ? "No stored messages found." : longest.messageText;
    }

    // Finds a message by ID across the sent, disregarded and stored arrays.
    
    public static Message findByMessageID(String id) {
        if (!messageIDs.contains(id)) {
            return null;
        }
        for (List<Message> messages : List.of(sentMessages, disregardedMessages, storedMessages)) {
            for (Message m : messages) {
                if (m.messageID.equals(id)) {
                    return m;
                }
            }
        }
        return null;
    }

    public static String searchByMessageID(String id) {
        Message found = findByMessageID(id);
        if (found == null) {
            return "No message found with ID " + id + ".";
        }
        return "Recipient: " + found.recipient + ", Message: " + found.messageText;
    }

    // Texts of every sent or stored message for one recipient.
    
    public static String[] searchByRecipient(String recipient) {
        List<String> matches = new ArrayList<>();
        for (List<Message> messages : List.of(sentMessages, storedMessages)) {
            for (Message m : messages) {
                if (m.recipient.equals(recipient)) {
                    matches.add(m.messageText);
                }
            }
        }
        return matches.toArray(new String[0]);
    }

    // Deletes a stored message using its hash. The hash array is searched,
    // the parallel ID array gives the matching ID, and that message is
    // removed from the stored array, the JSON file and both parallel arrays.
    
    public static String deleteByMessageHash(String hash) {
        for (int i = 0; i < messageHashes.size(); i++) {
            if (!messageHashes.get(i).equalsIgnoreCase(hash)) {
                continue;
            }
            String id = messageIDs.get(i);
            for (Message target : storedMessages) {
                if (target.messageID.equals(id)) {
                    List<Message> remaining = new ArrayList<>(storedMessages);
                    remaining.remove(target);
                    try {
                        writeMessages(storagePath, remaining);
                    } catch (IOException e) {
                        return "Message could not be deleted: " + e.getMessage();
                    }
                    storedMessages.remove(target);
                    messageHashes.remove(i);
                    messageIDs.remove(i);
                    return "Message: \"" + target.messageText + "\" successfully deleted.";
                }
            }
        }
        return "No stored message found with hash " + hash + ".";
    }

    // ---------- Part 3: JSON storage (Gson) ----------

    // stores message on the JSON file. 
    // returns true if it was written, false if the file could not be read or written
    
    public boolean storeMessage() {
        try {
            List<Message> all = Files.exists(storagePath)
                    ? readMessages(storagePath) : new ArrayList<>();
            all.add(this);
            writeMessages(storagePath, all);
            return true;
        } catch (IOException | JsonParseException | IllegalStateException e) {
            System.out.println("Could not store message to JSON: " + e.getMessage());
            return false;
        }
    }

    // Reads the JSON file into the stored-messages array
    
    public static void refreshStoredMessages() {
        storedMessages.clear();
        if (!Files.exists(storagePath)) {
            return;
        }
        try {
            for (Message m : readMessages(storagePath)) {
                storedMessages.add(m);
                m.registerIdentifiers();
            }
        } catch (IOException | JsonParseException | IllegalStateException e) {
            System.out.println("Could not read stored messages: " + e.getMessage());
        }
    }

    // Reads messages from any external JSON file
    
    public static Message[] loadMessagesFromFile(String filePath) {
        try {
            return readMessages(Path.of(filePath)).toArray(new Message[0]);
        } catch (IOException | JsonParseException | IllegalStateException e) {
            System.out.println("Could not read " + filePath + ": " + e.getMessage());
            return new Message[0];
        }
    }

    private static List<Message> readMessages(Path path) throws IOException {
        List<Message> messages = new ArrayList<>();
        String text = Files.readString(path);
        if (text.isBlank()) {
            return messages; // an empty file just means no messages yet
        }
        JsonArray array = JsonParser.parseString(text).getAsJsonArray();
        for (JsonElement element : array) {
            messages.add(fromJson(element.getAsJsonObject()));
        }
        return messages;
    }

    private static Message fromJson(JsonObject object) {
        String id = readString(object, "messageID");
        String hash = readString(object, "messageHash");

        Message message = new Message(readString(object, "recipient"),
                readString(object, "message"), 0);
        if (!id.isEmpty()) {
            message.messageID = id;
        }
        // Use the file's hash if it has one, otherwise rebuild it from the file's ID.
        message.messageHash = hash.isEmpty() ? message.createMessageHash() : hash;
        return message;
    }

    private static String readString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString() : "";
    }

    private static void writeMessages(Path path, List<Message> messages) throws IOException {
        JsonArray array = new JsonArray();
        for (Message m : messages) {
            JsonObject object = new JsonObject();
            object.addProperty("messageID", m.messageID);
            object.addProperty("messageHash", m.messageHash);
            object.addProperty("recipient", m.recipient);
            object.addProperty("message", m.messageText);
            array.add(object);
        }
        try (Writer writer = Files.newBufferedWriter(path)) {
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                    .toJson(array, writer);
        }
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
