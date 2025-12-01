package com.rotdex.data.manager

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Message types for P2P battle arena communication
 */
enum class MessageType {
    // State sync
    STATE_SYNC_REQUEST,
    STATE_SYNC_RESPONSE,

    // Game actions
    CARD_SELECTED,          // Was: CARDPREVIEW
    IMAGE_TRANSFER,
    IMAGE_REQUEST,          // PHASE 5: Request missing image from opponent
    PLAYER_READY,           // Was: READY
    READY_ACK,
    READY_TIMEOUT,          // Notify opponent of timeout

    // Battle execution
    REVEAL_START,           // Was: REVEAL
    BATTLE_START,
    STORY_SEGMENT,          // Was: BATTLE_STORY
    BATTLE_RESULT,

    // Infrastructure
    ACK,
    PING,
    PONG
}

/**
 * Reliable message with deduplication and acknowledgment support
 *
 * Protocol format:
 * - Regular message: MSG|<messageId>|<type>|<requiresAck>|<sentAt>|<version>|<payload>
 * - ACK message: ACK|<messageId>
 *
 * @param messageId Unique identifier for deduplication (UUID)
 * @param type Message type
 * @param payload Message payload (pipes will be escaped)
 * @param requiresAck Whether this message requires acknowledgment
 * @param sentAt Timestamp when message was created
 * @param version State version (reserved for future state sync)
 */
data class ReliableMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val type: MessageType,
    val payload: String,
    val requiresAck: Boolean = true,
    val sentAt: Long = System.currentTimeMillis(),
    val version: Int = 0
) {
    /**
     * Serialize message to protocol string format
     *
     * Example: MSG|uuid-123|PLAYER_READY|true|1234567890|0|true
     */
    fun toProtocolString(): String {
        return if (type == MessageType.ACK) {
            "ACK|$payload"
        } else {
            // Escape pipe characters in payload
            val escapedPayload = payload.replace("|", "\\|")
            "MSG|$messageId|$type|$requiresAck|$sentAt|$version|$escapedPayload"
        }
    }

    companion object {
        /**
         * Deserialize message from protocol string format
         *
         * @param protocolString The protocol string to parse
         * @return ReliableMessage or null if invalid format
         */
        fun fromProtocolString(protocolString: String): ReliableMessage? {
            return try {
                if (protocolString.startsWith("ACK|")) {
                    // ACK message: ACK|<messageId>
                    val messageId = protocolString.substringAfter("ACK|")
                    ReliableMessage(
                        messageId = UUID.randomUUID().toString(),  // ACKs don't need their own ID
                        type = MessageType.ACK,
                        payload = messageId,
                        requiresAck = false
                    )
                } else if (protocolString.startsWith("MSG|")) {
                    // Regular message: MSG|messageId|type|requiresAck|sentAt|version|payload
                    val parts = protocolString.split("|")
                    if (parts.size < 7) return null

                    val messageId = parts[1]
                    val type = MessageType.valueOf(parts[2])
                    val requiresAck = parts[3].toBoolean()
                    val sentAt = parts[4].toLong()
                    val version = parts[5].toInt()

                    // Payload is everything after the 6th pipe, with escaped pipes unescaped
                    val payload = parts.drop(6).joinToString("|").replace("\\|", "|")

                    ReliableMessage(
                        messageId = messageId,
                        type = type,
                        payload = payload,
                        requiresAck = requiresAck,
                        sentAt = sentAt,
                        version = version
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse protocol string: $protocolString", e)
                null
            }
        }

        private const val TAG = "ReliableMessage"
    }
}

/**
 * Message reliability layer for P2P communication
 *
 * Provides:
 * - Automatic message retries (up to 3 attempts)
 * - Deduplication of received messages
 * - ACK-based confirmation
 * - Memory cleanup of old messages
 *
 * Thread-safe for concurrent message handling.
 *
 * @param sendRaw Function to send raw protocol string to peer
 * @param scope CoroutineScope for async operations (retries, cleanup)
 * @param enableAutoCleanup Whether to automatically clean up old message IDs (default: true)
 */
class MessageReliabilityLayer(
    private val sendRaw: (String) -> Unit,
    private val scope: CoroutineScope,
    private val enableAutoCleanup: Boolean = true
) {
    // Pending messages awaiting ACK (messageId -> message)
    private val pendingMessages = ConcurrentHashMap<String, ReliableMessage>()

    // Received message IDs for deduplication (messageId -> receiveTime)
    private val receivedMessageIds = ConcurrentHashMap<String, Long>()

    // Active retry jobs (messageId -> Job)
    private val retryJobs = ConcurrentHashMap<String, Job>()

    // Cleanup job for old message IDs
    private var cleanupJob: Job? = null

    /**
     * Send a reliable message
     *
     * - Sends message immediately
     * - Queues for retry if requiresAck is true
     * - Retries up to MAX_RETRIES times with RETRY_DELAY_MS between attempts
     *
     * @param message The message to send
     */
    fun sendReliableMessage(message: ReliableMessage) {
        val protocolString = message.toProtocolString()

        // Send immediately
        sendRaw(protocolString)
        Log.d(TAG, "📤 Sent: ${message.type} (id=${message.messageId})")

        // Queue for retry if ACK required
        if (message.requiresAck) {
            pendingMessages[message.messageId] = message
            scheduleRetry(message.messageId)
        }
    }

    /**
     * Handle received message
     *
     * - Deduplicates based on messageId
     * - Sends ACK if required
     * - Returns message if not duplicate, null otherwise
     *
     * @param rawMessage The raw protocol string received
     * @param onMessage Callback for non-duplicate messages
     * @return ReliableMessage if not duplicate, null if duplicate or invalid
     */
    fun handleReceivedMessage(rawMessage: String, onMessage: () -> Unit = {}): ReliableMessage? {
        val message = ReliableMessage.fromProtocolString(rawMessage) ?: return null

        // Handle ACK messages
        if (message.type == MessageType.ACK) {
            handleAck(message.payload)
            return message
        }

        // Ensure cleanup task is running
        ensureCleanupTaskStarted()

        // Check for duplicate
        val now = System.currentTimeMillis()
        val previousReceiveTime = receivedMessageIds.putIfAbsent(message.messageId, now)

        if (previousReceiveTime != null) {
            Log.d(TAG, "📩 Duplicate message ignored: ${message.type} (id=${message.messageId})")
            return null
        }

        Log.d(TAG, "📩 Received: ${message.type} (id=${message.messageId})")

        // Send ACK if required
        if (message.requiresAck) {
            val ackMessage = ReliableMessage(
                type = MessageType.ACK,
                payload = message.messageId,
                requiresAck = false
            )
            sendRaw(ackMessage.toProtocolString())
            Log.d(TAG, "📤 Sent ACK for ${message.messageId}")
        }

        onMessage()
        return message
    }

    /**
     * Handle ACK for a message
     *
     * - Cancels pending retries
     * - Removes message from pending queue
     *
     * @param messageId The message ID being acknowledged
     */
    fun handleAck(messageId: String) {
        val message = pendingMessages.remove(messageId)
        if (message != null) {
            cancelRetry(messageId)
            Log.d(TAG, "✅ ACK received for ${message.type} (id=$messageId)")
        }
    }

    /**
     * Schedule retry for a pending message
     *
     * Retries up to MAX_RETRIES times with RETRY_DELAY_MS delay between attempts
     *
     * @param messageId The message ID to retry
     * @param attempt Current attempt number (0-indexed)
     */
    private fun scheduleRetry(messageId: String, attempt: Int = 0) {
        val job = scope.launch {
            delay(RETRY_DELAY_MS)

            val message = pendingMessages[messageId]
            if (message != null) {
                if (attempt < MAX_RETRIES) {
                    // Retry
                    sendRaw(message.toProtocolString())
                    Log.d(TAG, "🔄 Retry ${attempt + 1}/$MAX_RETRIES: ${message.type} (id=$messageId)")

                    // Schedule next retry
                    scheduleRetry(messageId, attempt + 1)
                } else {
                    // Give up after max retries
                    pendingMessages.remove(messageId)
                    retryJobs.remove(messageId)
                    Log.w(TAG, "❌ Max retries exceeded: ${message.type} (id=$messageId)")
                }
            }
        }

        retryJobs[messageId] = job
    }

    /**
     * Cancel retry job for a message
     *
     * @param messageId The message ID to cancel retries for
     */
    private fun cancelRetry(messageId: String) {
        retryJobs.remove(messageId)?.cancel()
    }

    /**
     * Clear all pending messages and cancel retries
     *
     * Used when disconnecting or resetting state
     */
    fun clearPendingMessages() {
        Log.d(TAG, "🧹 Clearing ${pendingMessages.size} pending messages")

        // Cancel all retry jobs
        retryJobs.values.forEach { it.cancel() }
        retryJobs.clear()

        // Clear pending messages
        pendingMessages.clear()
    }

    /**
     * Shutdown the reliability layer
     *
     * Cancels all pending operations and cleanup tasks
     */
    fun shutdown() {
        clearPendingMessages()
        cleanupJob?.cancel()
        cleanupJob = null
    }

    /**
     * Start background cleanup task
     *
     * Removes received message IDs older than CLEANUP_THRESHOLD_MS
     * Runs every CLEANUP_INTERVAL_MS
     */
    private fun ensureCleanupTaskStarted() {
        if (!enableAutoCleanup) return

        if (cleanupJob == null || cleanupJob?.isActive != true) {
            cleanupJob = scope.launch {
                while (true) {
                    delay(CLEANUP_INTERVAL_MS)
                    cleanupOldMessages()
                }
            }
        }
    }

    /**
     * Clean up old received message IDs
     *
     * Removes entries older than CLEANUP_THRESHOLD_MS to prevent memory leak
     */
    private fun cleanupOldMessages() {
        val now = System.currentTimeMillis()
        val threshold = now - CLEANUP_THRESHOLD_MS

        val iterator = receivedMessageIds.entries.iterator()
        var removed = 0

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value < threshold) {
                iterator.remove()
                removed++
            }
        }

        if (removed > 0) {
            Log.d(TAG, "🧹 Cleaned up $removed old message IDs")
        }
    }

    companion object {
        private const val TAG = "MessageReliability"

        // Retry configuration
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L

        // Cleanup configuration
        private const val CLEANUP_THRESHOLD_MS = 5 * 60 * 1000L  // 5 minutes
        private const val CLEANUP_INTERVAL_MS = 60 * 1000L       // 1 minute
    }
}
