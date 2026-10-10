package local.headunit.diplay
import com.shilapi.xcertplay.airplay.BplistCodec
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class BplistBoundsTest {
 private fun u64(bytes:ByteArray,at:Int,value:Long){for(i in 0..7)bytes[at+i]=(value ushr ((7-i)*8)).toByte()}
 private fun plist(objects:List<ByteArray>,root:Int=objects.lastIndex):ByteArray {
  val out=ByteArrayOutputStream();out.write("bplist00".toByteArray());val offsets=ArrayList<Int>()
  for(o in objects){offsets.add(out.size());out.write(o)}
  val table=out.size();require(table<256);for(offset in offsets)out.write(offset)
  val trailer=ByteArray(32);trailer[6]=1;trailer[7]=1;u64(trailer,8,objects.size.toLong());u64(trailer,16,root.toLong());u64(trailer,24,table.toLong());out.write(trailer);return out.toByteArray()
 }
 private fun rejected(bytes:ByteArray){try{BplistCodec.decode(bytes);fail("Unsafe plist accepted")}catch(expected:IllegalArgumentException){}}
 @Test fun impossibleObjectCountRejectedBeforeOffsetAllocation(){val b=BplistCodec.encode(true);u64(b,b.size-24,0x7fffffff);rejected(b)}
 @Test fun offsetsAndReferenceWidthsMustStayInsideInput(){
  val b=BplistCodec.encode(true);b[b.size-33]=0;rejected(b)
  val c=BplistCodec.encode(true);c[c.size-25]=0;rejected(c)
 }
 @Test fun circularReferenceStopsWithoutStackOverflow(){rejected(plist(listOf(byteArrayOf(0xa1.toByte(),0))))}
 @Test fun oversizedContainerCountRejectedBeforeCollectionAllocation(){rejected(plist(listOf(byteArrayOf(0xaf.toByte(),0x13,0x7f,0xff.toByte(),0xff.toByte(),0xff.toByte(),0xff.toByte(),0xff.toByte(),0xff.toByte(),0xff.toByte()))))}
 @Test fun deeplyNestedInputIsBounded(){var value:Any=1L;repeat(80){value=listOf(value)};rejected(BplistCodec.encode(value))}
 @Test fun expandingAcyclicGraphIsAlsoBounded(){val objects=ArrayList<ByteArray>();objects.add(byteArrayOf(9));for(i in 1..20)objects.add(byteArrayOf(0xa2.toByte(),(i-1).toByte(),(i-1).toByte()));rejected(plist(objects))}
 @Test fun validSharedReferencesAreNotMistakenForCycles(){
  val b=plist(listOf(byteArrayOf(0x51,120),byteArrayOf(0xa2.toByte(),0,0),byteArrayOf(0xa2.toByte(),1,1)))
  assertEquals(listOf(listOf("x","x"),listOf("x","x")),BplistCodec.decode(b))
 }
 @Test fun dateAndNullDecodingRemainSupported(){
  assertNull(BplistCodec.decode(plist(listOf(byteArrayOf(0)))))
  val objectBytes=ByteArray(9);objectBytes[0]=0x33;u64(objectBytes,1,java.lang.Double.doubleToRawLongBits(1700000000.0-978307200.0))
  assertEquals(java.util.Date(1700000000000),BplistCodec.decode(plist(listOf(objectBytes))))
 }
 @Test fun supportedValuesRoundTripWithoutFormatChange(){
  val value=linkedMapOf<String,Any?>("unicode" to "导航测试", "raw" to byteArrayOf(0,1,127,-1), "list" to listOf(true,false,1L,65536L,1.25,2.5f))
  val encoded=BplistCodec.encode(value);assertArrayEquals(encoded,BplistCodec.encode(BplistCodec.decode(encoded)))
 }
}
