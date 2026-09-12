# Bob User Guide

// Update the title above to match the actual product name

// Product screenshot goes here

// Product intro goes here

## Adding deadlines

// Describe the action and its outcome.

// Give examples of usage

Example: `keyword (optional arguments)`

// A description of the expected outcome goes here

```
expected output
```

## Editing a task: `edit`

Changes the description or the dates of a task you already have, without deleting
it and adding it again. The task keeps its number in the list, and stays done if it
was done.

Format: `edit TASK_NUMBER [/desc DESCRIPTION] [/by DATE] [/from DATE] [/to DATE]`

* `TASK_NUMBER` is the number shown next to the task by `list`.
* Give at least one of the markers, in any order. Details you leave out stay as
  they are.
* Each kind of task takes only the markers for the details it has:
  * a todo: `/desc`
  * a deadline: `/desc` and `/by`
  * an event: `/desc`, `/from` and `/to`
* Write a date as `yyyy-mm-dd`, followed by a 24-hour time such as `1800` if the
  hour matters. A new date replaces the old one whole, so a date given without a
  time removes the time the task had.
* An event can't be changed so that it ends before it starts.
* What follows a marker runs up to the next marker, so a new description can't
  itself contain `/desc`, `/by`, `/from` or `/to`.

Examples:

* `edit 1 /desc read library book`
* `edit 2 /by 2026-12-05`
* `edit 3 /from 2026-08-07 1000 /to 2026-08-07 1200`

If task 2 is a deadline to return a book by 6pm on 2 December 2026,
`edit 2 /by 2026-12-05` moves it to 5 December and shows the task before and after
the change:

```
Tadaa! Dis was:
  [D][ ] return book (by: Dec 02 2026 18:00)
Now is:
  [D][ ] return book (by: Dec 05 2026)
```

## Feature ABC

// Feature details


## Feature XYZ

// Feature details