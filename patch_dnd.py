import re

with open('app/src/main/java/com/example/service/DndManager.kt', 'r') as f:
    content = f.read()

content = content.replace("notificationManager.setInterruptionFilter(filter)", """try {
            notificationManager.setInterruptionFilter(filter)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }""")

with open('app/src/main/java/com/example/service/DndManager.kt', 'w') as f:
    f.write(content)
