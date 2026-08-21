# Text UI test plan

This file records how the chatbot's text user interface is tested, and every
test case that is run against it. It is the single source of truth for the
`test-ui` skill: the runner reads the settings and the test cases below, so
adding a test case here is all that is needed to have it run.

## Program under test

- **Main class:** `Bob`
- **Source directory:** `src/main/java`

The program is compiled fresh into a temporary directory before the session
starts, and is started again for each test case, so no test case can be
affected by tasks left over from an earlier one.

## How to run the tests

From the repository root, with Java 25 active (`sdk use java 25.0.3.fx-zulu`):

```bash
python3 .claude/skills/test-ui/scripts/run-ui-tests.py
```

Add `--only TC3` to run a single test case while working on it.

## How output is compared

Each test case feeds its **Input** lines to the program on standard input, as
if the user had typed them, and compares everything the program prints with
the test case's **Expected output**.

The comparison is exact, except that trailing spaces at the end of a line and
blank lines at the very end of the output are ignored. Those are invisible on
screen, so a difference in them is not a real difference in what the user sees.
This matters here because the ASCII-art banner lines end in spaces.

The session stops at the first failing test case and reports the expected and
actual output side by side, so the failure stays visible instead of scrolling
away behind later test cases.

## Test cases

### TC1 - Greeting and farewell

**Aim:** Check that the chatbot greets the user on startup and says goodbye
when the user types `bye`, with nothing in between.

**Input**

```text
bye
```

**Expected output**

```text
    ____________________________________________________________
      ____        _
     | __ )  ___ | |__
     |  _ \ / _ \| '_ \
     | |_) | (_) | |_) |
     |____/ \___/|_.__/
     Hello! I'm Bob.
     What can I do for you?
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

### TC2 - Add one task of each type and list them

**Aim:** Check that `todo`, `deadline` and `event` each store a task of the right
kind, confirm it with its own display form and the running task count, and
that `list` prints them in the order they were added, numbered from 1.

**Input**

```text
todo read book
deadline return book /by Sunday
event project meeting /from Mon 2pm /to 4pm
list
bye
```

**Expected output**

```text
    ____________________________________________________________
      ____        _
     | __ )  ___ | |__
     |  _ \ / _ \| '_ \
     | |_) | (_) | |_) |
     |____/ \___/|_.__/
     Hello! I'm Bob.
     What can I do for you?
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [T][ ] read book
     Now you have 1 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [D][ ] return book (by: Sunday)
     Now you have 2 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [E][ ] project meeting (from: Mon 2pm to: 4pm)
     Now you have 3 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Here are the tasks in your list:
     1.[T][ ] read book
     2.[D][ ] return book (by: Sunday)
     3.[E][ ] project meeting (from: Mon 2pm to: 4pm)
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

### TC3 - Mark a task as done and undo it

**Aim:** Check that `mark 1` sets the task's status box to `[X]` and `unmark 1`
sets it back to `[ ]`, that each is confirmed with the right wording, and that
the change is visible in a following `list`. The type box is unaffected.

**Input**

```text
todo read book
list
mark 1
list
unmark 1
list
bye
```

**Expected output**

```text
    ____________________________________________________________
      ____        _
     | __ )  ___ | |__
     |  _ \ / _ \| '_ \
     | |_) | (_) | |_) |
     |____/ \___/|_.__/
     Hello! I'm Bob.
     What can I do for you?
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [T][ ] read book
     Now you have 1 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Here are the tasks in your list:
     1.[T][ ] read book
    ____________________________________________________________

    ____________________________________________________________
     Nice! I've marked this task as done:
       [T][X] read book
    ____________________________________________________________

    ____________________________________________________________
     Here are the tasks in your list:
     1.[T][X] read book
    ____________________________________________________________

    ____________________________________________________________
     OK, I've marked this task as not done yet:
       [T][ ] read book
    ____________________________________________________________

    ____________________________________________________________
     Here are the tasks in your list:
     1.[T][ ] read book
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

### TC4 - List when nothing has been added

**Aim:** Check that `list` on an empty task store says so instead of printing
an empty numbered list.

**Input**

```text
list
bye
```

**Expected output**

```text
    ____________________________________________________________
      ____        _
     | __ )  ___ | |__
     |  _ \ / _ \| '_ \
     | |_) | (_) | |_) |
     |____/ \___/|_.__/
     Hello! I'm Bob.
     What can I do for you?
    ____________________________________________________________

    ____________________________________________________________
     You haven't told me about any tasks yet.
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

### TC5 - Bad arguments to mark, and an unrecognised line

**Aim:** Check that the chatbot survives a task number that does not exist and a task
number that is not a number, reporting each instead of crashing. Also checks
that a line which is not one of the known commands is reported as such: now
that every task is added with `todo`, `deadline` or `event`, a bare line is no
longer stored as a task the way it used to be.

**Input**

```text
todo read book
mark 5
mark two
unmark
borrow book
bye
```

**Expected output**

```text
    ____________________________________________________________
      ____        _
     | __ )  ___ | |__
     |  _ \ / _ \| '_ \
     | |_) | (_) | |_) |
     |____/ \___/|_.__/
     Hello! I'm Bob.
     What can I do for you?
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [T][ ] read book
     Now you have 1 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     I don't have a task numbered 5.
    ____________________________________________________________

    ____________________________________________________________
     Please tell me which task to mark, for example: mark 2
    ____________________________________________________________

    ____________________________________________________________
     Sorry, I don't know what "unmark" means.
     Try one of: todo, deadline, event, list, mark, unmark, bye
    ____________________________________________________________

    ____________________________________________________________
     Sorry, I don't know what "borrow book" means.
     Try one of: todo, deadline, event, list, mark, unmark, bye
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

### TC6 - Input ends without bye

**Aim:** Check that the chatbot still says goodbye and exits normally when the
input runs out before a `bye` line, which is what happens when input is piped
in from a file rather than typed.

**Input**

```text
todo read book
```

**Expected output**

```text
    ____________________________________________________________
      ____        _
     | __ )  ___ | |__
     |  _ \ / _ \| '_ \
     | |_) | (_) | |_) |
     |____/ \___/|_.__/
     Hello! I'm Bob.
     What can I do for you?
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [T][ ] read book
     Now you have 1 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

### TC7 - Missing description or date parts

**Aim:** Check that an add command with a missing description, a missing `/by`, or a
missing `/to` is answered with an example of the right form instead of
storing a half-filled task or crashing.

**Input**

```text
todo
deadline return book
deadline /by Sunday
event project meeting /from Mon 2pm
list
bye
```

**Expected output**

```text
    ____________________________________________________________
      ____        _
     | __ )  ___ | |__
     |  _ \ / _ \| '_ \
     | |_) | (_) | |_) |
     |____/ \___/|_.__/
     Hello! I'm Bob.
     What can I do for you?
    ____________________________________________________________

    ____________________________________________________________
     Please tell me what the todo is, for example: todo borrow book
    ____________________________________________________________

    ____________________________________________________________
     Please say when it is due, for example: deadline return book /by Sunday
    ____________________________________________________________

    ____________________________________________________________
     Please give both a description and a due date, for example: deadline return book /by Sunday
    ____________________________________________________________

    ____________________________________________________________
     Please say when it starts and ends, for example: event project meeting /from Mon 2pm /to 4pm
    ____________________________________________________________

    ____________________________________________________________
     You haven't told me about any tasks yet.
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

### TC8 - Dates and times are kept as free text

**Aim:** Check that whatever the user writes after `/by`, `/from` and `/to` is stored
and shown back unchanged, since at this stage dates are plain strings and are
not interpreted as real dates.

**Input**

```text
deadline do homework /by no idea :-p
event orientation week /from 4/10/2019 /to 11/10/2019
list
bye
```

**Expected output**

```text
    ____________________________________________________________
      ____        _
     | __ )  ___ | |__
     |  _ \ / _ \| '_ \
     | |_) | (_) | |_) |
     |____/ \___/|_.__/
     Hello! I'm Bob.
     What can I do for you?
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [D][ ] do homework (by: no idea :-p)
     Now you have 1 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [E][ ] orientation week (from: 4/10/2019 to: 11/10/2019)
     Now you have 2 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Here are the tasks in your list:
     1.[D][ ] do homework (by: no idea :-p)
     2.[E][ ] orientation week (from: 4/10/2019 to: 11/10/2019)
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```
