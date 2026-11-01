# Graph Report - .  (2026-11-01)

## Corpus Check
- Corpus is ~2,378 words - fits in a single context window. You may not need a graph.

## Summary
- 40 nodes · 42 edges · 8 communities
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 4 edges (avg confidence: 0.9)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Heap Monitoring Service|Heap Monitoring Service]]
- [[_COMMUNITY_Manual Reclaim Action|Manual Reclaim Action]]
- [[_COMMUNITY_Settings and State Persistence|Settings and State Persistence]]
- [[_COMMUNITY_Reclaim Features and Policies|Reclaim Features and Policies]]
- [[_COMMUNITY_Startup Activity Lifecycle|Startup Activity Lifecycle]]
- [[_COMMUNITY_Plugin Architecture and Spec|Plugin Architecture and Spec]]

## God Nodes (most connected - your core abstractions)
1. `FreeMemService` - 7 edges
2. `FreeMemSettingsState` - 7 edges
3. `FreeMemAction` - 5 edges
4. `Automatic Reclaim` - 5 edges
5. `Manual Reclaim` - 4 edges
6. `State` - 3 edges
7. `FreeMemStartup` - 3 edges
8. `FreeMemService` - 3 edges
9. `getInstance()` - 2 edges
10. `freemem` - 2 edges

## Surprising Connections (you probably didn't know these)
- `FreeMemService` --implements--> `Automatic Reclaim`  [INFERRED]
  README.md → README.md  _Bridges community 3 → community 5_

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **FreeMem Core Plugin Architecture** — readme_freememservice, readme_freememaction, readme_freememsettingsstate, readme_freememstartup [EXTRACTED 1.00]
- **Memory Reclamation Flow** — readme_automatic_reclaim, readme_manual_reclaim, readme_drop_psi_caches, readme_close_idle_tabs [EXTRACTED 1.00]

## Communities (8 total, 0 thin omitted)

### Community 0 - "Heap Monitoring Service"
Cohesion: 0.25
Nodes (4): com, FreeMemService, Int, ScheduledFuture

### Community 1 - "Manual Reclaim Action"
Cohesion: 0.38
Nodes (3): AnAction, AnActionEvent, FreeMemAction

### Community 2 - "Settings and State Persistence"
Cohesion: 0.48
Nodes (4): FreeMemSettingsState, getInstance(), State, PersistentStateComponent

### Community 3 - "Reclaim Features and Policies"
Cohesion: 0.47
Nodes (6): Automatic Reclaim, Close Idle Tabs, Drop PSI Caches, freemem, FreeMemAction, Manual Reclaim

### Community 4 - "Startup Activity Lifecycle"
Cohesion: 0.40
Nodes (3): FreeMemStartup, Project, ProjectActivity

### Community 5 - "Plugin Architecture and Spec"
Cohesion: 0.50
Nodes (4): freemem.xml Configuration, FreeMemService, FreeMemSettingsState, FreeMemStartup

## Knowledge Gaps
- **7 isolated node(s):** `AnActionEvent`, `ScheduledFuture`, `Int`, `Project`, `freemem.xml Configuration` (+2 more)
  These have ≤1 connection - possible missing edges or undocumented components.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Automatic Reclaim` connect `Reclaim Features and Policies` to `Plugin Architecture and Spec`?**
  _High betweenness centrality (0.033) - this node is a cross-community bridge._
- **Why does `FreeMemService` connect `Plugin Architecture and Spec` to `Reclaim Features and Policies`?**
  _High betweenness centrality (0.027) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `Automatic Reclaim` (e.g. with `Manual Reclaim` and `FreeMemService`) actually correct?**
  _`Automatic Reclaim` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 2 inferred relationships involving `Manual Reclaim` (e.g. with `Automatic Reclaim` and `FreeMemAction`) actually correct?**
  _`Manual Reclaim` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AnActionEvent`, `ScheduledFuture`, `Int` to the rest of the system?**
  _8 weakly-connected nodes found - possible documentation gaps or missing edges._