package com.omerek.guitarlearn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omerek.guitarlearn.data.Lesson
import com.omerek.guitarlearn.data.Lessons
import com.omerek.guitarlearn.data.Progress
import com.omerek.guitarlearn.ui.components.ScreenHeader
import com.omerek.guitarlearn.ui.components.SectionTitle
import com.omerek.guitarlearn.ui.theme.Amber
import com.omerek.guitarlearn.ui.theme.WoodDark

@Composable
fun LessonsScreen(progress: Progress, modifier: Modifier = Modifier, onOpen: (String) -> Unit) {
    val done = progress.completedLessons.value
    val total = Lessons.all.size
    val doneCount = Lessons.all.count { it.id in done }
    val next = Lessons.all.firstOrNull { it.id !in done }
    val grouped = Lessons.all.withIndex().groupBy { it.value.unit }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            ScreenHeader(
                title = "Gitar Öğren",
                subtitle = "Sıfırdan şarkı çalmaya, adım adım",
                icon = Icons.Rounded.School,
            ) {
                ProgressPanel(doneCount, total, next, onOpen)
            }
        }
        grouped.forEach { (unit, lessons) ->
            item(key = unit) {
                val unitDone = lessons.count { it.value.id in done }
                SectionTitle(
                    unit,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                    trailing = "$unitDone/${lessons.size}",
                )
            }
            items(lessons, key = { it.value.id }) { (index, lesson) ->
                val state = when {
                    lesson.id in done -> NodeState.DONE
                    lesson.id == next?.id -> NodeState.NEXT
                    else -> NodeState.TODO
                }
                LessonRow(
                    number = index + 1,
                    lesson = lesson,
                    state = state,
                    isFirst = lessons.first().value.id == lesson.id,
                    isLast = lessons.last().value.id == lesson.id,
                    onClick = { onOpen(lesson.id) },
                )
            }
        }
    }
}

@Composable
private fun ProgressPanel(doneCount: Int, total: Int, next: Lesson?, onOpen: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { doneCount / total.toFloat() },
                modifier = Modifier.size(64.dp),
                color = Amber,
                trackColor = Color.White.copy(alpha = 0.2f),
                strokeWidth = 7.dp,
                strokeCap = StrokeCap.Round,
            )
            Text("$doneCount/$total", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            if (next != null) {
                Text("SIRADAKİ DERS", color = Color(0xFFFFD7B0), style = MaterialTheme.typography.labelSmall)
                Text(next.title, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onOpen(next.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = WoodDark),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (doneCount == 0) "Başla" else "Devam et", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Amber)
                    Spacer(Modifier.width(6.dp))
                    Text("Tüm dersleri bitirdin!", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Text("Şarkılar sekmesinde pratik yapmaya devam et.", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private enum class NodeState { DONE, NEXT, TODO }

/** Ders yolu üzerindeki bir durak: solda numara/tik dairesi ve onu bağlayan çizgi. */
@Composable
private fun LessonRow(number: Int, lesson: Lesson, state: NodeState, isFirst: Boolean, isLast: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier
            .fillMaxWidth()
            .height(84.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Yol çizgisi + durak
        Box(Modifier.width(44.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.width(3.dp).weight(1f).background(if (isFirst) Color.Transparent else line))
                Spacer(Modifier.height(44.dp))
                Box(Modifier.width(3.dp).weight(1f).background(if (isLast) Color.Transparent else line))
            }
            val size = if (state == NodeState.NEXT) 44.dp else 38.dp
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        when (state) {
                            NodeState.DONE -> primary
                            NodeState.NEXT -> MaterialTheme.colorScheme.tertiary
                            NodeState.TODO -> MaterialTheme.colorScheme.surfaceContainerLowest
                        },
                    )
                    .border(2.dp, if (state == NodeState.TODO) line else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                when (state) {
                    NodeState.DONE -> Icon(Icons.Rounded.Check, contentDescription = "Tamamlandı", tint = MaterialTheme.colorScheme.onPrimary)
                    NodeState.NEXT -> Icon(Icons.Rounded.PlayArrow, contentDescription = "Sıradaki", tint = Color.White)
                    NodeState.TODO -> Text("$number", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (state == NodeState.NEXT) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLowest,
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                "$number. ${lesson.title}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (state == NodeState.DONE) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                lesson.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
