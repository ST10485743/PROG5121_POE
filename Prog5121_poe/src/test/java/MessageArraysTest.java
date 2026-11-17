import com.mycompany.prog5121_poe.Message;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.io.TempDir;


/**
 *
 * @author leago
 */
public class MessageArraysTest {

    private static final String MSG2_TEXT = "Where are you? You are late! I have asked you to be on time.";

    @TempDir
    Path tempDir;

    private Path storageFile;
    private Message message1;
    private Message message2;
    private Message message3;
    private Message message4;
    private Message message5;

    @BeforeEach
    void setUp() {
        Message.resetSession();
        storageFile = tempDir.resolve("storedMessages.json");
        Message.setStoragePath(storageFile);
    }

    @AfterEach
    void tearDown() {
        Message.resetSession();
    }

    // Creates a message from the brief's test data and applies its flag.
    private Message addTestMessage(int number, String recipient, String text, int flag) {
        Message message = new Message(recipient, text, number);
        message.sentMessage(flag);
        return message;
    }

    private void populateMessages1To4() {
        message1 = addTestMessage(0, "+27834557896", "Did you get the cake?", 1);
        message2 = addTestMessage(1, "+27838884567", MSG2_TEXT, 3);
        message3 = addTestMessage(2, "+27834484567", "Yohoooo, I am at your gate.", 2);
        message4 = addTestMessage(3, "0838884567", "It is dinner time!", 1);
    }

    private void populateMessages1To5() {
        populateMessages1To4();
        message5 = addTestMessage(4, "+27838884567", "Ok, I am leaving without you.", 3);
    }

    // ---------- Arrays populated ----------

    @Test
    void sentMessagesArray_CorrectlyPopulated() {
        populateMessages1To4();
        assertArrayEquals(new String[]{"Did you get the cake?", "It is dinner time!"},
                Message.getSentMessageTexts());
    }

    @Test
    void disregardedMessagesArray_CorrectlyPopulated() {
        populateMessages1To5();
        assertArrayEquals(new String[]{"Yohoooo, I am at your gate."},
                Message.getDisregardedMessageTexts());
    }

    @Test
    void storedMessagesArray_ReadFromJsonFile() {
        populateMessages1To5();
        assertTrue(Files.exists(storageFile));
        assertArrayEquals(new String[]{MSG2_TEXT, "Ok, I am leaving without you."},
                Message.getStoredMessageTexts());
    }

    @Test
    void messageHashAndIdArrays_ContainAllMessagesInParallel() {
        populateMessages1To5();
        Message[] all = {message1, message2, message3, message4, message5};

        String[] hashes = Message.getMessageHashes();
        String[] ids = Message.getMessageIDs();
        assertEquals(5, hashes.length);
        assertEquals(5, ids.length);
        for (int i = 0; i < all.length; i++) {
            assertEquals(all[i].getMessageHash(), hashes[i]);
            assertEquals(all[i].getMessageID(), ids[i]);
        }
    }

    // ---------- Display the longest message ----------

    @Test
    void longestStoredMessage_ReturnsMessage2Text() {
        populateMessages1To4();
        assertEquals(MSG2_TEXT, Message.longestStoredMessage());
    }

    @Test
    void longestStoredMessage_NoStoredMessages_ReturnsNotFoundMessage() {
        assertEquals("No stored messages found.", Message.longestStoredMessage());
    }

    // ---------- Search for message ID ----------

    @Test
    void findByMessageID_Message4_ReturnsItsRecipientAndMessage() {
        populateMessages1To4();
        Message found = Message.findByMessageID(message4.getMessageID());
        assertNotNull(found);
        assertEquals("0838884567", found.getRecipient());
        assertEquals("It is dinner time!", found.getMessageText());
        assertEquals("Recipient: 0838884567, Message: It is dinner time!",
                Message.searchByMessageID(message4.getMessageID()));
    }

    @Test
    void searchByMessageID_UnknownID_ReturnsNotFoundMessage() {
        populateMessages1To4();
        assertEquals("No message found with ID 0000000000.",
                Message.searchByMessageID("0000000000"));
    }

    // ---------- Search all messages for a recipient ----------

    @Test
    void searchByRecipient_ReturnsMessages2And5() {
        populateMessages1To5();
        assertArrayEquals(new String[]{MSG2_TEXT, "Ok, I am leaving without you."},
                Message.searchByRecipient("+27838884567"));
    }

    @Test
    void searchByRecipient_UnknownRecipient_ReturnsEmptyArray() {
        populateMessages1To5();
        assertEquals(0, Message.searchByRecipient("+27000000000").length);
    }

    // ---------- Delete a message using its hash ----------

    @Test
    void deleteByMessageHash_Message2_RemovesItEverywhere() throws IOException {
        populateMessages1To5();
        String id2 = message2.getMessageID();

        String result = Message.deleteByMessageHash(message2.getMessageHash());

        assertEquals("Message: \"" + MSG2_TEXT + "\" successfully deleted.", result);
        assertArrayEquals(new String[]{"Ok, I am leaving without you."},
                Message.getStoredMessageTexts());
        assertEquals(4, Message.getMessageHashes().length);
        assertFalse(String.join(",", Message.getMessageIDs()).contains(id2));
        assertFalse(Files.readString(storageFile).contains("Where are you?"));

        // The deletion survives a re-read of the file
        Message.refreshStoredMessages();
        assertArrayEquals(new String[]{"Ok, I am leaving without you."},
                Message.getStoredMessageTexts());
    }

    @Test
    void deleteByMessageHash_UnknownHash_ReturnsNotFoundMessage() {
        populateMessages1To5();
        assertEquals("No stored message found with hash 99:9:NOPE.",
                Message.deleteByMessageHash("99:9:NOPE"));
        assertEquals(2, Message.getStoredMessageTexts().length);
    }

    // ---------- Reports and sender/recipient display ----------

    @Test
    void sentMessagesReport_ListsSentMessagesWithHashRecipientAndMessage() {
        populateMessages1To5();
        String report = Message.sentMessagesReport();

        assertTrue(report.contains(message1.getMessageHash()));
        assertTrue(report.contains("+27834557896"));
        assertTrue(report.contains("Did you get the cake?"));
        assertTrue(report.contains(message4.getMessageHash()));
        assertTrue(report.contains("It is dinner time!"));
        assertFalse(report.contains("Where are you?"));
    }

    @Test
    void storedMessagesReport_ListsFullDetailsOfStoredMessages() {
        populateMessages1To5();
        String report = Message.storedMessagesReport();

        assertTrue(report.contains(message2.getMessageID()));
        assertTrue(report.contains(message2.getMessageHash()));
        assertTrue(report.contains("+27838884567"));
        assertTrue(report.contains(MSG2_TEXT));
        assertTrue(report.contains("Ok, I am leaving without you."));
        assertFalse(report.contains("Did you get the cake?"));
    }

    @Test
    void displayStoredSendersAndRecipients_ListsSenderAndEachRecipient() {
        populateMessages1To5();
        String display = Message.displayStoredSendersAndRecipients("Kyle Peters");

        assertEquals(2, display.split(System.lineSeparator()).length);
        assertTrue(display.contains("Sender: Kyle Peters | Recipient: +27838884567"));
    }

    // ---------- Reading the JSON file ----------

    @Test
    void refreshStoredMessages_PrettyPrintedExternalFile_IsReadIntoArray() throws IOException {
        Files.writeString(storageFile, "[\n"
                + "  {\n"
                + "    \"messageID\" : \"1234567890\",\n"
                + "    \"messageHash\": \"12:0:HITONIGHT\",\n"
                + "    \"recipient\": \"+27718693002\",\n"
                + "    \"message\": \"Hi Mike, can you join us for dinner tonight?\",\n"
                + "    \"extra\": 42\n"
                + "  },\n"
                + "  { \"messageID\": \"9876543210\", \"recipient\": \"+27834557896\", \"message\": \"Hello\" }\n"
                + "]\n");

        Message.refreshStoredMessages();

        assertArrayEquals(new String[]{"Hi Mike, can you join us for dinner tonight?", "Hello"},
                Message.getStoredMessageTexts());
        assertEquals("12:0:HITONIGHT", Message.getMessageHashes()[0]);
        assertEquals("1234567890", Message.getMessageIDs()[0]);
    }

    @Test
    void storeMessage_UnreadableFile_ReturnsErrorAndLeavesFileUntouched() throws IOException {
        Files.writeString(storageFile, "{ this is not valid json");
        Message message = new Message("+27718693002", "Hello there", 0);

        assertEquals("Message could not be stored.", message.sentMessage(3));
        assertEquals("{ this is not valid json", Files.readString(storageFile));
    }

    @Test
    void storeMessage_TextWithQuotesAndBraces_SurvivesRoundTrip() {
        String tricky = "He said \"call me\" {urgent} C:\\temp & <ok>";
        addTestMessage(0, "+27718693002", tricky, 3);

        Message.refreshStoredMessages();

        assertArrayEquals(new String[]{tricky}, Message.getStoredMessageTexts());
    }
    
}
