---
name: planning
description: "You MUST use this before beginning any design or development work including writing or modifying code, tests, features, behavior. This MUST be run when the user says: \"Implement [feature/plan]\", \"Build [feature/plan]\", \"Create [code/feature]\", \"Implement [code/feature/plan]\", etc. Determine user intent, requirements, and design before implementation."
metadata:
  author: Michael Aboff (aboff.com)
  version: "1.0"
---

# Plan Before Implementation

Turn initial user and agent defined requests into well designed plan before implementation.

Start by understanding the request and the application context. Ask questions one at a time until you understand the what you are building. Present the plan in small but detailed sections, checking with the user for any feedback before continuing.

## Quick Decision Guide

Before proceeding, answer these questions:
- [ ] Is the user asking me to implement, build, create, or modify code?
- [ ] Has the plan been written to `docs/agent-plans/YYYY-MM-DD-<topic>-design.md`?
- [ ] If NO to the above: STOP and complete documentation first
- [ ] If YES: Proceed with implementation referencing the written plan

## Process

**Understand - Purpose, constraints, and success criteria**
- DO check for the current state of the project (recent committed or modified files)
- DO ask questions one at a time to refine the idea using the AskUserQuestion tool with interactive multiple choice options
- DO break complex questions into separate simpler questions
- DO use the AskUserQuestion tool to present 2-4 options with clear descriptions for each choice

**Explore Approaches**
- DO review the pr for similar situations to determine used patterns
- DO review the application for shared utilities to avoid writing duplicate code, if there is an opportunity to create a shared utility plan for that
- DO prefer using well tested libraries and coding patterns instead of bleeding new options
- DO review your proposals before presenting them
- DO consider testing strategies
- DO consider designing for ease of understanding for human users
- DO use AskUserQuestion tool to propose 2-3 approaches with trade-offs, each option having a clear description
- DO lead with your recommended option (marked as "Recommended") and explain why in the option description

**Presentation**
- DO present your plan in small sections, organized in logical groupings (such as similar models, a controller/model/migration combination, etc)
- DO check with the user after each section for feedback
- DO cover changes to architecture, components, data flow, error handling, testing
- DO present the plan in easy to understand ways (tables, charts, proposed change logs)

## REQUIRED BEFORE ANY IMPLEMENTATION

**BLOCKING REQUIREMENT:** Never begin writing code, tests, or modifying files until these steps are complete:

**Documentation**
- DO write the approved plan to `docs/agent-plans/YYYY-MM-DD-<topic>-design.md`

**Implementation**
- If user approves:
  - DO clear the context
  - DO start implementation from the written design document

## Key Principles
- DO NOT plan modify unrelated files or functions if not necessary
- DO NOT plan for unnecessary features before they are needed (YAGNI)
- DO ask yourself if this approach is overcomplicated or missing anything before presenting
- DO present for incremental validation

### User-Provided Plans
If the user provides a complete or partial plan and asks you to implement it:
- STOP - DO NOT begin implementation immediately
- DO enable plan mode
- DO follow the Planning Process detailed above to clarify the plan
- DO write their plan as detailed above
- DO clear context and acknowledge the transition to implementation mode
- DO reference the written document when implementing

**This applies even if the plan is complete and detailed**

❌ **WRONG**:                                                                                                                                                                                                                     
User: "Implement the following plan: [detailed plan]"                                                                                                                                                                             
Agent: [immediately starts coding]

✅ **CORRECT**:                                                                                                                                                                                                                   
User: "Implement the following plan: [detailed plan]"                                                                                                                                                                             
Agent:
1. Writes plan to docs/agent-plans/2026-01-31-feature-design.md
2. "I've documented the plan. Clearing context to begin implementation."
3. Starts implementation referencing the written document                                                                                                                                                                         
                                                              