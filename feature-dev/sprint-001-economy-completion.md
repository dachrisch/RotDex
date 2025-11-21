# Sprint 001: Economy System Completion

**Sprint Duration**: 1 Week
**Sprint Goal**: Complete the economy and progression system based on the economy plan
**Start Date**: 2025-11-21
**Status**: Planning

---

## Sprint Overview

Complete the remaining economy features to match the progression plan documented in `docs/economy-progression-plan.md`. Focus on earning mechanics (daily rewards enhancement, spin wheel rebalancing) to complement the completed achievement system.

### Sprint Objectives

1. ✅ **Achievement System** - COMPLETED (18 achievements with UI)
2. 🎯 **Daily Rewards Enhancement** - Add streak milestone bonuses
3. 🎯 **Spin Wheel Rebalancing** - Update to weighted reward distribution
4. 🎯 **Economy Testing** - Verify progression targets (Day 1-5, Week 1-10)

---

## Sprint Backlog

### Priority 1: Daily Rewards Enhancement (HIGH)

**Goal**: Add streak milestone bonuses to increase retention

**User Stories**:
- As a player, I want to receive bonus rewards at streak milestones (7, 14, 21, 30 days) so I'm motivated to log in daily
- As a player, I want to see my progress towards the next milestone so I know when I'll get bonuses

**Tasks**:
- [ ] Update `DailyRewardsViewModel` to include milestone tracking
- [ ] Add milestone bonus definitions to `GameConfig.kt`
- [ ] Update `UserRepository.claimDailyReward()` to grant milestone bonuses
- [ ] Update `DailyRewardsScreen` UI to show:
  - Current streak with visual progress bar
  - Next milestone and rewards preview
  - Celebration animation on milestone achievement
- [ ] Create milestone achievement rewards:
  - Day 7: +100 coins, +5 gems
  - Day 14: +200 coins, +10 gems, +2 energy
  - Day 21: +300 coins, +20 gems, +5 energy
  - Day 30: +500 coins, +50 gems, +10 energy
- [ ] Add streak achievements (if not already in achievement system)
- [ ] Test milestone reward granting
- [ ] Update economy plan documentation

**Acceptance Criteria**:
- [x] Milestone bonuses granted automatically at correct streak counts
- [x] UI shows progress to next milestone
- [x] Celebration shown on milestone achievement
- [x] All bonuses match economy plan specifications

**Estimated Effort**: 4-6 hours
**Dependencies**: Achievement system (completed)
**Agent Workflow**: business-logic → ui-viewmodel → testing

---

### Priority 2: Spin Wheel Rebalancing (HIGH)

**Goal**: Update spin wheel to match economy plan with weighted distribution

**User Stories**:
- As a player, I want varied spin wheel rewards so I have excitement and chance at rare prizes
- As a player, I want to see reward probabilities so I understand my chances

**Tasks**:
- [ ] Update `SpinReward` enum with new reward tiers
- [ ] Implement weighted random selection in `UserRepository.spinWheel()`
- [ ] Update reward distribution:
  - 45%: 10-20 coins (Common)
  - 30%: 30-50 coins (Uncommon)
  - 15%: 1-2 gems (Rare)
  - 8%: 100 coins (Epic)
  - 1.5%: 5 gems + 50 coins (Legendary)
  - 0.5%: 50 gems + 500 coins + 10 energy (Jackpot)
- [ ] Update spin history to track reward rarity
- [ ] Update `DailyRewardsScreen` wheel UI:
  - Color-coded segments by rarity
  - Visual weight representation (size)
  - Reward preview text
  - Jackpot celebration animation
- [ ] Add sound effects for different reward tiers (optional)
- [ ] Test probability distribution (run 10,000 spins, verify percentages)
- [ ] Update economy plan with actual tested distribution

**Acceptance Criteria**:
- [x] Reward distribution matches specified percentages (±2%)
- [x] Wheel visually represents probabilities
- [x] Jackpot shows special celebration
- [x] All reward tiers functional

**Estimated Effort**: 6-8 hours
**Dependencies**: None
**Agent Workflow**: business-logic → ui-viewmodel → testing

---

### Priority 3: Economy Balance Testing (MEDIUM)

**Goal**: Verify the economy matches progression targets from the plan

**User Stories**:
- As a developer, I want to verify progression targets are achievable
- As a game designer, I want data on actual vs expected progression

**Tasks**:
- [ ] Create economy simulation script
- [ ] Test Day 1-5 progression:
  - Daily login: 5 days = 250 coins + 15 energy
  - Daily spins: 5 spins = ~150 coins average
  - Card generation: ~8 cards (with starting energy + daily + achievements)
  - Expected: 60% Common, 30% Rare, 10% Epic
- [ ] Test Week 1-10 progression:
  - Daily login: 10 weeks = 3500 coins + streak bonuses
  - Achievements: ~2000 coins + 200 gems
  - Card generation: 250-300 cards
  - Expected distribution: 30% C, 35% R, 25% E, 10% L
- [ ] Document actual results vs expected
- [ ] Create recommendations for balance adjustments
- [ ] Update `GameConfig.kt` if adjustments needed

**Acceptance Criteria**:
- [x] Simulation results within 10% of targets
- [x] Progression feels balanced (not too easy/hard)
- [x] Documentation updated with findings

**Estimated Effort**: 4-5 hours
**Dependencies**: Daily rewards enhancement, spin wheel rebalancing
**Agent Workflow**: testing → review

---

### Priority 4: Achievement Integration (LOW)

**Goal**: Integrate achievement unlock notifications into gameplay

**User Stories**:
- As a player, I want to see achievement notifications when I unlock them during gameplay
- As a player, I want to feel rewarded immediately when I complete achievements

**Tasks**:
- [ ] Update `CardCreateViewModel` to show achievement notifications
- [ ] Update `FusionViewModel` to show achievement notifications
- [ ] Add `AchievementNotificationHost` to relevant screens:
  - `CardCreateScreen` (after card generation)
  - `FusionScreen` (after successful fusion)
  - `CollectionScreen` (when viewing collection)
- [ ] Add state management for notification queue
- [ ] Test notification flow end-to-end
- [ ] Add achievement unlock sound effect (optional)

**Acceptance Criteria**:
- [x] Notifications appear when achievements unlock
- [x] Notifications don't block critical UI
- [x] Multiple achievements queue properly
- [x] Users can see what they earned

**Estimated Effort**: 3-4 hours
**Dependencies**: Achievement system UI (completed)
**Agent Workflow**: ui-viewmodel

---

## Sprint Metrics

### Velocity Tracking
- **Story Points**: 17-23 hours estimated
- **Completed**: 0/4 priorities
- **In Progress**: 0/4 priorities
- **Blocked**: 0/4 priorities

### Definition of Done
- [x] Code reviewed
- [x] All tests passing (unit + integration)
- [x] Documentation updated
- [x] Build successful
- [x] Feature works end-to-end
- [x] No critical bugs

### Success Criteria
1. Players can earn streak milestone bonuses
2. Spin wheel has varied, balanced rewards
3. Economy progression matches targets
4. Achievement notifications enhance gameplay feel

---

## Technical Notes

### Database Changes
- **None required** - All features use existing schemas

### Configuration Updates
```kotlin
// GameConfig.kt additions
object StreakMilestones {
    val DAY_7 = StreakBonus(7, coins = 100, gems = 5, energy = 0)
    val DAY_14 = StreakBonus(14, coins = 200, gems = 10, energy = 2)
    val DAY_21 = StreakBonus(21, coins = 300, gems = 20, energy = 5)
    val DAY_30 = StreakBonus(30, coins = 500, gems = 50, energy = 10)
}

object SpinWheelWeights {
    val COMMON = 0.45f      // 10-20 coins
    val UNCOMMON = 0.30f    // 30-50 coins
    val RARE = 0.15f        // 1-2 gems
    val EPIC = 0.08f        // 100 coins
    val LEGENDARY = 0.015f  // 5 gems + 50 coins
    val JACKPOT = 0.005f    // 50 gems + 500 coins + 10 energy
}
```

### Files to Modify
- `data/models/GameConfig.kt` - Add milestone and spin wheel configs
- `data/repository/UserRepository.kt` - Update claimDailyReward() and spinWheel()
- `ui/viewmodel/DailyRewardsViewModel.kt` - Add milestone tracking
- `ui/screens/DailyRewardsScreen.kt` - Update UI for milestones and wheel
- `ui/viewmodel/CardCreateViewModel.kt` - Add achievement notifications
- `ui/viewmodel/FusionViewModel.kt` - Add achievement notifications

### Testing Strategy
1. **Unit Tests**:
   - Spin wheel probability distribution
   - Milestone bonus calculation
   - Achievement notification state

2. **Integration Tests**:
   - Daily reward flow with milestones
   - Spin wheel reward granting
   - Achievement unlock → notification

3. **Manual Tests**:
   - UI/UX feel for notifications
   - Celebration animations
   - Progression pacing

---

## Risk Assessment

### High Risk
- ⚠️ **Spin wheel balance**: May need iteration based on player feedback
  - *Mitigation*: Test with 10,000 simulation runs, adjust weights

### Medium Risk
- ⚠️ **Milestone timing**: 30-day milestone may be too long
  - *Mitigation*: Document reasoning, prepare to adjust

### Low Risk
- ✅ **Technical implementation**: All features use existing patterns
- ✅ **Database**: No schema changes needed

---

## Dependencies

### Completed
- ✅ Achievement system (18 achievements with UI)
- ✅ Daily rewards (basic streak tracking)
- ✅ Spin wheel (basic implementation)
- ✅ Energy system
- ✅ User profile management

### Blocked By
- None - all prerequisites complete

---

## Sprint Schedule

### Day 1 (Nov 21)
- Sprint planning ✅
- Start Priority 1: Daily rewards enhancement
- Agent: business-logic

### Day 2 (Nov 22)
- Complete Priority 1 backend
- Start Priority 1 UI
- Agent: ui-viewmodel

### Day 3 (Nov 23)
- Complete Priority 1
- Start Priority 2: Spin wheel rebalancing
- Agent: business-logic

### Day 4 (Nov 24)
- Complete Priority 2 backend
- Start Priority 2 UI
- Agent: ui-viewmodel

### Day 5 (Nov 25)
- Complete Priority 2
- Start Priority 3: Economy testing
- Agent: testing

### Day 6 (Nov 26)
- Complete Priority 3
- Start Priority 4: Achievement integration
- Agent: ui-viewmodel

### Day 7 (Nov 27)
- Complete Priority 4
- Sprint review
- Sprint retrospective
- Update documentation

---

## Sprint Retrospective Template

### What Went Well
- (To be filled at end of sprint)

### What Could Be Improved
- (To be filled at end of sprint)

### Action Items for Next Sprint
- (To be filled at end of sprint)

---

## Next Sprint Preview

**Sprint 002 Candidates**:
1. Collection screen implementation
2. Card detail view
3. Card packs system (100 coins = basic, 50 gems = premium)
4. Enhanced card fusion UI
5. Sound effects and polish

---

## Related Documentation

- **Economy Plan**: `docs/economy-progression-plan.md`
- **Achievement System**: `docs/features/achievements-system.md`
- **Workflow State**: `feature-dev/workflow.json`
- **Agent Workflow**: `feature-dev/agent-workflow.md`
- **Architecture**: `CLAUDE.md`

---

**Sprint Owner**: Claude
**Last Updated**: 2025-11-21
**Status**: Planning → Ready to Start
