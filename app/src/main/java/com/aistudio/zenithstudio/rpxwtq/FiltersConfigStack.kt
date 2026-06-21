package com.aistudio.zenithstudio.rpxwtq

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// High-speed transient rendering state synchronization
import com.example.studio.ui.SlidersHighFreqState

data class FilterParameter(
    val id: String,
    val name: String,
    val currentValue: Float,
    val minValue: Float,
    val maxValue: Float
)

data class ActiveEffectNode(
    val id: String,
    val name: String,
    val parameters: List<FilterParameter>
)

@Composable
fun FiltersConfigStack(
    activeEffect: ActiveEffectNode,
    onParameterCommitted: (effectId: String, parameterId: String, newValue: Float) -> Unit,
    onCloseStack: () -> Unit,
    modifier: Modifier = Modifier,
    layerId: String = "" // Added as an override to bind high-frequency canvas rendering
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(Color(0xFF161F32))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Filters & FX Config Stack",
                    color = Color.White,
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Active Item: ${activeEffect.name}",
                    color = Color(0xFF00E5FF),
                    style = TextStyle(fontSize = 12.sp)
                )
            }
            IconButton(onClick = onCloseStack) {
                Text(text = "✕", color = Color.White, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                count = activeEffect.parameters.size,
                key = { index -> activeEffect.parameters[index].id }
            ) { index ->
                val parameter = activeEffect.parameters[index]
                FilterSliderRow(
                    parameter = parameter,
                    onCommitRequested = { finalValue ->
                        onParameterCommitted(activeEffect.id, parameter.id, finalValue)
                    },
                    layerId = layerId,
                    effectId = activeEffect.id
                )
            }
        }
    }
}

@Composable
fun FilterSliderRow(
    parameter: FilterParameter,
    onCommitRequested: (Float) -> Unit,
    layerId: String = "",
    effectId: String = ""
) {
    // Stage 1: Volatile state handles micro-gestures locally without invalidating the global layout tree
    var transientValue by remember { mutableStateOf(parameter.currentValue) }
    var textValue by remember { mutableStateOf(String.format("%.2f", parameter.currentValue)) }

    LaunchedEffect(parameter.currentValue) {
        transientValue = parameter.currentValue
        textValue = String.format("%.2f", parameter.currentValue)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = parameter.name,
                color = Color.White,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
            )
            
            BasicTextField(
                value = textValue,
                onValueChange = { newValue ->
                    textValue = newValue
                    newValue.toFloatOrNull()?.let { parsed ->
                        if (parsed in parameter.minValue..parameter.maxValue) {
                            transientValue = parsed
                            if (layerId.isNotEmpty() && effectId.isNotEmpty()) {
                                SlidersHighFreqState.set("${layerId}_effect_${effectId}_${parameter.id}", parsed)
                            }
                            onCommitRequested(parsed)
                        }
                    }
                },
                textStyle = TextStyle(color = Color(0xFF00E5FF), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .background(Color(0xFF0B0F19))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .width(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = transientValue,
            onValueChange = { currentDragValue ->
                // Quantize outputs to eliminate rendering junk from float noise
                transientValue = (currentDragValue * 100f).roundToInt() / 100f
                textValue = String.format("%.2f", transientValue)

                // Inject into the high frequency state map to trigger real-time canvas redraws
                if (layerId.isNotEmpty() && effectId.isNotEmpty()) {
                    SlidersHighFreqState.set("${layerId}_effect_${effectId}_${parameter.id}", transientValue)
                }
            },
            // Stage 2: EXPLICIT TRANSACTION FIX - Commit value back to database repository when gesture completes
            onValueChangeFinished = {
                onCommitRequested(transientValue)
            },
            valueRange = parameter.minValue..parameter.maxValue,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFA855F7),
                activeTrackColor = Color(0xFFA855F7),
                inactiveTrackColor = Color(0xFF0B0F19)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
