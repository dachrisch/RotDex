# Quick Start: Agent-Driven Development

## TL;DR - Start a New Feature

```bash
# 1. Create feature branch
git checkout -b feature/your-feature-name

# 2. Read the workflow
Read: feature-dev/agent-workflow.md

# 3. Run planning agent
Use Task tool → planning agent → get task breakdown

# 4. Run exploration agent
Use Task tool → exploration agent → find patterns to follow

# 5. Run implementation agents IN PARALLEL (one message, multiple tools)
- data-layer agent
- api-integration agent (if needed)
- business-logic agent
- ui-viewmodel agent
- navigation agent

# 6. Run testing agent
Use Task tool → testing agent → comprehensive tests

# 7. Run review agent
Use Task tool → review agent → quality check

# 8. Manual verification
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew detekt

# 9. Update docs
- feature-dev/workflow.json
- docs/features/STATUS.md
- feature-dev/[feature-name].md

# 10. Commit and push
git add .
git commit -m "Implement [feature-name]"
git push
```

---

## The 9 Agents

### Phase 1: Planning (planning.json)
**What**: Break down feature into tasks
**Input**: Feature requirements
**Output**: `feature-dev/[feature-name]-plan.md`

### Phase 2: Exploration (exploration.json)
**What**: Find existing patterns to follow
**Input**: Feature description
**Output**: `feature-dev/[feature-name]-exploration.md`

### Phase 3a: Data Layer (data-layer.json)
**What**: Create Room entities and DAOs
**Output**: Entities, DAOs, tests

### Phase 3b: API Integration (api-integration.json)
**What**: Create Retrofit services
**Output**: API interfaces, models, tests

### Phase 3c: Business Logic (business-logic.json)
**What**: Create repositories and managers
**Output**: Repositories, managers, tests

### Phase 3d: UI & ViewModel (ui-viewmodel.json)
**What**: Create Compose screens and ViewModels
**Output**: Screens, ViewModels, UI tests

### Phase 3e: Navigation (navigation.json)
**What**: Integrate navigation
**Output**: Updated NavGraph.kt

### Phase 4: Testing (testing.json)
**What**: Comprehensive test coverage
**Output**: Unit + instrumented tests

### Phase 5: Review (review.json)
**What**: Quality and security review
**Output**: Review report with findings

---

## Parallel Execution Example

**Instead of this** (sequential, slow):
```
Message 1: Run data-layer agent
Message 2: Run business-logic agent
Message 3: Run ui-viewmodel agent
Message 4: Run navigation agent
```

**Do this** (parallel, fast):
```
Message 1 with 4 Task tool calls:
  1. data-layer agent: "Create entities and DAOs for X"
  2. business-logic agent: "Create repository for X"
  3. ui-viewmodel agent: "Create screen and ViewModel for X"
  4. navigation agent: "Add X to navigation"
```

---

## File Locations Cheat Sheet

| What | Where |
|------|-------|
| Agent configs | `.claude/agents/*.json` |
| Workflow docs | `feature-dev/agent-workflow.md` |
| Feature status | `docs/features/STATUS.md` |
| Workflow state | `feature-dev/workflow.json` |
| Architecture | `CLAUDE.md` |
| Entities | `app/src/main/java/com/rotdex/data/models/` |
| DAOs | `app/src/main/java/com/rotdex/data/database/` |
| APIs | `app/src/main/java/com/rotdex/data/api/` |
| Repositories | `app/src/main/java/com/rotdex/data/repository/` |
| Managers | `app/src/main/java/com/rotdex/data/manager/` |
| ViewModels | `app/src/main/java/com/rotdex/ui/viewmodel/` |
| Screens | `app/src/main/java/com/rotdex/ui/screens/` |
| Navigation | `app/src/main/java/com/rotdex/ui/navigation/NavGraph.kt` |
| Unit tests | `app/src/test/java/com/rotdex/` |
| UI tests | `app/src/androidTest/java/com/rotdex/` |

---

## Common Commands

```bash
# Build
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest

# Instrumented tests
./gradlew connectedDebugAndroidTest

# Static analysis
./gradlew detekt

# Clean build
./gradlew clean build

# Run specific test
./gradlew test --tests "ClassName.testMethod"
```

---

## Decision Tree

**Q: Do I need database changes?**
→ Yes: Run data-layer agent
→ No: Skip

**Q: Do I need API integration?**
→ Yes: Run api-integration agent
→ No: Skip

**Q: Do I need business logic?**
→ Yes: Run business-logic agent
→ No: Skip

**Q: Do I need new UI?**
→ Yes: Run ui-viewmodel agent
→ No: Skip

**Q: Do I need navigation changes?**
→ Yes: Run navigation agent
→ No: Skip

**Always run**: planning → exploration → testing → review

---

## Troubleshooting

**Problem**: Agent can't find patterns
**Solution**: Run exploration agent with "very thorough" thoroughness

**Problem**: Parallel agents conflicting
**Solution**: Ensure they work on different files/layers

**Problem**: Tests failing
**Solution**: Run testing agent with focus on failing area

**Problem**: Don't know where to start
**Solution**: Start with planning agent

---

## Resources

- **Full workflow**: `feature-dev/agent-workflow.md`
- **Agent definitions**: `.claude/agents/README.md`
- **Architecture**: `CLAUDE.md`
- **Status tracking**: `docs/features/STATUS.md`
- **Current state**: `feature-dev/workflow.json`

---

## Example: Simple Feature (Card Detail View)

```
1. Planning agent:
   "Plan card detail view feature showing full card info"

2. Exploration agent:
   "Find patterns for detail views and card display"

3. Parallel (2 agents):
   - ui-viewmodel: "Create CardDetailScreen and ViewModel"
   - navigation: "Add detail route with card ID parameter"

4. Testing agent:
   "Write UI tests for card detail view"

5. Review agent:
   "Review card detail implementation"

Done!
```

## Example: Complex Feature (Card Trading)

```
1. Planning agent:
   "Plan card trading feature with offers and history"

2. Exploration agent:
   "Find patterns for user interactions and multi-card selection"

3. Parallel (5 agents):
   - data-layer: "Create TradeOffer and TradeHistory entities"
   - api-integration: "Create TradeApiService (if multiplayer)"
   - business-logic: "Create TradeRepository and TradeManager"
   - ui-viewmodel: "Create TradeScreen and ViewModel"
   - navigation: "Add trading routes"

4. Testing agent:
   "Comprehensive tests for trading feature"

5. Review agent:
   "Security and quality review for trading"

Done!
```

---

**Questions?** See `feature-dev/agent-workflow.md` for complete details.
