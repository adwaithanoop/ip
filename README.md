# Bob

Bob is a chatbot that keeps track of your todos, deadlines and events. You chat with it in a window, typing
commands such as `todo read book` or `deadline return book /by 2026-12-02`, and it saves your list so that the
list is still there the next time you start it.

See the [User Guide](docs/README.md) for the commands Bob understands.

## Running Bob

Prerequisites: JDK 25, on Windows or Linux with an Intel or AMD processor, or on an Apple silicon Mac. JavaFX does
not need to be installed separately: Gradle downloads it, but only the builds for those machines, so the chat window
does not start on others, such as an Intel Mac.

* **The chat window:** run `./gradlew run` from the project folder (`gradlew.bat run` on Windows).
* **The console version:** run the `main` method of `bob.Bob`, for example from IntelliJ as described below.
* **A single JAR file:** run `./gradlew shadowJar` to build `build/libs/bob.jar`, then start it with
  `java -jar build/libs/bob.jar`.

Bob keeps your tasks in `data/duke.txt`, inside the folder it is started from.

## Setting up in IntelliJ

Prerequisites: JDK 25, update IntelliJ to the most recent version.

1. Open IntelliJ (if you are not in the welcome screen, click `File` > `Close Project` to close the existing project first)
1. Open the project into IntelliJ as follows:
   1. Click `Open`.
   1. Select the project directory, and click `OK`.
   1. If there are any further prompts, accept the defaults.
1. Configure the project to use **JDK 25** (not other versions) as explained in [here](https://www.jetbrains.com/help/idea/sdk.html#set-up-jdk).<br>
   In the same dialog, set the **Project language level** field to the `SDK default` option.
1. After that, locate the `src/main/java/bob/Launcher.java` file, right-click it, and choose `Run Launcher.main()` (if the code editor is showing compile errors, try restarting the IDE). If the setup is correct, the chat window opens with Bob's greeting.
   To use the console version instead, do the same with `src/main/java/bob/Bob.java`.

**Warning:** Keep the `src\main\java` folder as the root folder for Java files (i.e., don't rename those folders or move Java files to another folder outside of this folder path), as this is the default location some tools (e.g., Gradle) expect to find Java files.

## Testing

* **JUnit tests:** run `./gradlew test`.
* **Text UI tests:** run `python3 .claude/skills/test-ui/scripts/run-ui-tests.py`. It runs the console version against
  every test case in [`test/ui-test-plan.md`](test/ui-test-plan.md) and compares what Bob prints with the expected
  output.
* **Continuous integration:** GitHub Actions runs `./gradlew check` on Windows, macOS and Linux for every push and
  pull request, as set up in [`.github/workflows/gradle.yml`](.github/workflows/gradle.yml).

## Acknowledgements

### Credits

* **Mouse wheel scrolling in the chat window:** the fix in `bob.ui.MainWindow`, which keeps the newest
  message in view without binding the scroll position, so that the mouse wheel and trackpad can still
  scroll the conversation, comes from the forum post
  [Enabling mouse wheel & trackpad scrolling in JavaFX ScrollPane](https://github.com/NUS-CS2103-AY2627-S1/forum/issues/160)
  by Kieran M ([@Kimame04](https://github.com/Kimame04)). Thanks Kieran!

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
