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

### TC5 - Bad arguments to mark and unmark, and an unrecognised line

**Aim:** Check that a task number that does not exist, a task number that is not a
number, and a `mark`/`unmark` with no number at all are each answered with their
own explanation instead of crashing or being lumped together. Also checks that a
line which is not one of the known commands is reported as such: now that every
task is added with `todo`, `deadline` or `event`, a bare line is no longer stored
as a task the way it used to be.

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
     Your list runs from 1 to 1; type list to see it.
    ____________________________________________________________

    ____________________________________________________________
     "two" isn't a task number.
     I need the number shown next to the task in list, for example: mark 2
    ____________________________________________________________

    ____________________________________________________________
     Which task should I unmark?
     Give me its number from list, for example: unmark 2
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
missing `/to` is answered with an explanation of the part that is missing and an
example of the right form, instead of storing a half-filled task or crashing. The
`list` at the end checks that nothing was stored by the refused commands.

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
     A todo needs a description — tell me what to do.
     For example: todo borrow book
    ____________________________________________________________

    ____________________________________________________________
     A deadline needs a due date, written after /by.
     For example: deadline return book /by Sunday
    ____________________________________________________________

    ____________________________________________________________
     A deadline needs a description, written before /by.
     For example: deadline return book /by Sunday
    ____________________________________________________________

    ____________________________________________________________
     An event needs an end time, written after /to at the end.
     For example: event project meeting /from Mon 2pm /to 4pm
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

### TC9 - The remaining ways an add command can be incomplete

**Aim:** Check the malformed add commands that TC7 does not cover — a `/by` with
nothing after it, an event with no description, and an event whose `/from` is
missing while `/to` is present — each get the explanation that fits them, rather
than one catch-all message. Also checks that `todolist` is treated as an unknown
command and not as a `todo` whose description is `list`, and that `mark` on an
empty list says the list is empty rather than quoting a range of task numbers
that does not exist.

**Input**

```text
deadline return book /by
event /from Mon /to Tue
event party /to 4pm
todolist
mark 0
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
     You wrote /by but not when it is due.
     For example: deadline return book /by Sunday
    ____________________________________________________________

    ____________________________________________________________
     An event needs a description, written before /from.
     For example: event project meeting /from Mon 2pm /to 4pm
    ____________________________________________________________

    ____________________________________________________________
     An event needs a start time, written after /from.
     For example: event project meeting /from Mon 2pm /to 4pm
    ____________________________________________________________

    ____________________________________________________________
     Sorry, I don't know what "todolist" means.
     Try one of: todo, deadline, event, list, mark, unmark, bye
    ____________________________________________________________

    ____________________________________________________________
     There is nothing to mark yet — your list is empty.
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

### TC10 - Empty and blank input lines

**Aim:** Check that pressing Enter on its own, or typing only spaces, is answered
with a prompt to type something rather than with "I don't know what "" means",
and that the conversation carries on normally afterwards.

**Input**

```text

   
todo read book
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
     You didn't type anything.
     Tell me about a task, or type list to see the ones I already have.
    ____________________________________________________________

    ____________________________________________________________
     You didn't type anything.
     Tell me about a task, or type list to see the ones I already have.
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
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

### TC11 - Refused commands leave the list untouched

**Aim:** Check that errors do not quietly change what the chatbot has stored.
Good and bad commands are interleaved, and the running task count in each
confirmation, together with the `list` at the end, shows that every refused
command added nothing and marked nothing. In particular the failed `mark 3`
must not affect the task that `mark 2` later marks.

**Input**

```text
todo read book
todo
deadline return book /by Sunday
deadline /by
mark 3
mark 2
event project meeting /from Mon 2pm
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
     A todo needs a description — tell me what to do.
     For example: todo borrow book
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [D][ ] return book (by: Sunday)
     Now you have 2 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     A deadline needs a description, written before /by.
     For example: deadline return book /by Sunday
    ____________________________________________________________

    ____________________________________________________________
     I don't have a task numbered 3.
     Your list runs from 1 to 2; type list to see it.
    ____________________________________________________________

    ____________________________________________________________
     Nice! I've marked this task as done:
       [D][X] return book (by: Sunday)
    ____________________________________________________________

    ____________________________________________________________
     An event needs an end time, written after /to at the end.
     For example: event project meeting /from Mon 2pm /to 4pm
    ____________________________________________________________

    ____________________________________________________________
     OK, I've marked this task as not done yet:
       [T][ ] read book
    ____________________________________________________________

    ____________________________________________________________
     Here are the tasks in your list:
     1.[T][ ] read book
     2.[D][X] return book (by: Sunday)
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```
