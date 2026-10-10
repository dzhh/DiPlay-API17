package local.headunit.diplay
import org.junit.Assert.*
import org.junit.Test

class DescriptorParserTest {
 private fun fixture():ByteArray=byteArrayOf(9,2,48,0,2,3,0,0x80.toByte(),50,9,4,0,0,0,2,13,0,0,9,4,1,0,0,10,0,0,0,9,4,1,1,2,10,0,0,0,7,5,0x81.toByte(),2,0,2,0,7,5,2,2,0,2,0).also{it[2]=it.size.toByte()}
 @Test fun keepsAlternateInterfacesAndEndpoints(){val c=DescriptorParser.parse(fixture());assertEquals(3,c.id);assertEquals(3,c.interfaces.size);assertEquals(1,c.interfaces[2].alternate);assertEquals(512,c.interfaces[2].endpoints[0].packetSize)}
 @Test fun rejectsReservedUnconfiguredValue(){val f=fixture();f[5]=0;try{DescriptorParser.parse(f);fail("Accepted unconfigured value")}catch(_:IllegalArgumentException){}}
 @Test fun everyTruncationRejected(){val f=fixture();for(n in 0 until f.size){try{DescriptorParser.parse(f.copyOf(n));fail("Accepted truncation $n")}catch(_:IllegalArgumentException){}}}
 @Test fun rejectsZeroLengthAndNestedConfigurations(){for(offset in listOf(9,18,27,36,43)){val f=fixture();f[offset]=0;try{DescriptorParser.parse(f);fail("Accepted zero length $offset")}catch(_:IllegalArgumentException){}};val f=fixture();f[10]=2;try{DescriptorParser.parse(f);fail("Accepted nested config")}catch(_:IllegalArgumentException){}}
 @Test fun rejectsDuplicateAlternateAndEndpoint(){for(index in listOf(30,45)){val f=fixture();f[index]=if(index==30)0 else 0x81.toByte();try{DescriptorParser.parse(f);fail("Accepted duplicate $index")}catch(_:IllegalArgumentException){}}}
 @Test fun fuzzNeverEscapesArrayBounds(){val random=java.util.Random(123);repeat(5000){val f=fixture();repeat(1+random.nextInt(3)){f[random.nextInt(f.size)]=random.nextInt(256).toByte()};try{DescriptorParser.parse(f)}catch(_:IllegalArgumentException){}}}
}
