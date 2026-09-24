import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

# Add to buildUiState declaration
content = content.replace(
    "is15mEnabled: Boolean,",
    "is15mEnabled: Boolean,\n        remMins: Int,"
)

# Add to buildUiState call
content = content.replace(
    "is15mEnabled = is15mEnabled,",
    "is15mEnabled = is15mEnabled,\n                    remMins = remMins,"
)

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
    f.write(content)
