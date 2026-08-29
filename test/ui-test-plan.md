# Text UI test plan

This file records how the chatbot's text user interface is tested, and every
test case that is run against it. It is the single source of truth for the
`test-ui` skill: the runner reads the settings and the test cases below, so
adding a test case here is all that is needed to have it run.

## Program under test

- **Main class:** `bob.Bob`
- **Source directory:** `src/main/java`
- **Data file:** `data/duke.txt`

The program is compiled fresh into a temporary directory before the session
starts, and is started again for each test case, so no test case can be
affected by tasks left over from an earlier one.

Each test case also runs in a working directory of its own. The program keeps
its task list in the data file named above, relative to the directory it is
started in, so a directory per test case is what stops one test case from
loading the tasks another one saved — and it keeps a test run from writing
anything into the repository.

The one test case that has the program print the path of the data file (TC22)
expects it written with `/` between the folder and the file name. That is how
the path prints on macOS and Linux; on Windows the same path prints as
`data\duke.txt`, because the program builds it from its parts and lets the
operating system supply the separator. On Windows, expect that test case to
report a difference in those four lines only.

## The save file

Because the task list now outlives a run of the program, a test case may say
what is on the disk on either side of the run:

- a **Data file before** block is written to `data/duke.txt` before the program
  starts, standing for tasks saved in an earlier session;
- a **Data file after** block is compared with `data/duke.txt` once the program
  has finished, and the test case fails if it does not match. The block may be
  the single line `(no file)`, which says the program should have left no save
  file at all.

A test case with neither block starts with no save file — the ordinary first
run — and nothing is checked about what it saves. That is the case for TC1 to
TC16 below, all of which were written before the chatbot saved anything.

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
     Try one of: todo, deadline, event, list, mark, unmark, delete, bye
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
     Try one of: todo, deadline, event, list, mark, unmark, delete, bye
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

### TC12 - Delete a task from the middle of the list

**Aim:** Check the worked example from the requirements: `delete 3` removes the
third task, confirms it by showing the task itself, and reports the number of
tasks left. The `list` afterwards checks that the tasks below the deleted one
move up, so the numbering stays an unbroken run from 1 with no gap where the
deleted task used to be. Showing the removed task in the confirmation matters
precisely because of that renumbering: after a deletion, the number the user
typed no longer refers to the same task.

**Input**

```text
todo read book
deadline return book /by June 6th
event project meeting /from Aug 6th 2pm /to 4pm
todo join sports club
todo borrow book
mark 1
mark 2
mark 4
list
delete 3
list
bye
```

**Expected output**

```text
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

    ____________________________________________________________
     Got it. I've added this task:
       [T][ ] read book
     Now you have 1 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [D][ ] return book (by: June 6th)
     Now you have 2 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [E][ ] project meeting (from: Aug 6th 2pm to: 4pm)
     Now you have 3 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [T][ ] join sports club
     Now you have 4 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [T][ ] borrow book
     Now you have 5 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Nice! I've marked this task as done:
       [T][X] read book
    ____________________________________________________________

    ____________________________________________________________
     Nice! I've marked this task as done:
       [D][X] return book (by: June 6th)
    ____________________________________________________________

    ____________________________________________________________
     Nice! I've marked this task as done:
       [T][X] join sports club
    ____________________________________________________________

    ____________________________________________________________
     Here are the tasks in your list:
     1.[T][X] read book
     2.[D][X] return book (by: June 6th)
     3.[E][ ] project meeting (from: Aug 6th 2pm to: 4pm)
     4.[T][X] join sports club
     5.[T][ ] borrow book
    ____________________________________________________________

    ____________________________________________________________
     Noted. I've removed this task:
       [E][ ] project meeting (from: Aug 6th 2pm to: 4pm)
     Now you have 4 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Here are the tasks in your list:
     1.[T][X] read book
     2.[D][X] return book (by: June 6th)
     3.[T][X] join sports club
     4.[T][ ] borrow book
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

### TC13 - Bad arguments to delete

**Aim:** Check that `delete` rejects the same four mistakes as `mark` and
`unmark` — a task number given on an empty list, no number at all, something
that is not a number, and a number outside the list — each with the explanation
that fits it and naming `delete` rather than another command. The `list` at the
end checks that no refused `delete` removed anything.

**Input**

```text
delete 1
todo read book
delete
delete two
delete 0
delete 2
list
bye
```

**Expected output**

```text
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

    ____________________________________________________________
     There is nothing to delete yet — your list is empty.
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [T][ ] read book
     Now you have 1 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Which task should I delete?
     Give me its number from list, for example: delete 2
    ____________________________________________________________

    ____________________________________________________________
     "two" isn't a task number.
     I need the number shown next to the task in list, for example: delete 2
    ____________________________________________________________

    ____________________________________________________________
     I don't have a task numbered 0.
     Your list runs from 1 to 1; type list to see it.
    ____________________________________________________________

    ____________________________________________________________
     I don't have a task numbered 2.
     Your list runs from 1 to 1; type list to see it.
    ____________________________________________________________

    ____________________________________________________________
     Here are the tasks in your list:
     1.[T][ ] read book
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

### TC14 - Deleting the first and last tasks, emptying the list, and adding again

**Aim:** Check the ends of the list, which the middle deletion in TC12 does not
reach: deleting the first task and deleting the last one. Then check that a task
added after deletions is counted against how many tasks there actually are, not
against how many have ever been added — the count would keep climbing if it were
kept as a separate running total instead of being read from the list itself.
Finally, check that deleting the last remaining task leaves the list genuinely
empty, so `list` says so rather than printing nothing.

**Input**

```text
todo read book
todo return book
todo borrow book
delete 1
delete 2
list
todo join sports club
list
delete 1
delete 1
list
bye
```

**Expected output**

```text
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

    ____________________________________________________________
     Got it. I've added this task:
       [T][ ] read book
     Now you have 1 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [T][ ] return book
     Now you have 2 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [T][ ] borrow book
     Now you have 3 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Noted. I've removed this task:
       [T][ ] read book
     Now you have 2 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Noted. I've removed this task:
       [T][ ] borrow book
     Now you have 1 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Here are the tasks in your list:
     1.[T][ ] return book
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [T][ ] join sports club
     Now you have 2 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Here are the tasks in your list:
     1.[T][ ] return book
     2.[T][ ] join sports club
    ____________________________________________________________

    ____________________________________________________________
     Noted. I've removed this task:
       [T][ ] return book
     Now you have 1 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Noted. I've removed this task:
       [T][ ] join sports club
     Now you have 0 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     You haven't told me about any tasks yet.
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

### TC15 - A deleted task is gone for good, and marking follows the new numbering

**Aim:** Check that the numbers `mark` and `delete` take are read against the
list as it stands *now*, not as it stood before a deletion. After `delete 1`,
`mark 1` must mark what used to be task 2, and the highest number that was valid
before the deletion must no longer be. Also checks that a deleted task does not
come back: nothing in a later `list` shows it.

**Input**

```text
todo read book
deadline return book /by Sunday
event project meeting /from Mon 2pm /to 4pm
delete 1
mark 1
mark 3
list
bye
```

**Expected output**

```text
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
     Noted. I've removed this task:
       [T][ ] read book
     Now you have 2 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Nice! I've marked this task as done:
       [D][X] return book (by: Sunday)
    ____________________________________________________________

    ____________________________________________________________
     I don't have a task numbered 3.
     Your list runs from 1 to 2; type list to see it.
    ____________________________________________________________

    ____________________________________________________________
     Here are the tasks in your list:
     1.[D][X] return book (by: Sunday)
     2.[E][ ] project meeting (from: Mon 2pm to: 4pm)
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

### TC16 - Commands that take no arguments reject trailing text

**Aim:** Check that `list` and `bye`, which stand alone, are recognised only when
typed exactly. `bye now` must not end the conversation and `list foo` must not
list anything; both are unrecognised lines. The commands after them prove the
conversation carried on, which is the visible consequence of `bye now` not being
taken as `bye`. Also checks that command words are case-sensitive, so `BYE` is
not `bye`. This is the counterpart to the `todolist` case in TC9: there, a
command that *does* take arguments must not match a longer word; here, a command
that takes none must not match a longer line.

**Input**

```text
bye now
list foo
BYE
todo read book
list
bye
```

**Expected output**

```text
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

    ____________________________________________________________
     Sorry, I don't know what "bye now" means.
     Try one of: todo, deadline, event, list, mark, unmark, delete, bye
    ____________________________________________________________

    ____________________________________________________________
     Sorry, I don't know what "list foo" means.
     Try one of: todo, deadline, event, list, mark, unmark, delete, bye
    ____________________________________________________________

    ____________________________________________________________
     Sorry, I don't know what "BYE" means.
     Try one of: todo, deadline, event, list, mark, unmark, delete, bye
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

### TC17 - Every change to the list is written to the save file

**Aim:** Check that the tasks are on the disk as soon as they are entered, rather
than only when the chatbot is shut down properly. The save file is checked after
a run that adds one task of each kind and marks one of them, so it also checks
that each kind is written with its own fields and that a `mark` is saved too.
The console output is unchanged by saving: the user is told nothing about a save
that worked, because there is nothing they need to do about it.

**Input**

```text
todo read book
deadline return book /by June 6th
event project meeting /from Aug 6th 2pm /to 4pm
mark 1
bye
```

**Expected output**

```text
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

    ____________________________________________________________
     Got it. I've added this task:
       [T][ ] read book
     Now you have 1 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [D][ ] return book (by: June 6th)
     Now you have 2 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [E][ ] project meeting (from: Aug 6th 2pm to: 4pm)
     Now you have 3 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Nice! I've marked this task as done:
       [T][X] read book
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

**Data file after**

```text
T | 1 | read book
D | 0 | return book | June 6th
E | 0 | project meeting | Aug 6th 2pm | 4pm
```

### TC18 - Tasks saved earlier are there again at startup

**Aim:** Check the other half of the round trip: a save file written by an earlier
session is read back into the list, with each task's kind, done status and dates
as they were saved. The user is told that tasks were picked up, since a list that
was not empty to begin with would otherwise be a surprise. The save file is
checked afterwards as well, to confirm that merely reading and listing tasks does
not rewrite it.

**Data file before**

```text
T | 1 | read book
D | 0 | return book | June 6th
E | 0 | project meeting | Aug 6th 2pm | 4pm
```

**Input**

```text
list
bye
```

**Expected output**

```text
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

    ____________________________________________________________
     Welcome back! I've picked up 3 tasks you saved earlier.
    ____________________________________________________________

    ____________________________________________________________
     Here are the tasks in your list:
     1.[T][X] read book
     2.[D][ ] return book (by: June 6th)
     3.[E][ ] project meeting (from: Aug 6th 2pm to: 4pm)
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

**Data file after**

```text
T | 1 | read book
D | 0 | return book | June 6th
E | 0 | project meeting | Aug 6th 2pm | 4pm
```

### TC19 - Deleting and unmarking are saved, and an emptied list empties the file

**Aim:** Check that the save file follows the list down as well as up. A task
deleted from a loaded list is gone from the file, an `unmark` is written back
just as a `mark` is, and deleting the last task leaves an empty file rather than
a file still holding the task that was deleted. Leaving the old contents behind
would be the failure that matters here: the task would come back at the next
startup.

**Data file before**

```text
T | 1 | read book
D | 0 | return book | June 6th
```

**Input**

```text
unmark 1
delete 2
delete 1
list
bye
```

**Expected output**

```text
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

    ____________________________________________________________
     Welcome back! I've picked up 2 tasks you saved earlier.
    ____________________________________________________________

    ____________________________________________________________
     OK, I've marked this task as not done yet:
       [T][ ] read book
    ____________________________________________________________

    ____________________________________________________________
     Noted. I've removed this task:
       [D][ ] return book (by: June 6th)
     Now you have 1 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Noted. I've removed this task:
       [T][ ] read book
     Now you have 0 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     You haven't told me about any tasks yet.
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

**Data file after**

```text
```

### TC20 - A session that changes nothing writes no save file

**Aim:** Check that starting the chatbot and quitting without adding anything
does not create a save file. A first run should leave the disk as it found it,
and a `list` that finds nothing is not a change to be written. This is also what
lets every test case above it start from a genuinely empty state.

**Input**

```text
list
mark 1
bye
```

**Expected output**

```text
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

    ____________________________________________________________
     You haven't told me about any tasks yet.
    ____________________________________________________________

    ____________________________________________________________
     There is nothing to mark yet — your list is empty.
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

**Data file after**

```text
(no file)
```

### TC21 - Bars and backslashes in a task survive the round trip

**Aim:** Check the characters that mean something in the save file itself. A
vertical bar separates the fields of a saved task and a backslash marks the
character after it as ordinary, so a task containing either would split a saved
line in the wrong places if it were written as typed. Both are written as escape
sequences and read back as the characters the user typed: the tasks loaded from
the file below show the bar and the backslash again in `list`, and the task added
during the run is written back escaped in the same way.

**Data file before**

```text
T | 0 | tidy up \| then rest
D | 1 | back up C:\\work | tomorrow
```

**Input**

```text
list
todo a \ b | c
bye
```

**Expected output**

```text
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

    ____________________________________________________________
     Welcome back! I've picked up 2 tasks you saved earlier.
    ____________________________________________________________

    ____________________________________________________________
     Here are the tasks in your list:
     1.[T][ ] tidy up | then rest
     2.[D][X] back up C:\work (by: tomorrow)
    ____________________________________________________________

    ____________________________________________________________
     Got it. I've added this task:
       [T][ ] a \ b | c
     Now you have 3 tasks in the list.
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

**Data file after**

```text
T | 0 | tidy up \| then rest
D | 1 | back up C:\\work | tomorrow
T | 0 | a \\ b \| c
```

### TC22 - Lines that cannot be read are reported and skipped

**Aim:** Check that a damaged save file costs the user only the damaged lines.
Each way a line can be wrong — a kind of task the chatbot does not know, too few
fields for the kind it claims to be, a status field that is neither `1` nor `0`,
and an empty description — is reported on its own, naming the line so the user
can find it, and the good tasks around it are still loaded. A blank line is not
damage and is passed over in silence. The warning that the skipped lines will be
lost is the point of reporting them at all: the next command that changes the
list rewrites the whole file without them.

**Data file before**

```text
T | 1 | read book
X | 0 | who knows
D | 0 | return book
T | 2 | maybe done
T | 0 |

E | 0 | party | Fri 8pm | Fri 11pm
```

**Input**

```text
list
bye
```

**Expected output**

```text
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

    ____________________________________________________________
     Welcome back! I've picked up 2 tasks you saved earlier.
     Line 2 of data/duke.txt isn't a task I can read: "X" is not a kind of task I know (I know T, D and E).
     Line 3 of data/duke.txt isn't a task I can read: a saved deadline has 4 fields, but this line has 3.
     Line 4 of data/duke.txt isn't a task I can read: "2" doesn't say whether the task is done (it should be 1 or 0).
     Line 5 of data/duke.txt isn't a task I can read: the description is empty.
     I've left those 4 lines out of your list.
     They will be lost the next time the list changes — fix the file to keep them.
    ____________________________________________________________

    ____________________________________________________________
     Here are the tasks in your list:
     1.[T][X] read book
     2.[E][ ] party (from: Fri 8pm to: Fri 11pm)
    ____________________________________________________________

    ____________________________________________________________
     Bye. Hope to see you again soon!
    ____________________________________________________________
```

**Data file after**

```text
T | 1 | read book
X | 0 | who knows
D | 0 | return book
T | 2 | maybe done
T | 0 |

E | 0 | party | Fri 8pm | Fri 11pm
```
