import re

with open('app/src/main/java/com/example/ui/components/SmartPill.kt', 'r') as f:
    content = f.read()

# Replace the method signature
content = content.replace(
    "fun SmartPill(\n    isDndActive: Boolean,\n    currentWeek: Int,\n    modifier: Modifier = Modifier\n) {",
    "fun SmartPill(\n    isDndActive: Boolean,\n    currentWeek: Int,\n    modifier: Modifier = Modifier,\n    ongoingCourseName: String? = null\n) {"
)

# Replace the inner logic for Expanded State
content = content.replace(
"""                    if (isDnd) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "DND".tr,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(4.dp)
                        )
                        Text(
                            text = "Do Not Disturb is Active".tr,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Week".tr,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(4.dp)
                        )
                        Text(
                            text = "Academic Week $currentWeek",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }""",
"""                    if (isDnd) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "DND".tr,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(4.dp)
                        )
                        Text(
                            text = "Do Not Disturb is Active".tr,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (ongoingCourseName != null) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Class".tr,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.padding(4.dp)
                        )
                        Text(
                            text = "Class in Progress: $ongoingCourseName".tr,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Week".tr,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(4.dp)
                        )
                        Text(
                            text = "Academic Week $currentWeek",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }"""
)

# Collapsed State
content = content.replace(
"""                    Text(
                        text = if (isDnd) "DND" else "Week $currentWeek",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )""",
"""                    Text(
                        text = if (isDnd) "DND" else if (ongoingCourseName != null) "In Class" else "Week $currentWeek",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )"""
)

with open('app/src/main/java/com/example/ui/components/SmartPill.kt', 'w') as f:
    f.write(content)

