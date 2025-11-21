# Bluetooth Battle System - Implementation Plan

## Overview
A local multiplayer feature where two players can connect via Bluetooth and battle with their cards in real-time.

## Technical Feasibility: Bluetooth on Android

### ✅ YES, Bluetooth is Possible!
Android provides robust Bluetooth APIs for peer-to-peer connections:
- **Bluetooth Classic**: Good for continuous data streams, requires pairing
- **Bluetooth Low Energy (BLE)**: Lower power, good for turn-based games
- **Nearby Connections API**: Google's high-level API that abstracts Bluetooth/WiFi Direct

### Recommended Approach: **Nearby Connections API**
**Why?**
- Handles Bluetooth AND WiFi Direct automatically (best available connection)
- No pairing required - direct device-to-device connection
- Part of Google Play Services (widely available)
- Simpler API than raw Bluetooth
- Better connection stability
- Works offline (no internet needed)

## Architecture Overview

### 1. Connection Layer
```
NearbyConnectionsManager
├── Device Discovery (scan for nearby players)
├── Connection Establishment (host/join)
├── Connection State Management
└── Message Sending/Receiving
```

### 2. Battle System Layer
```
BattleManager
├── Battle State Machine
├── Turn Management
├── Card Selection & Validation
├── Damage Calculation
├── Win/Loss Conditions
└── Battle History
```

### 3. UI Layer
```
Battle Screens
├── BattleLobbyScreen (find/connect to opponent)
├── DeckSelectionScreen (choose 3-5 cards for battle)
├── BattleArenaScreen (the actual battle interface)
└── BattleResultScreen (winner/stats)
```

## Battle Flow

### Phase 1: Connection
1. **Player 1** (Host):
   - Clicks "HOST BATTLE" button
   - Device starts advertising via Nearby Connections
   - Shows "Waiting for opponent..." with lobby code

2. **Player 2** (Client):
   - Clicks "JOIN BATTLE" button
   - Scans for nearby hosts
   - Sees list of available hosts
   - Selects host to connect

3. **Connection Established**:
   - Both players see "Connected to [PlayerName]"
   - Move to deck selection

### Phase 2: Deck Selection
1. Both players select 3-5 cards from their collection
2. Can see opponent is "Ready" when they finish selecting
3. Both click "READY" to start battle

### Phase 3: Battle Mechanics

#### Turn-Based Combat System:
```
Round Structure:
1. Both players simultaneously choose a card from their deck
2. Cards are revealed simultaneously
3. Damage calculation:
   - Both cards attack each other
   - Damage = Attacker's Attack - (Defender's Defense bonus if any)
   - Cards lose health based on damage taken
4. Card with 0 health is eliminated
5. Rarity bonuses applied:
   - Legendary: +20% attack, +20 health
   - Epic: +10% attack, +10 health
   - Rare: +5% attack, +5 health
   - Common: base stats
6. Continue until one player has no cards left
```

#### Battle States:
- **WAITING_FOR_PLAYERS**: Connection phase
- **DECK_SELECTION**: Players choosing cards
- **CARD_SELECTION**: Players choosing card for current round
- **REVEAL**: Showing selected cards
- **COMBAT**: Damage calculation and animation
- **ROUND_END**: Show results of round
- **BATTLE_END**: Winner declared

### Phase 4: Battle End
1. Winner declared (player with cards remaining)
2. Battle stats shown:
   - Rounds played
   - Total damage dealt
   - Cards eliminated
   - MVP card (highest damage)
3. Rewards:
   - Winner: +50 coins, +5 gems
   - Loser: +10 coins, +1 gem (participation)
4. Option to rematch or disconnect

## Data Models

### Battle Models
```kotlin
data class BattleSession(
    val sessionId: String,
    val hostPlayerId: String,
    val clientPlayerId: String,
    val hostDeck: List<Card>,
    val clientDeck: List<Card>,
    val currentRound: Int,
    val state: BattleState,
    val winner: String?
)

enum class BattleState {
    WAITING_FOR_PLAYERS,
    DECK_SELECTION,
    CARD_SELECTION,
    REVEAL,
    COMBAT,
    ROUND_END,
    BATTLE_END
}

data class BattleCard(
    val card: Card,
    val currentHealth: Int,
    val bonusAttack: Int = 0,
    val isAlive: Boolean = true
)

data class BattleRound(
    val roundNumber: Int,
    val hostCard: BattleCard,
    val clientCard: BattleCard,
    val hostDamage: Int,
    val clientDamage: Int,
    val winner: String? // "host", "client", or "tie"
)

data class BattleResult(
    val winner: String, // "host" or "client"
    val totalRounds: Int,
    val hostDamageDealt: Int,
    val clientDamageDealt: Int,
    val mvpCard: Card,
    val timestamp: Long
)

// Message types for Nearby Connections
sealed class BattleMessage {
    data class DeckSelection(val cards: List<Long>) : BattleMessage()
    data class CardSelection(val cardId: Long) : BattleMessage()
    data class CardReady(val ready: Boolean) : BattleMessage()
    data class StateSync(val state: BattleState) : BattleMessage()
    data class Disconnect(val reason: String) : BattleMessage()
}
```

### Database Schema
```kotlin
@Entity(tableName = "battle_history")
data class BattleHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val opponentName: String,
    val didWin: Boolean,
    val totalRounds: Int,
    val damageDealt: Int,
    val damageReceived: Int,
    val coinsEarned: Int,
    val gemsEarned: Int,
    val timestamp: Long = System.currentTimeMillis()
)
```

## Implementation Phases

### Phase 1: Foundation (Week 1)
**Goal**: Establish Bluetooth connection between two devices

**Tasks**:
1. Add Nearby Connections API dependency to `build.gradle`
2. Add necessary permissions to `AndroidManifest.xml`:
   - BLUETOOTH_ADVERTISE
   - BLUETOOTH_CONNECT
   - BLUETOOTH_SCAN
   - ACCESS_FINE_LOCATION (required for Bluetooth scanning)
   - NEARBY_WIFI_DEVICES (Android 13+)
3. Create `NearbyConnectionsManager`:
   - Device discovery
   - Connection handling
   - Message serialization/deserialization
4. Create `BattleLobbyScreen`:
   - Host button
   - Join button
   - List of nearby hosts
   - Connection status
5. Test connection between two physical devices

**Deliverable**: Two devices can find each other and establish connection

### Phase 2: Deck Selection (Week 2)
**Goal**: Players can select cards for battle

**Tasks**:
1. Create `DeckSelectionScreen`:
   - Show player's card collection
   - Multi-select cards (3-5 cards)
   - Show selected card count
   - Ready button
2. Sync deck selection between devices:
   - Send `DeckSelection` message
   - Show "Opponent Ready" indicator
3. Validate decks:
   - Both players have valid decks
   - Transition to battle when both ready

**Deliverable**: Both players select decks and proceed to battle

### Phase 3: Battle Core (Week 3)
**Goal**: Implement turn-based battle mechanics

**Tasks**:
1. Create `BattleManager`:
   - State machine implementation
   - Turn management
   - Damage calculation
   - Win/loss detection
2. Create `BattleArenaScreen`:
   - Player deck display (remaining cards)
   - Current round card selection
   - Battle arena (center stage)
   - Health bars
   - Turn indicator
3. Implement combat logic:
   - Simultaneous card selection
   - Reveal animation
   - Damage calculation with rarity bonuses
   - Card elimination
4. Add battle sound effects and animations

**Deliverable**: Complete battle from start to finish

### Phase 4: Battle Results & Polish (Week 4)
**Goal**: Show results, rewards, and polish UX

**Tasks**:
1. Create `BattleResultScreen`:
   - Winner announcement
   - Battle statistics
   - Rewards earned
   - Rematch button
2. Implement rewards system:
   - Award coins and gems
   - Update user profile
   - Save battle history
3. Create `BattleHistoryDao` and repository methods
4. Add battle history screen to view past battles
5. Polish animations and transitions
6. Add error handling and reconnection logic
7. Add "forfeit" option during battle

**Deliverable**: Complete battle feature with polish

### Phase 5: Advanced Features (Future)
- **Ranked Mode**: ELO rating system
- **Spectator Mode**: Watch other battles
- **Tournament Mode**: Multiple rounds, bracket system
- **Card Abilities**: Special effects (stun, heal, shield)
- **Best of 3**: Multiple battles in one session
- **Chat**: Pre-written messages during battle

## Technical Challenges & Solutions

### Challenge 1: Connection Reliability
**Problem**: Bluetooth can be unstable
**Solution**:
- Implement heartbeat mechanism (ping every 2 seconds)
- Auto-reconnect logic
- Save battle state periodically
- Resume from saved state if disconnected

### Challenge 2: State Synchronization
**Problem**: Keep both devices in sync
**Solution**:
- Host device is "source of truth"
- Client sends actions, host validates
- Host broadcasts state updates
- Include sequence numbers in messages

### Challenge 3: Cheating Prevention
**Problem**: Players could modify local data
**Solution**:
- Host validates all actions
- Both players send card selection simultaneously
- Reveal only after both selections received
- Timestamp validation for selections

### Challenge 4: Permission Handling
**Problem**: Many permissions required, some runtime
**Solution**:
- Request permissions progressively
- Clear explanation of why each permission needed
- Graceful degradation if permissions denied
- Use ActivityResult API for permission requests

## File Structure

```
app/src/main/java/com/rotdex/
├── data/
│   ├── models/
│   │   ├── BattleSession.kt
│   │   ├── BattleCard.kt
│   │   ├── BattleRound.kt
│   │   ├── BattleResult.kt
│   │   ├── BattleMessage.kt
│   │   └── BattleHistory.kt
│   ├── database/
│   │   └── BattleHistoryDao.kt
│   ├── manager/
│   │   ├── NearbyConnectionsManager.kt
│   │   └── BattleManager.kt
│   └── repository/
│       └── BattleRepository.kt
├── ui/
│   ├── screens/
│   │   ├── BattleLobbyScreen.kt
│   │   ├── DeckSelectionScreen.kt
│   │   ├── BattleArenaScreen.kt
│   │   ├── BattleResultScreen.kt
│   │   └── BattleHistoryScreen.kt
│   └── viewmodel/
│       ├── BattleLobbyViewModel.kt
│       ├── DeckSelectionViewModel.kt
│       ├── BattleArenaViewModel.kt
│       └── BattleResultViewModel.kt
└── utils/
    ├── BattleCalculations.kt
    └── BattleAnimations.kt
```

## Dependencies to Add

```gradle
// In app/build.gradle
dependencies {
    // Nearby Connections API
    implementation 'com.google.android.gms:play-services-nearby:19.0.0'

    // Kotlinx Serialization for message encoding
    implementation 'org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0'

    // Optional: For better animations
    implementation 'androidx.compose.animation:animation:1.5.4'
}
```

## Permissions Required

```xml
<!-- In AndroidManifest.xml -->
<manifest>
    <!-- Bluetooth permissions (Android 12+) -->
    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />

    <!-- Location required for Bluetooth scanning -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <!-- Nearby WiFi devices (Android 13+) -->
    <uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />

    <application>
        <!-- Nearby Connections requires Google Play Services -->
        <meta-data
            android:name="com.google.android.gms.version"
            android:value="@integer/google_play_services_version" />
    </application>
</manifest>
```

## UI/UX Considerations

### Battle Lobby Design
- **Vibrant theme** matching existing brainrot aesthetic
- **Large, clear buttons**: "HOST BATTLE 🔥" and "JOIN BATTLE ⚔️"
- **Animated scanning indicator** when looking for opponents
- **Player names** displayed prominently

### Battle Arena Design
```
┌─────────────────────────────────┐
│ Opponent Deck: [🎴][🎴][🎴]     │
│ HP: ████████░░ 80/100           │
├─────────────────────────────────┤
│                                 │
│        ⚔️ BATTLE ARENA ⚔️        │
│                                 │
│      [Card A] VS [Card B]       │
│                                 │
├─────────────────────────────────┤
│ Your Deck: [🎴][🎴][🎴][🎴][🎴] │
│ HP: ██████████ 100/100          │
└─────────────────────────────────┘
```

### Animations
- **Card flip** when revealing selections
- **Slash effect** for attacks
- **Health bar decrease** animation
- **Card elimination** (fade + shake)
- **Victory confetti** 🎉

## Testing Strategy

### Unit Tests
- Battle calculations (damage, health, rarity bonuses)
- State machine transitions
- Win/loss detection
- Message serialization

### Integration Tests
- Connection establishment
- Message sending/receiving
- State synchronization
- Disconnection handling

### Manual Testing (2 Physical Devices Required!)
- Connection flow
- Deck selection sync
- Full battle playthrough
- Edge cases (disconnection, backgrounding app)
- Performance (battery drain, latency)

## Success Metrics

### Technical
- ✅ Connection success rate > 95%
- ✅ Average latency < 500ms
- ✅ Zero crashes during battles
- ✅ Battery drain < 10% per 10-minute battle

### User Experience
- ✅ < 30 seconds to find and connect to opponent
- ✅ Intuitive card selection (no confusion)
- ✅ Clear battle state at all times
- ✅ Satisfying animations and feedback

## Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| Bluetooth connection fails | HIGH | Fallback to WiFi Direct via Nearby API, clear error messages |
| Devices out of range | MEDIUM | Range indicator, reconnect prompt |
| Permission denial | MEDIUM | Clear explanation, graceful degradation |
| Cheating/hacking | LOW | Host validation, server validation in future |
| Battery drain | MEDIUM | Optimize connection, use BLE when possible |

## Future Enhancements
1. **Online Multiplayer**: Use Firebase for remote battles
2. **AI Opponent**: Practice against computer
3. **Card Abilities**: Special powers and effects
4. **Battle Pass**: Seasonal rewards for battles won
5. **Leaderboards**: Track top battlers
6. **Tournaments**: Organized competition events
7. **Spectator Mode**: Watch friends battle
8. **Replay System**: Save and review battles

## Estimated Timeline
- **Phase 1 (Connection)**: 1 week
- **Phase 2 (Deck Selection)**: 1 week
- **Phase 3 (Battle Core)**: 1.5 weeks
- **Phase 4 (Results & Polish)**: 1.5 weeks
- **Total**: ~5 weeks for MVP

## Next Steps
1. ✅ Review and approve this plan
2. Set up Nearby Connections API and test basic connection
3. Create battle data models
4. Implement Phase 1 (Connection)
5. Test on two physical Android devices

---

**Ready to start implementation?** 🚀
