# Image Transfer Testing Guide

**Date:** 2025-11-26
**Issue:** Images not transferring between devices during battle
**Status:** No automated tests exist for this functionality

## Problem Analysis

The BattleManager implements a bidirectional image transfer protocol using Google Nearby Connections:

1. **Sender** sends both:
   - IMAGE_TRANSFER metadata message (BYTES payload)
   - FILE payload with actual image

2. **Receiver** must handle arrival in EITHER order:
   - FILE arrives first → store as "orphaned" until metadata arrives
   - IMAGE_TRANSFER arrives first → register expectation until FILE arrives

## Why Unit Tests Are Difficult

BattleManager has deep Android dependencies that are hard to mock:

- `ConnectionsClient` (Google Play Services)
- `Payload` and `PayloadCallback` (Nearby Connections API)
- `ParcelFileDescriptor` (Android OS)
- `Context` and file system operations

**Recommendation:** Use instrumented tests (androidTest) instead of unit tests for this functionality.

## Manual Testing Checklist

To debug image transfer issues on physical devices:

###  1. Enable Verbose Logging

Check logcat on BOTH devices for:

```
TAG: BattleManager
```

**Key log messages to look for:**

**Sender side:**
```
Sending image: cardId=X, payloadId=Y, size=Z
```

**Receiver side (FILE arrives first):**
```
Received FILE payload: payloadId=X, path=..., size=Y
Metadata not yet received, storing orphaned file: payloadId=X
```

**Receiver side (IMAGE_TRANSFER arrives first):**
```
Received IMAGE_TRANSFER metadata: cardId=X, payloadId=Y, fileName=Z
File not yet received, waiting for payloadId=Y
```

**Complete transfer:**
```
Updated opponent card X with image: /path/to/image?t=timestamp
```

### 2. Common Failure Points

| Scenario | Symptom | Log Check |
|----------|---------|-----------|
| File doesn't exist | No "Sending image" log | Sender: "Image file not found" |
| FILE never arrives | Orphaned file never matched | Receiver: Missing "Received FILE payload" |
| Metadata never arrives | Expected transfer never matched | Receiver: Missing "Received IMAGE_TRANSFER" |
| Wrong payload ID | IDs don't match | Check payloadId values match between logs |
| Image not displayed | Transfer completes but UI doesn't update | Check "Updated opponent card" log |
| Coil cache issue | Old image shown | Verify ?t=timestamp in image URL |

### 3. Device Testing Steps

**Device A (Host):**
1. Start battle arena as host
2. Select a card
3. Check logcat for "Sending image"
4. Verify FILE payload sent
5. Verify IMAGE_TRANSFER message sent

**Device B (Client):**
1. Connect to host
2. Wait for card selection
3. Check logcat for "Received FILE payload" OR "Received IMAGE_TRANSFER"
4. Verify both messages eventually arrive
5. Check for "Updated opponent card" message
6. Verify opponent card image appears in UI

### 4. Debug Recommendations

If images still don't transfer:

1. **Check file permissions:**
   ```kotlin
   // In BattleManager, add logging
   Log.d(TAG, "Image file exists: ${imageFile.exists()}")
   Log.d(TAG, "Image file readable: ${imageFile.canRead()}")
   Log.d(TAG, "Image file size: ${imageFile.length()}")
   ```

2. **Verify payload IDs match:**
   ```kotlin
   // When sending
   Log.d(TAG, "FILE payloadId: ${filePayload.id}")

   // When receiving
   Log.d(TAG, "Received payloadId: ${payload.id}")
   Log.d(TAG, "Expected payloadId: $payloadId from metadata")
   ```

3. **Check orphaned files cleanup:**
   ```kotlin
   // Add to resetBattleState()
   Log.d(TAG, "Orphaned files before cleanup: ${orphanedFiles.keys}")
   ```

4. **Verify image path update:**
   ```kotlin
   // In updateCardWithImage()
   Log.d(TAG, "Current opponent card ID: ${_opponentCard.value?.card?.id}")
   Log.d(TAG, "Trying to update card ID: $cardId")
   Log.d(TAG, "Image path: $imagePath")
   ```

## Recommended Test Strategy

Since unit tests are complex, use **instrumented tests** with actual Nearby Connections:

### Option 1: Mock ConnectionsClient (Medium difficulty)

```kotlin
@RunWith(AndroidJUnit4::class)
class BattleManagerInstrumentedTest {

    @Test
    fun testImageTransfer_metadataFirst() {
        // Use Robolectric or real Android test context
        val context = InstrumentationRegistry.getInstrumentation().context
        val manager = BattleManager(context)

        // Simulate receiving messages in order
        // ... test implementation
    }
}
```

### Option 2: Integration Test on Emulators (High confidence)

Create two emulator instances and test real Nearby Connections:

1. Start two emulators
2. Run instrumented test that pairs them
3. Transfer actual image
4. Verify receipt

**Example:**
```kotlin
@Test
fun testRealImageTransfer() {
    // Requires 2 emulators running
    // Use Nearby Connections test mode
}
```

### Option 3: Manual QA Test Plan (Fastest to implement)

Create a test document with step-by-step verification:

1. ✅ Host starts battle
2. ✅ Client discovers host
3. ✅ Client connects
4. ✅ Both select cards
5. ✅ Host sees client card image
6. ✅ Client sees host card image
7. ✅ Images persist after battle

## Known Issues to Check

Based on the code review, potential issues:

### Issue 1: ParcelFileDescriptor Lifecycle
The code closes `parcelFileDescriptor` immediately after reading. Verify this doesn't cause issues:

```kotlin
// Line 128 in BattleManager.kt
parcelFileDescriptor.close()
```

**Test:** Does the FileInputStream finish reading before close?

### Issue 2: Orphaned File Cleanup
Orphaned files are cleaned up in `resetBattleState()`.

**Test:** Are orphaned files being deleted before metadata arrives?

### Issue 3: Card ID Mismatch
If opponent card isn't set yet when image arrives:

```kotlin
// Line 646 in updateCardWithImage()
_opponentCard.value?.let { battleCard ->
    if (battleCard.card.id == cardId) {
        // Only updates if IDs match
    }
}
```

**Test:** Is opponent card set BEFORE image transfer completes?

### Issue 4: Cache-Busting Timestamp
Image URL gets `?t=timestamp` appended for cache busting:

```kotlin
// Line 648
val cacheBustedPath = "$imagePath?t=${System.currentTimeMillis()}"
```

**Test:** Does Coil correctly handle file:// URLs with query parameters?

## Debugging Commands

### View logcat for both devices:
```bash
# Device A
adb -s <device_A_serial> logcat -s BattleManager:D

# Device B
adb -s <device_B_serial> logcat -s BattleManager:D
```

### Check saved images on device:
```bash
adb shell ls -la /data/data/com.rotdex/files/card_images/
```

### Pull images for inspection:
```bash
adb pull /data/data/com.rotdex/files/card_images/ ./debug_images/
```

## Next Steps

1. **Add more debug logging** to BattleManager (4 key points above)
2. **Test on 2 physical devices** with logcat running on both
3. **Capture logs** when image transfer fails
4. **Share logs** for analysis
5. **Consider instrumented tests** once root cause is found

## Test Coverage Goal

Once instrumented tests are created, target coverage:

- ✅ Metadata arrives before FILE
- ✅ FILE arrives before metadata
- ✅ Multiple concurrent transfers
- ✅ Transfer completes and updates UI
- ✅ Orphaned files cleaned up
- ✅ Cache-busting works
- ✅ Error handling (file not found, connection dropped)

Total: 7 core test cases needed
