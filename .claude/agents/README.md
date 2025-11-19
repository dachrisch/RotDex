# RotDex Agent Definitions

This directory contains specialized agent configurations for feature development in the RotDex Android app.

## Agent Overview

| Agent               | Type            | Phase | Purpose                                             |
|---------------------|-----------------|-------|-----------------------------------------------------|
| **planning**        | Plan            | 1     | Analyze requirements and create implementation plan |
| **exploration**     | Explore         | 2     | Find existing patterns and reusable components      |
| **data-layer**      | general-purpose | 3a    | Implement Room entities, DAOs, and migrations       |
| **api-integration** | general-purpose | 3b    | Implement API services and network integration      |
| **business-logic**  | general-purpose | 3c    | Implement repositories and manager classes          |
| **ui-viewmodel**    | general-purpose | 3d    | Implement Compose screens and ViewModels            |
| **navigation**      | general-purpose | 3e    | Integrate navigation routes and flows               |
| **testing**         | general-purpose | 4     | Write unit and instrumented tests                   |
| **review**          | general-purpose | 5     | Code review and quality assurance                   |

## Development Workflow

```
┌─────────────┐
│  Planning   │  Analyze requirements, create task breakdown
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Exploration │  Find existing patterns to follow
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────┐
│         Parallel Implementation             │
│  ┌────────────┬────────────┬──────────────┐ │
│  │ Data Layer │ API Layer  │ Business     │ │
│  │            │            │ Logic        │ │
│  └────────────┴────────────┴──────────────┘ │
│  ┌────────────┬────────────┐                │
│  │ UI/View    │ Navigation │                │
│  │ Model      │            │                │
│  └────────────┴────────────┘                │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
            ┌──────────────┐
            │   Testing    │  Unit + Instrumented tests
            └──────┬───────┘
                   │
                   ▼
            ┌──────────────┐
            │    Review    │  Quality and security review
            └──────────────┘
```

## Agent Capabilities

### Phase 1: Planning (`planning.yaml`)
**What it does:**
- Breaks down features into tasks
- Identifies affected components (Database, API, Repository, ViewModel, UI)
- Creates dependency graph
- Assesses risks and complexity

**Output:**
- `feature-dev/[feature-name]-plan.md`
- Updates to `workflow.json` and `STATUS.md`

**When to use:**
- Starting a new feature
- Need to understand scope and complexity

---

### Phase 2: Exploration (`exploration.yaml`)
**What it does:**
- Finds similar existing features
- Identifies reusable patterns and components
- Locates relevant code to reference
- Documents architectural patterns

**Output:**
- `feature-dev/[feature-name]-exploration.md`
- List of files and patterns to follow

**When to use:**
- After planning, before implementation
- When unsure how to implement something
- To ensure consistency with existing code

---

### Phase 3: Parallel Implementation

#### 3a. Data Layer (`data-layer.yaml`)
**What it does:**
- Creates Room entities and DAOs
- Adds database migrations
- Updates DatabaseModule
- Writes database tests

**Output:**
- `data/models/` - Entity classes
- `data/database/` - DAO interfaces
- Unit tests

**When to use:**
- Feature needs new database tables
- Adding queries to existing DAOs

---

#### 3b. API Integration (`api-integration.yaml`)
**What it does:**
- Defines Retrofit API services
- Creates request/response models
- Updates NetworkModule
- Writes API tests

**Output:**
- `data/api/` - Service interfaces and models
- Integration tests

**When to use:**
- Feature needs external API calls
- Adding new API endpoints

---

#### 3c. Business Logic (`business-logic.yaml`)
**What it does:**
- Creates repository classes
- Implements manager classes
- Adds validation rules
- Handles errors

**Output:**
- `data/repository/` - Repository classes
- `data/manager/` - Manager classes
- Unit tests

**When to use:**
- Coordinating between database and API
- Complex business rules
- Data transformation logic

---

#### 3d. UI & ViewModel (`ui-viewmodel.yaml`)
**What it does:**
- Creates Compose screens
- Implements ViewModels with state management
- Handles user interactions
- Writes UI tests

**Output:**
- `ui/screens/` - Composable screens
- `ui/viewmodel/` - ViewModels
- UI tests

**When to use:**
- Building new screens
- Adding UI interactions
- State management

---

#### 3e. Navigation (`navigation.yaml`)
**What it does:**
- Adds routes to navigation graph
- Integrates screens into app flow
- Implements navigation callbacks

**Output:**
- Updated `ui/navigation/NavGraph.kt`

**When to use:**
- After screens are created
- Adding new navigation flows

---

### Phase 4: Testing (`testing.yaml`)
**What it does:**
- Writes unit tests for business logic
- Writes instrumented tests for UI
- Writes integration tests
- Verifies error handling

**Output:**
- `test/` - Unit tests
- `androidTest/` - Instrumented tests

**When to use:**
- After implementation completes
- Adding test coverage
- Verifying edge cases

---

### Phase 5: Review (`review.yaml`)
**What it does:**
- Reviews for security vulnerabilities
- Checks error handling
- Verifies performance
- Validates architecture adherence

**Output:**
- `feature-dev/[feature-name]-review.md`
- List of issues and recommendations

**When to use:**
- Before merging feature
- Quality gate before release
- Security audit

---

## How to Use Agents

### Running Single Agent
```
Use Task tool with:
- subagent_type: "general-purpose" (or "Plan"/"Explore")
- prompt: Reference agent config from .claude/agents/[agent-name].yaml
- description: Short task description
```

### Running Parallel Agents
**Launch multiple agents in ONE message:**
```
Message with 5 Task tool calls:
1. data-layer agent
2. api-integration agent
3. business-logic agent
4. ui-viewmodel agent
5. navigation agent
```

This executes all agents simultaneously for maximum efficiency.

---

## Agent Configuration Format

Each agent YAML file contains:

```yaml
name: agent-name
version: 1.0.0
description: What this agent does
type: general-purpose|Plan|Explore

purpose: Detailed purpose

responsibilities:
  - task1
  - task2

inputs:
  required:
    - input1
  optional:
    - input2

outputs:
  deliverables:
    - output1
  destination: where files go

workflow:
  phase: when it runs
  depends_on:
    - prerequisites
  parallel_with:
    - other agents

prompts:
  system_context: Agent context
  task_template: Task structure
```

---

## Best Practices

### Before Using Agents
1. Read `feature-dev/agent-workflow.md` for full workflow
2. Check `feature-dev/workflow.json` for current state
3. Review `CLAUDE.md` for architecture patterns

### During Agent Execution
1. Run planning → exploration → parallel implementation → testing → review
2. Launch implementation agents in parallel when possible
3. Update `workflow.json` after each phase
4. Document decisions in feature docs

### After Agents Complete
1. Review all outputs
2. Run tests: `./gradlew test connectedAndroidTest`
3. Run linter: `./gradlew detekt`
4. Update documentation
5. Mark complete in `STATUS.md`

---

## Troubleshooting

### Agent Not Finding Patterns
- Ensure exploration agent ran first
- Check that similar features exist
- Review CLAUDE.md for architecture

### Parallel Agents Conflicting
- Ensure agents work on different files/layers
- If editing same file, run sequentially
- Data layer should complete before business logic

### Tests Failing
- Run testing agent with specific focus
- Check for missing mocks
- Verify dependency injection setup

---

## Examples

### Simple Feature: Card Detail View
1. **Planning**: Small task, minimal components
2. **Exploration**: Reference CardCreateScreen
3. **Implementation**: UI + ViewModel agents only
4. **Testing**: UI tests
5. **Review**: Quick review

### Complex Feature: Card Trading
1. **Planning**: Large task, multiple components
2. **Exploration**: Review fusion system (similar multi-card logic)
3. **Implementation**: All 5 agents in parallel (data, API, business, UI, nav)
4. **Testing**: Comprehensive unit + integration + UI tests
5. **Review**: Full security and performance review

---

## Related Documentation

- **Agent Workflow**: `feature-dev/agent-workflow.md`
- **Architecture Guide**: `CLAUDE.md`
- **Feature Status**: `docs/features/STATUS.md`
- **Workflow State**: `feature-dev/workflow.json`

---

**Questions?** See `feature-dev/agent-workflow.md` for detailed workflow documentation.
