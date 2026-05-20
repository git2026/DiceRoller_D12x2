package com.padm.d12x2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.graphics.withMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.padm.d12x2.ui.theme.D12DiceRollerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// Curva de desaceleração suave para os dados
private val DiceEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

// Conjunto de cores de cada dado
data class DieColors(
    val light: Color,
    val dark: Color,
    val edge: Color,
    val accent: Color
)

val CrimsonDie = DieColors(
    light = Color(0xFFEF5350),
    dark = Color(0xFFB71C1C),
    edge = Color(0xFF7F1D1D),
    accent = Color(0xFFD32F2F)
)

val SapphireDie = DieColors(
    light = Color(0xFF42A5F5),
    dark = Color(0xFF0D47A1),
    edge = Color(0xFF0A3069),
    accent = Color(0xFF1976D2)
)

// Geometria do dodecaedro: 20 vértices, 12 faces pentagonais
private val PHI = ((1.0 + sqrt(5.0)) / 2.0).toFloat()
private val INV = 1f / PHI

// Coordenadas dos 20 vértices
private val VERTICES = arrayOf(
    floatArrayOf( 1f,  1f,  1f),
    floatArrayOf( 1f,  1f, -1f),
    floatArrayOf( 1f, -1f,  1f),
    floatArrayOf( 1f, -1f, -1f),
    floatArrayOf(-1f,  1f,  1f),
    floatArrayOf(-1f,  1f, -1f),
    floatArrayOf(-1f, -1f,  1f),
    floatArrayOf(-1f, -1f, -1f),
    floatArrayOf( 0f, INV,  PHI),
    floatArrayOf( 0f, INV, -PHI),
    floatArrayOf( 0f,-INV,  PHI),
    floatArrayOf( 0f,-INV, -PHI),
    floatArrayOf( INV, PHI, 0f),
    floatArrayOf( INV,-PHI, 0f),
    floatArrayOf(-INV, PHI, 0f),
    floatArrayOf(-INV,-PHI, 0f),
    floatArrayOf( PHI, 0f, INV),
    floatArrayOf( PHI, 0f,-INV),
    floatArrayOf(-PHI, 0f, INV),
    floatArrayOf(-PHI, 0f,-INV),
)

// Índices dos vértices que compõem cada face
private val FACES = arrayOf(
    intArrayOf( 0,  8, 10,  2, 16),
    intArrayOf( 0, 16, 17,  1, 12),
    intArrayOf( 0, 12, 14,  4,  8),
    intArrayOf( 1, 17,  3, 11,  9),
    intArrayOf( 1,  9,  5, 14, 12),
    intArrayOf( 2, 10,  6, 15, 13),
    intArrayOf( 2, 13,  3, 17, 16),
    intArrayOf( 3, 13, 15,  7, 11),
    intArrayOf( 4, 14,  5, 19, 18),
    intArrayOf( 4, 18,  6, 10,  8),
    intArrayOf( 5,  9, 11,  7, 19),
    intArrayOf( 6, 18, 19,  7, 15),
)

// Valores fixos em cada face
private val FACE_VALUES = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)

// Desvio base de rotação aplicado ao desenho
private const val BASE_RX = 20f
private const val BASE_RY = -25f

// Ângulos pré-calculados para cada face ficar virada para o utilizador
private val FACE_TARGETS: Array<FloatArray> = Array(12) { fi ->
    val f = FACES[fi]
    val cx = f.sumOf { VERTICES[it][0].toDouble() }.toFloat() / 5f
    val cy = f.sumOf { VERTICES[it][1].toDouble() }.toFloat() / 5f
    val cz = f.sumOf { VERTICES[it][2].toDouble() }.toFloat() / 5f
    val len = sqrt(cx * cx + cy * cy + cz * cz)
    val nx = cx / len; val ny = cy / len; val nz = cz / len

    val rxRot = Math.toDegrees(atan2(ny.toDouble(), nz.toDouble())).toFloat()
    val ryRot = Math.toDegrees(-atan2(nx.toDouble(), sqrt((ny * ny + nz * nz).toDouble()))).toFloat()

    floatArrayOf(rxRot - BASE_RX, ryRot - BASE_RY)
}

// Rotação X → Y → Z aplicada a um vetor
private fun rotateXYZ(v: FloatArray, rx: Float, ry: Float, rz: Float): FloatArray {
    val cxr = cos(Math.toRadians(rx.toDouble())).toFloat()
    val sxr = sin(Math.toRadians(rx.toDouble())).toFloat()
    val cyr = cos(Math.toRadians(ry.toDouble())).toFloat()
    val syr = sin(Math.toRadians(ry.toDouble())).toFloat()
    val czr = cos(Math.toRadians(rz.toDouble())).toFloat()
    val szr = sin(Math.toRadians(rz.toDouble())).toFloat()

    var x = v[0]; var y = v[1]; var z = v[2]
    var t = y * cxr - z * sxr; z = y * sxr + z * cxr; y = t
    t = x * cyr + z * syr; z = -x * syr + z * cyr; x = t
    t = x * czr - y * szr; y = x * szr + y * czr; x = t

    return floatArrayOf(x, y, z)
}

// Produto vetorial entre dois vetores
private fun cross(a: FloatArray, b: FloatArray) = floatArrayOf(
    a[1] * b[2] - a[2] * b[1],
    a[2] * b[0] - a[0] * b[2],
    a[0] * b[1] - a[1] * b[0]
)

// Normaliza para comprimento
private fun normalize(v: FloatArray): FloatArray {
    val len = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
    return if (len > 1e-6f) floatArrayOf(v[0] / len, v[1] / len, v[2] / len) else v.copyOf()
}

// Interpolação linear entre duas cores
private fun mixColor(a: Color, b: Color, t: Float): Color {
    val f = t.coerceIn(0f, 1f)
    return Color(
        red   = a.red   + (b.red   - a.red)   * f,
        green = a.green + (b.green - a.green) * f,
        blue  = a.blue  + (b.blue  - a.blue)  * f,
        alpha = 1f
    )
}

// Área de um polígono 2D
private fun shoelaceArea(pts: List<Offset>): Float {
    var a = 0f
    for (i in pts.indices) {
        val j = (i + 1) % pts.size
        a += pts[i].x * pts[j].y - pts[j].x * pts[i].y
    }
    return abs(a) / 2f
}

// Delta angular para aterrar no ângulo-alvo após N voltas completas
private fun angleDelta(current: Float, target: Float, spins: Int): Float {
    val mod = ((target - current % 360f) % 360f + 360f) % 360f
    return spins * 360f + mod
}

// Ecrã principal com os dois dados e o botão do "Roll"
@Composable
fun DiceRollerScreen() {
    // Resultado de cada dado
    var dice1 by remember { mutableIntStateOf(0) }
    var dice2 by remember { mutableIntStateOf(0) }
    var isRolling by remember { mutableStateOf(false) }
    var hasRolled by remember { mutableStateOf(false) }

    // Desvio vertical para a simulação de gravidade
    var yOff1 by remember { mutableFloatStateOf(0f) }
    var yOff2 by remember { mutableFloatStateOf(0f) }

    // Rotação manual com o dedo
    var dragRx1 by remember { mutableFloatStateOf(0f) }
    var dragRy1 by remember { mutableFloatStateOf(0f) }
    var dragRx2 by remember { mutableFloatStateOf(0f) }
    var dragRy2 by remember { mutableFloatStateOf(0f) }

    val scope = rememberCoroutineScope()

    // Rotação 3D de cada dado (3 eixos por dado)
    val rx1 = remember { Animatable(0f) }
    val ry1 = remember { Animatable(0f) }
    val rz1 = remember { Animatable(0f) }
    val rx2 = remember { Animatable(0f) }
    val ry2 = remember { Animatable(0f) }
    val rz2 = remember { Animatable(0f) }

    // Escala para o efeito de pulso ao lançar
    val scale = remember { Animatable(1f) }

    fun roll() {
        if (isRolling) return
        isRolling = true
        hasRolled = true

        // Resultado aleatório e ângulos correspondentes
        val r1 = (1..12).random()
        val r2 = (1..12).random()
        val t1 = FACE_TARGETS[r1 - 1]
        val t2 = FACE_TARGETS[r2 - 1]

        scope.launch {
            // Absorve a rotação manual na base antes de animar
            val totalRx1 = rx1.value + dragRx1
            val totalRy1 = ry1.value + dragRy1
            val totalRx2 = rx2.value + dragRx2
            val totalRy2 = ry2.value + dragRy2
            dragRx1 = 0f; dragRy1 = 0f
            dragRx2 = 0f; dragRy2 = 0f
            rx1.snapTo(totalRx1)
            ry1.snapTo(totalRy1)
            rx2.snapTo(totalRx2)
            ry2.snapTo(totalRy2)

            dice1 = 0
            dice2 = 0

            // Voltas completas + ângulo preciso de aterragem
            val xd1 = angleDelta(rx1.value, t1[0], (3..5).random())
            val yd1 = angleDelta(ry1.value, t1[1], (2..4).random())
            val zd1 = (2..4).random() * 360f
            val xd2 = angleDelta(rx2.value, t2[0], (3..5).random())
            val yd2 = angleDelta(ry2.value, t2[1], (2..4).random())
            val zd2 = (2..4).random() * 360f

            val dur = 2000

            // Rotação com durações desfasadas para parecer natural
            val j1 = launch { rx1.animateTo(rx1.value + xd1, tween((dur * 1.05).toInt(), easing = DiceEasing)) }
            val j2 = launch { ry1.animateTo(ry1.value + yd1, tween(dur, easing = DiceEasing)) }
            val j3 = launch { rz1.animateTo(rz1.value + zd1, tween((dur * 0.96).toInt(), easing = DiceEasing)) }
            val j4 = launch { rx2.animateTo(rx2.value + xd2, tween((dur * 1.07).toInt(), easing = DiceEasing)) }
            val j5 = launch { ry2.animateTo(ry2.value + yd2, tween((dur * 1.02).toInt(), easing = DiceEasing)) }
            val j6 = launch { rz2.animateTo(rz2.value + zd2, tween((dur * 0.98).toInt(), easing = DiceEasing)) }

            // Gravidade - Dado 1
            val p1 = launch {
                var v = -780f; val g = 2500f; val rest = 0.40f; val dt = 0.014f; var y = 0f
                while (true) {
                    v += g * dt; y += v * dt
                    if (y >= 0f && v > 0f) {
                        y = 0f; v = -v * rest
                        if (abs(v) < 40f) break
                    }
                    yOff1 = y; delay(16)
                }
                yOff1 = 0f
            }

            // Gravidade - Dado 2
            val p2 = launch {
                var v = -780f; val g = 2500f; val rest = 0.40f; val dt = 0.014f; var y = 0f
                while (true) {
                    v += g * dt; y += v * dt
                    if (y >= 0f && v > 0f) {
                        y = 0f; v = -v * rest
                        if (abs(v) < 40f) break
                    }
                    yOff2 = y; delay(16)
                }
                yOff2 = 0f
            }

            // Pulso de escala ao carregar no botão
            val sc = launch {
                scale.animateTo(1.06f, tween(160))
                scale.animateTo(0.97f, tween(130))
                scale.animateTo(1f, spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ))
            }

            j1.join(); j2.join(); j3.join()
            j4.join(); j5.join(); j6.join()
            p1.join(); p2.join(); sc.join()

            // Revela os valores finais
            dice1 = r1
            dice2 = r2
            isRolling = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE8EDF6)) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            // Título da app
            Text(
                "Dice Roller",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF2D3A5C),
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "D12 × 2",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5A6A8A),
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(110.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                DieSlot(
                    dice1, rx1.value + dragRx1, ry1.value + dragRy1, rz1.value,
                    scale.value, yOff1, CrimsonDie, hasRolled, isRolling
                ) { dx, dy ->
                    if (!isRolling) { dragRx1 -= dy * 0.35f; dragRy1 += dx * 0.35f }
                }
                Spacer(Modifier.width(8.dp))
                DieSlot(
                    dice2, rx2.value + dragRx2, ry2.value + dragRy2, rz2.value,
                    scale.value, yOff2, SapphireDie, hasRolled, isRolling
                ) { dx, dy ->
                    if (!isRolling) { dragRx2 -= dy * 0.35f; dragRy2 += dx * 0.35f }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Soma dos dois dados
            if (hasRolled && !isRolling) {
                Text(
                    (dice1 + dice2).toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )
            } else if (isRolling) {
                Text("…", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF94A3B8))
            } else {
                Spacer(Modifier.height(48.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Botão de lançamento
            Button(
                onClick = { roll() },
                enabled = !isRolling,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B5BDB),
                    disabledContainerColor = Color(0xFFA5B4FC)
                ),
                contentPadding = PaddingValues(horizontal = 56.dp, vertical = 18.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 2.dp,
                    hoveredElevation = 10.dp
                )
            ) {
                Text(
                    "ROLL",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    color = Color.White
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

// Slot de um dado: sombra no chão + dado 3D + valor
@Composable
private fun DieSlot(
    value: Int,
    rotX: Float, rotY: Float, rotZ: Float,
    scaleVal: Float, yOffset: Float,
    colors: DieColors, hasRolled: Boolean, isRolling: Boolean,
    onDrag: (dx: Float, dy: Float) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(160.dp)
    ) {
        Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            // Sombra oval no chão, reage à altura do dado
            Canvas(
                Modifier
                    .align(Alignment.BottomCenter)
                    .size(width = 70.dp, height = 10.dp)
                    .offset(y = (-12).dp)
                    .graphicsLayer {
                        val h = abs(yOffset) / 150f
                        alpha = (0.35f - h * 0.30f).coerceIn(0.03f, 0.35f)
                        scaleX = 1f + h * 0.3f
                    }
            ) { drawOval(Color.Black, size = size) }

            D12Die3D(rotX, rotY, rotZ, scaleVal, yOffset, colors, onDrag)
        }
        Spacer(Modifier.height(8.dp))
        // Valor individual após o dado aterrar
        Text(
            text = if (hasRolled && !isRolling && value > 0) value.toString() else "–",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.accent
        )
    }
}

// Dado 3D com arrastamento, gravidade e escala
@Composable
private fun D12Die3D(
    rotX: Float, rotY: Float, rotZ: Float,
    scaleVal: Float, yOffset: Float,
    colors: DieColors,
    onDrag: (dx: Float, dy: Float) -> Unit
) {
    Canvas(
        Modifier
            .size(145.dp)
            .offset(y = yOffset.dp)
            .graphicsLayer { scaleX = scaleVal; scaleY = scaleVal }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        drawDodecahedron(rotX + BASE_RX, rotY + BASE_RY, rotZ, colors)
    }
}

// Desenha o dodecaedro 3D com iluminação e números nas faces
private fun DrawScope.drawDodecahedron(
    rx: Float, ry: Float, rz: Float,
    colors: DieColors
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val sc = size.minDimension * 0.22f
    val fov = 8f

    // Roda todos os vértices e projeta para 2D
    val rot = VERTICES.map { rotateXYZ(it, rx, ry, rz) }
    val proj = rot.map { v ->
        val p = fov / (fov - v[2])
        Offset(cx + v[0] * sc * p, cy - v[1] * sc * p)
    }

    // Direção da luz e vetor médio para reflexo
    val lightDir = normalize(floatArrayOf(-0.35f, 0.55f, 0.75f))
    val halfVec  = normalize(floatArrayOf(lightDir[0], lightDir[1], lightDir[2] + 1f))

    // Informação de cada face visível
    class FaceInfo(
        val i: Int, val z: Float, val diff: Float, val spec: Float,
        val path: Path, val pts: List<Offset>,
        val center3D: FloatArray, val faceU: FloatArray, val faceV: FloatArray
    )

    val visible = mutableListOf<FaceInfo>()

    for (fi in FACES.indices) {
        val f = FACES[fi]

        // Centro da face em 3D
        var fcx = 0f; var fcy = 0f; var fcz = 0f
        for (vi in f) { fcx += rot[vi][0]; fcy += rot[vi][1]; fcz += rot[vi][2] }
        fcx /= 5f; fcy /= 5f; fcz /= 5f

        // Normal exterior da face
        val e1 = floatArrayOf(
            rot[f[1]][0] - rot[f[0]][0],
            rot[f[1]][1] - rot[f[0]][1],
            rot[f[1]][2] - rot[f[0]][2]
        )
        val e2 = floatArrayOf(
            rot[f[2]][0] - rot[f[0]][0],
            rot[f[2]][1] - rot[f[0]][1],
            rot[f[2]][2] - rot[f[0]][2]
        )
        val n = cross(e1, e2).let { raw ->
            if (raw[0] * fcx + raw[1] * fcy + raw[2] * fcz < 0)
                floatArrayOf(-raw[0], -raw[1], -raw[2]) else raw
        }.let { normalize(it) }

        // Descarta faces traseiras
        if (n[2] <= 0.02f) continue

        val ndl = (n[0] * lightDir[0] + n[1] * lightDir[1] + n[2] * lightDir[2]).coerceIn(-1f, 1f)
        val diff = (0.30f + 0.70f * (ndl + 1f) / 2f).coerceIn(0.18f, 1f)

        val ndh = (n[0] * halfVec[0] + n[1] * halfVec[1] + n[2] * halfVec[2]).coerceIn(0f, 1f)
        val spec = ndh.pow(28) * 0.30f

        val path = Path().apply {
            moveTo(proj[f[0]].x, proj[f[0]].y)
            for (j in 1 until 5) lineTo(proj[f[j]].x, proj[f[j]].y)
            close()
        }
        // Eixos locais da face para projeção plana do texto
        val faceU = normalize(e1)
        val faceV = normalize(cross(n, faceU))

        visible.add(FaceInfo(
            fi, fcz, diff, spec, path,
            f.map { proj[it] },
            floatArrayOf(fcx, fcy, fcz), faceU, faceV
        ))
    }

    // Ordena do mais longe ao mais perto
    visible.sortBy { it.z }

    // Preenche cada face com a cor resultante da iluminação
    for (face in visible) {
        val base = mixColor(colors.edge, colors.light, face.diff)
        val col = Color(
            red   = (base.red   + face.spec).coerceAtMost(1f),
            green = (base.green + face.spec).coerceAtMost(1f),
            blue  = (base.blue  + face.spec).coerceAtMost(1f), alpha = 1f
        )
        drawPath(face.path, col, style = Fill)
        drawPath(face.path, colors.dark.copy(alpha = 0.50f),
            style = Stroke(width = 1.6f, join = StrokeJoin.Round))
    }

    // Números projetados sobre cada face visível
    val nativeCanvas = drawContext.canvas.nativeCanvas
    for (face in visible) {
        val area = shoelaceArea(face.pts)
        if (area < 350f) continue

        val fc = face.center3D
        val u = face.faceU
        val v = face.faceV

        // Tamanho do texto no espaço 3D
        val hs = 0.55f

        // Projeta um ponto 3D para coordenadas de ecrã
        fun proj3D(pt: FloatArray): FloatArray {
            val pf = fov / (fov - pt[2])
            return floatArrayOf(cx + pt[0] * sc * pf, cy - pt[1] * sc * pf)
        }

        // Quatro cantos do retângulo de texto sobre a face
        val tl = proj3D(floatArrayOf(
            fc[0] - u[0] * hs + v[0] * hs,
            fc[1] - u[1] * hs + v[1] * hs,
            fc[2] - u[2] * hs + v[2] * hs
        ))
        val tr = proj3D(floatArrayOf(
            fc[0] + u[0] * hs + v[0] * hs,
            fc[1] + u[1] * hs + v[1] * hs,
            fc[2] + u[2] * hs + v[2] * hs
        ))
        val br = proj3D(floatArrayOf(
            fc[0] + u[0] * hs - v[0] * hs,
            fc[1] + u[1] * hs - v[1] * hs,
            fc[2] + u[2] * hs - v[2] * hs
        ))
        val bl = proj3D(floatArrayOf(
            fc[0] - u[0] * hs - v[0] * hs,
            fc[1] - u[1] * hs - v[1] * hs,
            fc[2] - u[2] * hs - v[2] * hs
        ))

        // Matriz que transforma o texto para acompanhar a face
        val textS = 100f
        val matrix = android.graphics.Matrix()
        val ok = matrix.setPolyToPoly(
            floatArrayOf(0f, 0f, textS, 0f, textS, textS, 0f, textS), 0,
            floatArrayOf(tl[0], tl[1], tr[0], tr[1], br[0], br[1], bl[0], bl[1]), 0,
            4
        )
        if (!ok) continue

        val txt = FACE_VALUES[face.i].toString()
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD
            )
            textSize = textS * 0.72f
            textAlign = android.graphics.Paint.Align.CENTER
        }

        nativeCanvas.withMatrix(matrix) {
            // Sombra do número
            paint.color = android.graphics.Color.argb(100, 0, 0, 0)
            drawText(txt, textS / 2f + 1.2f, textS / 2f + textS * 0.22f + 1.2f, paint)

            // Número em branco
            paint.color = android.graphics.Color.WHITE
            drawText(txt, textS / 2f, textS / 2f + textS * 0.22f, paint)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DiceRollerScreenPreview() {
    D12DiceRollerTheme { DiceRollerScreen() }
}
