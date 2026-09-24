import re

with open('app/src/main/java/com/example/service/CourseNotificationManager.kt', 'r') as f:
    content = f.read()

# 1. Update Channel IDs
content = content.replace('val CHANNEL_ONGOING_ID = "channel_ongoing_class"', 'val CHANNEL_ONGOING_ID = "channel_ongoing_class_v2"')
content = content.replace('val CHANNEL_NAG_ID = "channel_nag_loop"', 'val CHANNEL_NAG_ID = "channel_nag_loop_v2"')
content = content.replace('val CHANNEL_REMINDER_15M_ID = "channel_class_reminder_15m"', 'val CHANNEL_REMINDER_15M_ID = "channel_class_reminder_15m_v2"')

# 2. Update Importance and Priority
# Change Ongoing from LOW to DEFAULT
content = content.replace('NotificationManager.IMPORTANCE_LOW', 'NotificationManager.IMPORTANCE_DEFAULT')
content = content.replace('.setPriority(NotificationCompat.PRIORITY_LOW)', '.setPriority(NotificationCompat.PRIORITY_DEFAULT)\n            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)\n            .setCategory(NotificationCompat.CATEGORY_STATUS)')

# Ensure 15m and Nag have CATEGORY_ALARM or CATEGORY_EVENT and max priority
content = content.replace('.setPriority(NotificationCompat.PRIORITY_HIGH)', '.setPriority(NotificationCompat.PRIORITY_MAX)')
content = content.replace('NotificationManager.IMPORTANCE_HIGH', 'NotificationManager.IMPORTANCE_HIGH') # keep high

with open('app/src/main/java/com/example/service/CourseNotificationManager.kt', 'w') as f:
    f.write(content)
print("Updated CourseNotificationManager.kt")
