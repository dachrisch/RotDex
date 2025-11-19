# Feature Development Documentation

This directory contains detailed documentation for feature development and workflow coordination.

## Purpose

The `feature-dev/` directory serves as:
1. **Implementation tracking** - Detailed progress documentation for each feature
2. **Workflow coordination** - JSON-based state tracking for multi-agent coordination
3. **Development history** - Complete record of decisions, issues, and resolutions
4. **Knowledge base** - Reference material for future development work

## Files

### `card-generation-implementation.md`
Comprehensive documentation of the card generation feature implementation:
- Complete implementation timeline with all commits
- Detailed explanation of each phase
- API integration details and error handling
- Testing strategy and coverage
- Known issues and future enhancements
- Architecture diagrams and code examples

### `workflow.json`
Structured workflow state for agent coordination:
- Current development phase and status
- Feature completion tracking
- Priority queue for next tasks
- Technical debt inventory
- Testing status and coverage
- API integration details
- Agent coordination and task assignment
- Known issues and resolutions

## Usage

### For Developers
- **Starting new work?** Check `workflow.json` for next priorities and current status
- **Need context?** Read implementation docs to understand what's already done
- **Debugging?** Check known issues and error handling sections
- **Adding tests?** Review existing test coverage and strategy

### For AI Agents
- **Task coordination:** Read `workflow.json` to understand current state and next tasks
- **Context loading:** Use implementation docs to get full context on completed work
- **Handoff:** Update `workflow.json` when completing tasks or switching focus
- **Consistency:** Follow patterns and conventions documented here

### For Project Managers
- **Progress tracking:** Check completion status of features
- **Priority planning:** Review next_priorities in workflow.json
- **Technical debt:** Monitor technical_debt list
- **Risk assessment:** Review known_issues

## Updating Documentation

### When to Update
- After completing a major feature or phase
- When discovering new issues or technical debt
- When changing development priorities
- Before/after agent handoffs

### What to Update
1. **Implementation docs** - Add new phases, commits, and learnings
2. **workflow.json** - Update status, priorities, and coordination info
3. **Known issues** - Document new issues or mark resolved
4. **Next priorities** - Adjust based on business/technical needs

## Related Documentation

- **CLAUDE.md** - General project guidance and architecture
- **TESTING.md** - Testing infrastructure and best practices
- **README.md** (root) - Project overview and setup instructions

## Structure

```
feature-dev/
├── README.md                              # This file
├── workflow.json                          # Structured workflow state
├── card-generation-implementation.md      # Card generation feature docs
└── [future-feature]-implementation.md     # Future feature docs
```

## Conventions

### Implementation Documentation
- Start with overview and status
- Include complete timeline with commits
- Document all issues encountered and how they were resolved
- Add code examples for key concepts
- List all tests added
- Note future enhancements

### Workflow JSON
- Keep status fields up to date
- Add new tasks to task_queue
- Move completed items to appropriate status
- Update handoff_notes for context
- Document all blockers

## Best Practices

1. **Be detailed** - Future you (or another agent) will appreciate context
2. **Include examples** - Code snippets, API responses, error messages
3. **Link commits** - Reference specific commits for changes
4. **Track decisions** - Document why choices were made
5. **Note patterns** - Highlight reusable patterns for future features
6. **Update regularly** - Don't wait until the end to document

## Questions?

For questions about:
- **Project architecture** → See CLAUDE.md
- **Testing approach** → See TESTING.md
- **Specific features** → See implementation docs in this directory
- **Current status** → See workflow.json
