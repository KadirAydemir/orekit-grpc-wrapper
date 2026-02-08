---
description: Acts as an Architect to plan changes and a Reviewer to verify them, leaving implementation to others.
---

# Architect & Review Workflow

This workflow is designed for a split-role process where this agent plans and verifies, but the actual coding is done by the user or another agent.

## 1. Planning (Architect Mode)
- **Role**: Software Architect
- **Goal**: Analyze the request and create a detailed blueprint.
- **Actions**:
  1.  Explore the codebase to understand the context.
  2.  **Review Rules**: Check `.agent/rules/agent.md` and other relevant rules.
  3.  **Create Artifacts**:
      -   Create `.agent/<task_name>/task.md`.
      -   Create `.agent/<task_name>/implementation_plan.md` using `write_to_file`.
      -   **Create Prompt**: Create a file named `.agent/<task_name>/prompt.md` containing:
          > "Be a Junior Developer on the Orekit project.
          > Follow the rules in `.agent/rules/agent.md` STRICTLY.
          > Read the plan at `.agent/<task_name>/implementation_plan.md`.
          > Implement the changes exactly as described.
          > Update `.agent/<task_name>/task.md` as you complete items.
          > If you have questions, stop and ask."
  4.  **STOP** and inform user: "Plan created. Tell your local agent to read `.agent/<task_name>/prompt.md`".

## 2. Implementation (External)
- **Role**: User / Other Agent (e.g., Local LLM)
- **Goal**: Implement the changes defined in `.agent/<task_name>/implementation_plan.md`.
- **How to Start**: Simply tell your local agent:
  > **"Read and execute the instructions in `.agent/<task_name>/prompt.md`"**
- **Actions**:
  -   The user or another agent writes the code.
  -   This agent waits for the user to say "Implementation complete" or "Review this".

## 3. Verification (Reviewer Mode)
- **Role**: Code Reviewer / QA
- **Goal**: Ensure the implementation matches the plan and works correctly.
- **Actions**:
  1.  Identify the task directory (ask user if unsure).
  2.  Read the modified files.
  3.  Run verification steps from `.agent/<task_name>/implementation_plan.md`.
  4.  If issues are found:
      -   Report them clearly.
      -   Suggest fixes.
  5.  If successful:
      -   Create `walkthrough.md` to document the results.
      -   **Clean Up**: Run `rm -rf .agent/<task_name>` to delete the temporary planning files.
      -   Mark the task as complete.
