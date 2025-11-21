# RotDex Agent Definitions

This directory contains YAML configuration files for specialized agents used in feature development.

## Quick Reference

For complete agent documentation, workflow, and usage examples, see:

📖 **Main Documentation**: [`feature-dev/agent-workflow.md`](../../feature-dev/agent-workflow.md)

📋 **Agent Definitions**: [`feature-dev/workflow.json`](../../feature-dev/workflow.json) (see `agent_definitions` section)

## Available Agents

| Agent               | File                      | Type            | Phase |
|---------------------|---------------------------|-----------------|-------|
| **planning**        | `planning.yaml`           | Plan            | 1     |
| **exploration**     | `exploration.yaml`        | Explore         | 2     |
| **data-layer**      | `data-layer.yaml`         | general-purpose | 3a    |
| **api-integration** | `api-integration.yaml`    | general-purpose | 3b    |
| **business-logic**  | `business-logic.yaml`     | general-purpose | 3c    |
| **ui-viewmodel**    | `ui-viewmodel.yaml`       | general-purpose | 3d    |
| **navigation**      | `navigation.yaml`         | general-purpose | 3e    |
| **testing**         | `testing.yaml`            | general-purpose | 4     |
| **review**          | `review.yaml`             | general-purpose | 5     |

## Quick Start

### 1. Plan a Feature
```
Use Task tool:
- subagent_type: "Plan"
- prompt: "Plan implementation for [feature name]"
```

### 2. Explore Existing Code
```
Use Task tool:
- subagent_type: "Explore"
- prompt: "Find similar patterns for [feature type]"
```

### 3. Implement in Parallel
Launch multiple agents in **one message**:
- data-layer
- api-integration (if needed)
- business-logic
- ui-viewmodel
- navigation

### 4. Test & Review
- Run testing agent
- Run review agent

## Development Workflow

```
Planning → Exploration → Parallel Implementation → Testing → Review
```

See detailed workflow diagram and examples in [`feature-dev/agent-workflow.md`](../../feature-dev/agent-workflow.md).

## Agent Configuration

Each YAML file in this directory defines:
- Agent purpose and responsibilities
- Required and optional inputs
- Expected outputs
- Workflow dependencies
- Code patterns to follow

## Integration with workflow.json

The [`feature-dev/workflow.json`](../../feature-dev/workflow.json) file contains:
- Complete agent definitions in the `agent_definitions` section
- Current project state and priorities
- Task queue and coordination
- Best practices

## Related Documentation

- **Agent Workflow**: `feature-dev/agent-workflow.md` - Complete workflow guide
- **Architecture**: `CLAUDE.md` - RotDex architecture patterns
- **Current State**: `feature-dev/workflow.json` - Project status
- **Feature Docs**: `docs/features/` - Implementation details

---

**For detailed agent documentation, usage examples, and troubleshooting**, see [`feature-dev/agent-workflow.md`](../../feature-dev/agent-workflow.md).
