# Connection Test Instructions

## What We Built

A minimal proof-of-concept test screen to verify that Nearby Connections API (Bluetooth/WiFi Direct) works for multiplayer connectivity.

## What You Need

**IMPORTANT**: You need **2 physical Android devices** to test this. Emulators do NOT support Bluetooth.

- 2 Android phones/tablets (Android 6.0+, ideally Android 12+)
- Both devices connected to your computer OR
- One device + sideload APK to second device

## How to Test

### Step 1: Install on Both Devices

**Option A: Both devices connected to computer**
```bash
# Check connected devices
adb devices

# Should see something like:
# ABC123          device
# XYZ789          device

# Install on first device
adb -s ABC123 install app/build/outputs/apk/debug/app-debug.apk

# Install on second device
adb -s XYZ789 install app/build/outputs/apk/debug/app-debug.apk
```

**Option B: Install via Android Studio**
1. Connect first device via USB
2. Click Run ▶️ in Android Studio
3. Wait for installation to complete
4. Disconnect first device
5. Connect second device via USB
6. Click Run ▶️ again

**Option C: Sideload APK to second device**
1. Build: `./gradlew assembleDebug`
2. Find APK: `app/build/outputs/apk/debug/app-debug.apk`
3. Email/Share APK to second device
4. Install on second device (enable "Install from Unknown Sources")

### Step 2: Grant Permissions

On **BOTH devices**:
1. Open the app
2. Navigate to: Home → "CONNECTION TEST 🧪"
3. When prompted, grant ALL permissions:
   - ✅ Bluetooth permissions
   - ✅ Location permissions
   - ✅ Nearby devices permissions

**Note**: Location is required by Android for Bluetooth scanning (privacy requirement)

### Step 3: Test Connection

#### On Device 1 (Host):
1. Open Connection Test screen
2. Click "🔥 START HOSTING"
3. You should see:
   - Status: "📡 Advertising..."
   - Activity Log: "Advertising as: Player1234"
   - "✅ Waiting for opponent..."

#### On Device 2 (Client):
1. Open Connection Test screen
2. Click "🔍 SCAN FOR HOSTS"
3. You should see:
   - Status: "🔍 Discovering..."
   - Activity Log: "Scanning..."
   - After a few seconds: "👀 Found: Player1234"
4. In the "Found Hosts" section, click "Connect to: Player1234"

#### Connection Established:
- **Both devices** should show:
  - Status: "✅ CONNECTED!"
  - Activity Log: "✅ Connected successfully!"

### Step 4: Test Messaging

Once connected, on **EITHER device**:
1. Type a message in "Test Message" field
2. Click "📤 SEND MESSAGE"
3. Watch the Activity Log on **BOTH devices**
4. You should see:
   - Sending device: "📤 Sent: [your message]"
   - Receiving device: "📩 Received: [your message]"

### Step 5: Disconnect and Reset

Click "STOP & RESET" on either device to disconnect.

## Expected Results

### ✅ Success Criteria

1. **Discovery**: Device 2 finds Device 1 within 5-10 seconds
2. **Connection**: Devices connect within 2-3 seconds after clicking "Connect"
3. **Messaging**: Messages appear on both devices instantly (< 1 second delay)
4. **Stability**: Connection stays active while both devices are nearby

### ❌ Common Issues

| Problem | Possible Causes | Solution |
|---------|----------------|----------|
| Permissions denied | User declined permissions | Go to App Settings → Permissions → Grant all |
| "Advertising failed" | Bluetooth off, Permissions missing | Turn on Bluetooth, check permissions |
| "Discovery failed" | Location off, Permissions missing | Turn on Location, check permissions |
| Device not found | Devices too far apart | Move devices closer (< 10 meters) |
| Connection rejected | Network interference | Retry, move away from WiFi routers |
| Messages not received | Connection dropped | Check connection status, reconnect |

## What This Proves

If the test works:
- ✅ Nearby Connections API is working
- ✅ Devices can find each other
- ✅ Devices can connect
- ✅ Devices can send/receive messages
- ✅ **We can build the battle system!**

## Technical Details

### Connection Method
- Uses **Nearby Connections API** (not raw Bluetooth)
- Automatically chooses best connection:
  - Bluetooth Classic
  - Bluetooth Low Energy (BLE)
  - WiFi Direct
- Strategy: `P2P_POINT_TO_POINT` (1-to-1 connection)

### Message Format
- Simple UTF-8 text strings
- For battle, we'll use JSON-serialized data

### Range
- Typical: 10-30 meters (30-100 feet)
- Depends on:
  - Environment (walls, obstacles)
  - Device capabilities
  - Interference (WiFi, other Bluetooth devices)

## Debugging Tips

### View Logs
```bash
# Device 1 logs
adb -s ABC123 logcat | grep ConnectionTest

# Device 2 logs
adb -s XYZ789 logcat | grep ConnectionTest
```

### Key Log Messages
- `Starting advertising as: [name]` - Host started
- `Advertising started successfully` - Host ready
- `Starting discovery as: [name]` - Client scanning
- `Endpoint found: [name]` - Client found host
- `Connection initiated with: [name]` - Connection starting
- `Connection successful!` - Connected!
- `Received message: [text]` - Message received

## Next Steps

Once this test works:
1. ✅ We know connectivity works
2. ➡️ Build battle data models
3. ➡️ Implement battle state machine
4. ➡️ Create battle UI
5. ➡️ Replace simple text messages with battle actions

## Removing the Test Screen

This is a temporary test screen. To remove it later:
1. Delete `ConnectionTestScreen.kt`
2. Delete `ConnectionTestManager.kt`
3. Remove navigation route from `NavGraph.kt`
4. Remove button from `HomeScreen.kt`
5. Keep the Nearby Connections dependency (needed for battle)

## Need Help?

Common issues and solutions:

**"I only have one device!"**
- Borrow a friend's phone for 30 minutes
- Ask a family member
- Test the UI/logic with mock connections (ask me to implement)

**"Permissions keep getting denied"**
- Go to Android Settings → Apps → RotDex → Permissions
- Manually grant all permissions
- Restart the app

**"Devices can't find each other"**
- Make sure Bluetooth is ON on both devices
- Make sure Location is ON on both devices
- Move devices closer together (same room)
- Turn off and on Bluetooth on both devices
- Restart both apps

**"Connection keeps dropping"**
- Normal behavior if devices move apart
- Keep devices within 10 meters
- Avoid obstacles between devices
- Move away from WiFi routers/microwaves
