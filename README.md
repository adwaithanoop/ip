# Bob project template

This is a project template for a greenfield Java project. Given below are instructions on how to use it.

## Setting up in Intellij

Prerequisites: JDK 25, update Intellij to the most recent version.

1. Open Intellij (if you are not in the welcome screen, click `File` > `Close Project` to close the existing project first)
1. Open the project into Intellij as follows:
   1. Click `Open`.
   1. Select the project directory, and click `OK`.
   1. If there are any further prompts, accept the defaults.
1. Configure the project to use **JDK 25** (not other versions) as explained in [here](https://www.jetbrains.com/help/idea/sdk.html#set-up-jdk).<br>
   In the same dialog, set the **Project language level** field to the `SDK default` option.
1. After that, locate the `src/main/java/bob/Bob.java` file, right-click it, and choose `Run Bob.main()` (if the code editor is showing compile errors, try restarting the IDE). If the setup is correct, you should see something like the below as the output:
   ```
       ____________________________________________________________
          .        *         .        .        *        .
              *         .         +        .       <]==-     .
           .        +        ____        __      .        *
         -==[>  *           / __ )____  / /_         +
         +           .     / __  / __ \/ __ \  *              .
                  *       / /_/ / /_/ / /_/ /   <]==-   .
            .         +  /_____/\____/_.___/       .        *
                +         .         *        .        +        .
           .        -==[>      .         *                 .
        Hello! I'm Bob.
        What can I do for you?
       ____________________________________________________________
   ```

**Warning:** Keep the `src\main\java` folder as the root folder for Java files (i.e., don't rename those folders or move Java files to another folder outside of this folder path), as this is the default location some tools (e.g., Gradle) expect to find Java files.

## Acknowledgements

### Use of AI

Parts of this project were written with the help of an AI coding assistant, in line with the
course's [policy on citing AI-generated/assisted work](https://nus-cs2103-ay2627-s1.github.io/website/admin/appendixB-policies.html).

* **Tool used:** Claude Code (Anthropic), models Claude Opus 5.
* **Used by:** Adwaith Anoop, the sole author of this project.
* **Extent:** AI assistance was used across most increments from Level 1 to Level 8,
  and for the A-MoreOOP increment. It contributed to the Java sources under
  `src/main/java/`, the UI test plan in
  `test/ui-test-plan.md`. Where the assistance was confined to a specific method or
  block, it is also noted in a comment at that point in the code. AI was also used 
  significantly in A-MoreOOP. 
* **How it was used:** "For earlier levels I practised using the course's suggested
  prompts. Later on, I described the increment, reviewed the AI's implementation line
  by line, and revised it before committing" (level AI-5). For A-MoreOOP I gave the
  increment's requirements and asked for the work to proceed one self-contained step
  at a time, each step tested against `test/ui-test-plan.md` and committed separately,
  with the assistant explaining its design choices and the trade-offs so I could
  accept or challenge them.
* **What I checked:** I reviewed the changes made line by line and ran test cases in `test/ui-test-plan.md`
  after every change. I am also actively revisiting past commits and comparing with coding principles I have learnt
  from CS2030S and week 1's pre-req material.

The `.claude/` directory in this repository holds project instructions and a test-running skill for
the assistant. These configure how the AI works on this project and are not part of the chatbot.
