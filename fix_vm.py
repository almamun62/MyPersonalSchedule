import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

# Let's count the number of { and }. 
# If they are mismatched, we can just replace the end of the file.
# We know the last function should end the class.
# Let's just fix it manually by removing the last `}` and ensuring the class bracket is correct.

lines = content.split('\n')
# Looking at the error log, there is an extra `}` at line 416 that is closing the class early.
# Let's find it.
start_idx = 410
end_idx = 425

for i, line in enumerate(lines[start_idx:end_idx]):
    print(f"{start_idx + i}: {line}")
