package pl.librus.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.librus.mobile.domain.calculations.GradeCalculator
import pl.librus.mobile.domain.models.SubjectGradeGroup
import pl.librus.mobile.ui.AppLanguage
import pl.librus.mobile.ui.Strings

@Composable
fun SimulatorScreen(
    groups: List<SubjectGradeGroup>,
    strings: Strings = Strings(AppLanguage.ENGLISH)
) {
    var selectedSubjectIndex by remember { mutableStateOf(0) }
    val currentGroup = groups.getOrNull(selectedSubjectIndex)

    // Simulated grades list: pairs of (gradeValue, weight)
    var simulatedGrades by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }
    var inputGradeText by remember { mutableStateOf("5") }
    var inputWeightText by remember { mutableStateOf("3") }

    // Target Calculator State
    var targetAverageText by remember { mutableStateOf("4.75") }
    var targetWeightText by remember { mutableStateOf("3") }

    val currentGrades = currentGroup?.semester1Grades ?: emptyList()
    val currentAvg = currentGroup?.semester1Average

    val simulatedAvg = remember(currentGrades, simulatedGrades) {
        if (simulatedGrades.isEmpty()) currentAvg
        else GradeCalculator.simulateNewAverage(currentGrades, simulatedGrades)
    }

    val requiredGrade = remember(currentGrades, targetAverageText, targetWeightText) {
        val target = targetAverageText.toDoubleOrNull()
        val weight = targetWeightText.toDoubleOrNull()
        if (target != null && weight != null) {
            GradeCalculator.calculateRequiredGrade(currentGrades, weight, target)
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Feature Header
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = strings.simHeader,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = strings.simSubtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subject Selector
        Text(
            text = strings.selectSubject,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        var subjectDropdownExpanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(
                onClick = { subjectDropdownExpanded = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = currentGroup?.subject?.name ?: strings.selectSubject,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            DropdownMenu(
                expanded = subjectDropdownExpanded,
                onDismissRequest = { subjectDropdownExpanded = false }
            ) {
                groups.forEachIndexed { index, group ->
                    DropdownMenuItem(
                        text = { Text(group.subject.name) },
                        onClick = {
                            selectedSubjectIndex = index
                            simulatedGrades = emptyList()
                            subjectDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Average Comparison Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(strings.currentAvg, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = currentAvg?.let { "$it" } ?: "-",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text("➔", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(strings.projectedAvg, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = simulatedAvg?.let { "$it" } ?: "-",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Add Simulated Grade Section
        Text(
            text = strings.simNewGradeHeader,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputGradeText,
                onValueChange = { inputGradeText = it },
                label = { Text(strings.gradeOneToSix) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = inputWeightText,
                onValueChange = { inputWeightText = it },
                label = { Text(strings.weight) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )

            Button(
                onClick = {
                    val g = inputGradeText.toDoubleOrNull()
                    val w = inputWeightText.toDoubleOrNull()
                    if (g != null && w != null && g in 1.0..6.0 && w > 0) {
                        simulatedGrades = simulatedGrades + (g to w)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(54.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.addSimulated)
            }
        }

        // List of currently added simulated grades
        if (simulatedGrades.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${strings.addedSimulatedGrades} (${simulatedGrades.size}):", fontSize = 12.sp)
                TextButton(onClick = { simulatedGrades = emptyList() }) {
                    Text(strings.clear, color = MaterialTheme.colorScheme.error)
                }
            }
            simulatedGrades.forEachIndexed { idx, pair ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${strings.simGradeLabel}${pair.first.toInt()}${strings.simWeightLabel}${pair.second.toInt()}", fontWeight = FontWeight.Medium)
                        IconButton(
                            onClick = {
                                simulatedGrades = simulatedGrades.toMutableList().also { it.removeAt(idx) }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Target Goal Calculator Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = strings.targetCalcHeader,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = targetAverageText,
                        onValueChange = { targetAverageText = it },
                        label = { Text(strings.targetAvgLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = targetWeightText,
                        onValueChange = { targetWeightText = it },
                        label = { Text(strings.examWeightLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (requiredGrade != null) {
                    val isFeasible = requiredGrade in 1.0..6.0
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isFeasible) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isFeasible) {
                                    strings.targetSuccess(requiredGrade)
                                } else if (requiredGrade > 6.0) {
                                    strings.targetImpossible(requiredGrade, targetAverageText)
                                } else {
                                    strings.targetAlreadyAbove(targetAverageText)
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (isFeasible) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
