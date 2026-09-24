# Let's fix this properly. The previous regex broke the file structure because I probably deleted `replaceCourses` entirely or mismatched a brace earlier up.

import urllib.request
import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

# Let's see what is immediately preceding `fun test15MinuteReminder`
match = re.search(r'([\s\S]{300}fun test15MinuteReminder)', content)
if match:
    print(match.group(1))

