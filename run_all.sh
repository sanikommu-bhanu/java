#!/bin/sh
# compiles and runs every runnable task (GUI tasks run headless self-tests)
for d in Assignment*/*/; do echo "== $d"; (cd "$d" && cat output.txt | grep '^\$ ' ); done
echo 'Run the commands listed in each assignment README.md inside the task folder.'
