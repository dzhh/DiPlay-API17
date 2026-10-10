package local.headunit.diplay
import com.shilapi.xcertplay.transport.*
import com.shilapi.xcertplay.compat.Base64Compat
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import org.bouncycastle.asn1.x509.Certificate
import org.junit.Assert.*
import org.junit.Test
class PairRecordAuditTest {
 companion object {
  private fun phoneKey():ByteArray {val g=KeyPairGenerator.getInstance("RSA");g.initialize(2048);val p=g.generateKeyPair().public as RSAPublicKey;val der=org.bouncycastle.asn1.pkcs.RSAPublicKey(p.modulus,p.publicExponent).encoded;return ("-----BEGIN RSA PUBLIC KEY-----\n"+Base64Compat.encode(der)+"\n-----END RSA PUBLIC KEY-----\n").toByteArray()}
  private val record by lazy {LockdownPairRecordGenerator.generate(phoneKey(),"00:00:00:00:00:00","test-host","test-buid")}
 }
 private fun copy(phone:ByteArray=record.devicePublicKeyPem,hostPrivate:ByteArray=record.hostPrivateKeyPem,deviceCert:ByteArray=record.deviceCertificatePem)=LockdownPairRecord.restore(record.hostId,record.systemBuid,record.wifiMacAddress,phone,deviceCert,hostPrivate,record.hostCertificatePem,record.rootPrivateKeyPem,record.rootCertificatePem)
 @Test fun completeChainHasNonzeroSerialAndClockTolerance(){
  assertTrue(PairRecordAudit.verify(record).contains("chain_verified=true"))
  val pem=record.rootCertificatePem.toString(Charsets.US_ASCII).lineSequence().filterNot{it.startsWith("-----")}.joinToString("")
  val c=Certificate.getInstance(Base64Compat.decode(pem));assertEquals(java.math.BigInteger.ONE,c.serialNumber.value)
  assertTrue(c.startDate.date.time<=System.currentTimeMillis()-59000);assertTrue(c.endDate.date.time>System.currentTimeMillis())
 }
 @Test fun deviceKeyMismatchRejected(){try{PairRecordAudit.verify(copy(phone=phoneKey()));fail()}catch(e:IllegalArgumentException){assertTrue(e.message!!.contains("device public key"))}}
 @Test fun hostKeyMismatchRejected(){try{PairRecordAudit.verify(copy(hostPrivate=record.rootPrivateKeyPem));fail()}catch(e:IllegalArgumentException){assertTrue(e.message!!.contains("private/public"))}}
 @Test fun corruptSignatureRejected(){val text=record.deviceCertificatePem.toString(Charsets.US_ASCII);val der=Base64Compat.decode(text.lineSequence().filterNot{it.startsWith("-----")}.joinToString(""));der[der.lastIndex]=(der.last().toInt() xor 1).toByte();val cert=("-----BEGIN CERTIFICATE-----\n"+Base64Compat.encode(der)+"\n-----END CERTIFICATE-----\n").toByteArray();try{PairRecordAudit.verify(copy(deviceCert=cert));fail()}catch(e:IllegalArgumentException){assertTrue(e.message!!.contains("signature invalid"))}}
 @Test fun pairRequestNeverSendsPrivateKeys(){val entries=record.toPairRequestDictionary().entries;assertEquals(setOf("DeviceCertificate","HostCertificate","RootCertificate","HostID","SystemBUID"),entries.keys)}
}
