# 🤖 RoxyCode

> [!WARNING]
> **HEAVY DEVELOPMENT NOTICE:** This project is currently under heavy development and is **not yet ready for production use**. APIs, features, and UI are subject to breaking changes without notice.

RoxyCode is a cross-platform AI-powered coding assistant designed to help developers evolve their codebases with high autonomy and precision.

## 🏗 Project Structure

The project is divided into several modules, each serving a specific purpose in the RoxyCode ecosystem:

### 🧩 JSmashy (The Core Engine)
JSmashy is the underlying engine that prepares codebases for consumption by Large Language Models (LLMs). It "smashes" the repository into a structured, LLM-friendly format while optionally "skeletonizing" files to save tokens.

*   **`jsmashy-lib`**: The core library containing the repository scanner, `.gitignore` logic, and language-specific analyzers (e.g., Java ANTLR4 parser).
*   **`jsmashy-cli`**: A command-line interface for JSmashy, allowing users to generate codebase "smash" files (XML/Text) for use with external LLM tools.

### 🖼 RoxyCode GUI
*   **`roxycode-gui`**: A JavaFX-based desktop application that provides an integrated chat interface. It leverages Gemini's long-context capabilities to provide deep codebase insights and assistant features.

---

## 🚀 Usage

### JSmashy CLI

#### Running with Maven
To run the JSmashy CLI from source using Maven, use the following command from the project root:

```bash
mvn exec:java -pl jsmashy-cli -Dexec.mainClass="org.roxycode.jsmashy.cli.Main" -Dexec.args="<input-dir> <output-file>"
```

#### Convenience Script
A convenience script `jsmashy.sh` is provided in the root directory for easier access:

```bash
./jsmashy.sh <input-dir> <output-file>
```

### RoxyCode GUI

To launch the desktop application:

```bash
mvn javafx:run -pl roxycode-gui
```

---

## 🛠 Tech Stack
*   **Java 21**
*   **Maven** (Build Tool)
*   **JavaFX** (GUI Framework)
*   **Micronaut** (Dependency Injection)
*   **ANTLR4** (Source Code Analysis)
*   **Gemini AI** (LLM Service)

---

## 📄 License
This project is licensed under the terms of the LICENSE file found in the root directory.
