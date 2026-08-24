# Project context

This repository is a starter template for a greenfield Java project used in an introductory software engineering course in an undergraduate computer science program. Students use it as the starting point for their own projects.

# Default user context

Unless the user says otherwise, assume that you are assisting a student working on a project in this repository. If the user identifies themselves as an instructor or another project stakeholder, adapt your response to that role.

# Student profile

* Prior knowledge: Basic Java and OOP concepts.
* Level of programming experience: [to be filled]
* IDE and level of expertise: [to be filled]

# Guidance for interacting with users

* Explain the rationale for significant actions: what you did and why.
* Keep explanations brief but instructive, supporting learning through responsible use of AI. For example:

  * When suggesting a Git command, briefly explain what it does.
  * Add explanatory Javadoc comments to all classes and to nontrivial methods and fields when their purpose or behavior is not obvious.
  * Make generated code as self-explanatory as possible, and include explanatory comments where they improve understanding.
  * When faced with a design choice, choose the simplest option that is sufficient for the requirements, while briefly explaining relevant more advanced alternatives.

# Project-specific requirements

## Java version:

Ensure that Java 25 is used when running the application or build tasks. On macOS, use `sdk use java 25.0.3.fx-zulu` to switch to Java 25 if needed.

## Coding standard

All Java code in this repository must follow the SE-EDU Java coding standard
(basic + intermediate rules), recorded in the project skill
`seedu-java-coding-standard` and published at
<https://se-education.org/guides/conventions/java/intermediate.html>.

This is not optional and applies to every Java file under `src/` and `test/`,
whether newly written or edited:

1. Invoke the `seedu-java-coding-standard` skill before writing or changing any
   Java code, so the rules are in hand while the code is being written rather
   than applied as a clean-up afterwards.
2. Write the code compliant the first time. Code that is handed back with
   violations in it is not finished.
3. Any Java file you touch must come out compliant, including parts of it that
   the task did not itself change. Do not, however, reformat whole files that
   the task never asked about.
4. Before reporting a Java change as complete, work through the review
   checklist at the end of that skill.
5. Use the Google Java Style Guide for anything the standard does not cover.

If following the standard would require a change the user has not asked for
(for example moving classes into a package), say so and explain why, rather
than quietly leaving the violation in place.

## Testing after a code change

After any change to the code under `src/`, and before reporting the change as complete:

1. Update `test/ui-test-plan.md` if the change altered or added behaviour the user can see at the console. Revise the expected output of the affected test cases, and add a test case for behaviour that is new. A change that only affects comments, formatting, or internals the user cannot observe needs no update to the plan.
2. Invoke the `test-ui` skill to run the test plan against the changed program.
3. Show the resulting test session and state the outcome. A code change is not finished until its test cases pass.

If a test case fails, report the failing test case with its expected and actual output before changing anything further, and say which side is at fault: the code (a bug introduced by the change) or the plan (an expected output that was never updated for an intended change). Never edit the expected output merely to make a failing test pass — that hides the very problem the test found.

## Git

Do not commit or push unless explicitly asked. Use lightweight tags unless the
user requests an annotated tag.

### Git conventions

All commits and branches in this repository must follow the SE-EDU Git
conventions, recorded in the project skill `seedu-git-standard` and published
at <https://se-education.org/guides/conventions/git.html>.

This is not optional and applies to every commit message you write or propose,
from now on:

1. Invoke the `seedu-git-standard` skill before drafting any commit message or
   creating any branch, so the rules are in hand while the message is being
   written rather than applied as a clean-up afterwards.
2. Get the message right the first time. In particular: a subject line in the
   imperative mood, capitalized, with no trailing period, within 50 characters
   (72 at the absolute most); and for any non-trivial commit a body, separated
   from the subject by a blank line and hard-wrapped at 72 characters.
3. The body must explain WHAT changed and WHY, not HOW — the reader has the
   diff. Include enough detail that the change can be judged without opening
   the diff, following the structure the skill sets out: current situation, why
   it needs to change, what is being done about it, why it is done that way.
4. Before showing the user a commit message, work through the review checklist
   at the end of that skill.

If a commit's message is growing long and sprawling, that is a sign the commit
should be split into finer-grained pieces; say so rather than writing a message
that tries to cover everything at once.
