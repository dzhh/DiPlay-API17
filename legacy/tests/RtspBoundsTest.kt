package local.headunit.diplay
import com.shilapi.xcertplay.airplay.RtspMessage
import org.junit.Assert.*
import org.junit.Test

class RtspBoundsTest {
 private fun wire(headers:String="",body:String="")=("POST /test RTSP/1.0\r\n"+headers+"\r\n"+body).toByteArray(Charsets.US_ASCII)
 private fun rejected(bytes:ByteArray){try{RtspMessage.parseMessages(bytes);fail("Malformed framing accepted")}catch(expected:IllegalArgumentException){}}
 @Test fun invalidLengthsAreNotReinterpretedAsAnotherRequest(){
  for(length in listOf("-1","+1","NaN","999999999999999999999999999999999999"))rejected(wire("Content-Length: $length\r\n"))
 }
 @Test fun oversizedDeclaredBodyIsRejectedBeforeItArrives(){rejected(wire("Content-Length: ${RtspMessage.MAX_BODY_BYTES+1}\r\n"))}
 @Test fun incompleteHeaderCannotAccumulateWithoutBound(){rejected(ByteArray(RtspMessage.MAX_HEADER_BYTES+1){65})}
 @Test fun terminatedOversizedHeaderIsAlsoRejected(){rejected(wire("X-Padding: "+"a".repeat(RtspMessage.MAX_HEADER_BYTES)+"\r\n"))}
 @Test fun duplicateContentLengthsMustAgree(){
  rejected(wire("Content-Length: 1\r\ncontent-length: 2\r\n","xx"))
  assertEquals(1,RtspMessage.parseMessages(wire("Content-Length: 1\r\ncontent-length: 1\r\n","x")).messages.size)
 }
 @Test fun fragmentedAndCoalescedMessagesPreserveExactBodies(){
  val first=wire("CSeq: 1\r\nContent-Length: 5\r\n","hello");val second=wire("CSeq: 2\r\n")
  for(split in 0..first.size){
   val a=RtspMessage.parseMessages(first.copyOfRange(0,split));val b=RtspMessage.parseMessages(a.rest+first.copyOfRange(split,first.size)+second)
   val messages=a.messages+b.messages;assertEquals(2,messages.size);assertArrayEquals("hello".toByteArray(),messages[0].body);assertTrue(messages[1].body.isEmpty());assertTrue(b.rest.isEmpty())
  }
 }
}
