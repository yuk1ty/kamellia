---
description: >
  This document defines the mandatory development process for the project.
  Its purpose is to ensure robust and reliable development by enforcing
  a fixed set of steps that MUST be followed during coding.
  All listed checks MUST be performed and verified every time.
globs:
alwaysApply: true
---

# General Development Process

## Purpose

This document defines the mandatory development process for the project.
By strictly following these rules, robust and reliable development is ensured.
It specifies steps that MUST be executed during coding and checks that MUST
be verified on every single implementation.

## Steps

1. The implementation MUST begin by strictly following the instructions
   provided in the user’s prompt.
   - If there are any unclear or ambiguous points, the assistant MUST stop
     and explicitly confirm them with the user.
   - When asking for clarification, the assistant MUST present multiple
     possible options along with their background or rationale.
   - The assistant MUST NOT proceed with implementation based on its own
     assumptions or unilateral decisions.

2. Once the assistant believes the implementation is complete, it MUST
   perform the following verification steps in the order listed below:
   1. Run `./gradlew compileTestKotlin`
      - This step MUST be executed to verify that the entire project
        compiles successfully.

   2. Run `test-runner` subagent.

   3. Run `code-quality-checker` subagent.
