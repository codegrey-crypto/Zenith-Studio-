package com.example

import androidx.compose.ui.geometry.Offset
import com.example.studio.ui.AnchorPoint
import com.example.studio.ui.CshImportEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CshImportEngineTest {

    @Test
    fun testParseCshStream_withMockedV2File() {
        // 1. Construct a valid mock Photoshop Custom Shape File (v2) in memory
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Header: "cshb" signature, version 2, 1 shape
        dos.writeBytes("cshb")
        dos.writeInt(2) // Version 2
        dos.writeInt(1) // Shape count

        // Shape Name: "Box" (length 3, 6 bytes UTF-16 BE)
        dos.writeInt(3) // Character count
        dos.writeChar('B'.code)
        dos.writeChar('o'.code)
        dos.writeChar('x'.code)
        // Name padding: total prefix (4) + name length (3 * 2 = 6) = 10.
        // 10 is not divisible by 4. So we pad to next multiple of 4 (12), writing 2 padding bytes.
        dos.writeShort(0)

        // Shape ID & Data Size
        dos.writeInt(123) // ID
        
        // Let's compute geometry data size.
        // Geometry header is 20 bytes (top, left, bottom, right, numPaths).
        // Record 1 (Subpath length record): 26 bytes.
        // Record 2 (Bezier knot record): 26 bytes.
        // Total = 72 bytes.
        dos.writeInt(72)

        // Shape Block Geometry:
        dos.writeInt(0) // top
        dos.writeInt(0) // left
        dos.writeInt(1000) // bottom
        dos.writeInt(1000) // right
        dos.writeInt(2) // numPaths (total of 2 records)

        // Record 1: Closed Subpath Length Record (record type 0)
        dos.writeShort(0) // recordType = 0
        dos.writeShort(1) // numPoints = 1
        // Padding for length record: 22 bytes
        for (i in 0 until 22) {
            dos.writeByte(0)
        }

        // Record 2: Closed Bezier Knot, Linked (record type 1)
        dos.writeShort(1) // recordType = 1
        dos.writeInt(0) // controlInY
        dos.writeInt(0) // controlInX
        dos.writeInt(16777216) // anchorY = 1.0f * 2^24
        dos.writeInt(16777216) // anchorX = 1.0f * 2^24
        dos.writeInt(0) // controlOutY
        dos.writeInt(0) // controlOutX

        dos.flush()

        // 2. Parse the stream using our parsing engine
        val stream = baos.toByteArray().inputStream()
        val shapes = CshImportEngine.parseCshStream(stream)

        // 3. Verify parsed data
        assertEquals(1, shapes.size)
        val shape = shapes[0]
        assertEquals("Box", shape.name)
        assertEquals(1, shape.subpaths.size)
        
        val subpath = shape.subpaths[0]
        assertTrue(subpath.isClosed)
        assertEquals(1, subpath.anchors.size)

        val anchor = subpath.anchors[0]
        assertEquals(Offset(1f, 1f), anchor.position)
        assertEquals(Offset(0f, 0f), anchor.handleIn)
        assertEquals(Offset(0f, 0f), anchor.handleOut)
    }

    @Test
    fun testCshToSvgConversion() {
        val anchor1 = AnchorPoint(
            position = Offset(0.2f, 0.2f),
            handleIn = Offset(0.1f, 0.1f),
            handleOut = Offset(0.3f, 0.3f)
        )
        val anchor2 = AnchorPoint(
            position = Offset(0.8f, 0.8f),
            handleIn = Offset(0.7f, 0.7f),
            handleOut = Offset(0.9f, 0.9f)
        )
        val subpath = CshImportEngine.ParsedCshSubpath(
            isClosed = true,
            anchors = listOf(anchor1, anchor2)
        )
        val shape = CshImportEngine.ParsedCshShape(
            name = "TestStar",
            subpaths = listOf(subpath)
        )

        val pathData = CshImportEngine.convertSubpathsToSvgPathData(listOf(subpath), viewSize = 100f)
        
        // Let us verify that the SVG path commands are built correctly with coordinates multiplied by viewSize (100f)
        assertTrue(pathData.contains("M 20.0 20.0"))
        assertTrue(pathData.contains("80.0 80.0"))
        assertTrue(pathData.contains("Z"))

        val svgString = CshImportEngine.convertCshShapeToSvgString(shape, viewSize = 100f)
        assertTrue(svgString.contains("xmlns=\"http://www.w3.org/2000/svg\""))
        assertTrue(svgString.contains("width=\"100.0\""))
        assertTrue(svgString.contains("viewBox=\"0 0 100.0 100.0\""))
        assertTrue(svgString.contains("d=\"M 20.0 20.0"))
    }
}
