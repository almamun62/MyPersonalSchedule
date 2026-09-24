import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

# I see the problem. The replacement regex earlier removed `    }` that belonged to `replaceCourses`.
# Let's fix the specific replacement string that was inserted.
# The search string was: 
# r'    private fun rescheduleClassRemindersInternal\(\) \{[\s\S]*?\}[\s\S]*?\}'
# That greedily ate too much.

# I will just write a python script to re-add the missing `    }` before `fun loadMamunSchedule`
lines = content.split('\n')
for i, line in enumerate(lines):
    if "fun loadMamunSchedule" in line:
        if lines[i-1].strip() != "}":
            lines.insert(i, "    }")
            break

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
    f.write('\n'.join(lines))
