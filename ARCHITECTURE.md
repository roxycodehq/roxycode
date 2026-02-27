
# 🏗 RoxyCode Architecture

RoxyCode is an AI-powered software engineering assistant designed to evolve complex Java codebases. It combines high-level architectural planning with low-level code execution in a secure sandbox.

## 🚀 High-Level Overview

The system is built on a "Plan-then-Act" philosophy. It uses Google's Gemini Pro models and their long-context window capability to ingest entire codebases, allowing for deep contextual understanding.

### Core Philosophy
1.  **Codebase as Context**: The entire project is converted into a structured XML representation ("Smash") and cached in Gemini.
2.  **Autonomous Agents**: Specific personas (Analyst, Coder, Researcher) handle different stages of the development lifecycle.
3.  **Sandboxed Execution**: AI agents don't just "suggest" code; they execute JavaScript scripts in a GraalJS sandbox to perform file I/O, builds, and tests.

---

## 📦 Module Breakdown

### 1. `jsmashy-lib` (The Scanner)
The foundation of RoxyCode's context engine.
-   **RepositoryScanner**: Recursively crawls the filesystem.
-   **Language Analyzers**: Uses **ANTLR4** (for Java) to "skeletonize" code. It preserves class/method signatures and Javadoc while stripping implementation details to save tokens.
-   **XmlSmashFormatter**: Converts the scanned project into a single, structured XML document suitable for LLM consumption.

### 2. `roxycode-gui` (The Orchestrator)
A JavaFX application powered by the **Micronaut** framework.
-   **GeminiService**: Manages API communication, including the creation and lifecycle of `CachedContent`.
-   **ChatService**: Manages the multi-turn agentic loop. It handles streaming responses and intercepts "Function Calls" from the LLM.
-   **AgentScriptService**: The execution engine. It hosts a GraalJS `Context` where the agent's code runs. It exposes Java services (like `textEditorService`) as JS globals.
-   **PlanService**: A state machine that tracks the progress of "Plans" (Functional Requirements, Technical Constraints, Implementation Steps).

---

## 🤖 Agent & Sandbox Integration

### The Agentic Loop
RoxyCode uses Gemini's **Function Calling** to bridge the gap between "thinking" and "doing."
1.  The agent decides to perform an action (e.g., read a file).
2.  Gemini emits a `execute_js(script)` call.
3.  The `ChatService` intercepts this, passes the script to `AgentScriptService`.
4.  The script runs in the sandbox, interacting with the real project via provided APIs.
5.  Results (logs, return values, errors) are sent back to Gemini to inform the next turn.

### Sandbox Security & Constraints
-   **GraalJS**: Used for high-performance JS execution within the JVM.
-   **Restricted Access**: Standard Node.js modules (`fs`, `path`) are unavailable. Agents must use the curated `textEditorService`, `javaBuildToolService`, etc.
-   **Plan-Act Mandate**: Agents are restricted in what they can do based on their current role (e.g., a RESEARCH agent cannot modify files).

---

## 🔄 Data Flow

1.  **Ingestion**: User selects a project → `jsmashy` scans → XML generated → XML uploaded to Gemini as `CachedContent`.
2.  **Planning**: User submits a task → **ANALYST** agent researches and creates a `.toml` plan at `roxy/plans/`.
3.  **Execution**: User approves plan → **CODER** agent executes implementation steps one by one, verifying each with builds and tests.
4.  **Completion**: Plan steps are marked complete → User reviews the final output.

---

## 🛠 Areas for Improvement & Roadmap

### Technical Debt
-   **Language Support**: The skeletonization logic is currently robust only for Java. Other languages are treated as raw text.
-   **Single Plan Limit**: The system currently handles one active plan at a time.
-   **Direct JS-Java Bridge**: Some complex Java objects returned to JS don't serialize well to JSON; better DTO mapping is needed.

### Future Features
-   **Git Integration**: Native support for creating branches and committing changes directly from the agent.
-   **UI Visualization**: A more interactive view of the "Project Tree" and "Plan Progress."
-   **Multi-Agent Collaboration**: Allowing the Analyst and Coder to "talk" to each other to resolve plan ambiguities.
