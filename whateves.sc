import java.security._
import java.security.spec._
import javax.crypto._
import javax.crypto.spec._


val keyGen = KeyPairGenerator.getInstance("RSA")
keyGen.initialize(1024)
val pair = keyGen.generateKeyPair()
val (priv, pub) = (pair.getPrivate, pair.getPublic)

pub.getAlgorithm
pub.getFormat
pub.getEncoded

priv.getAlgorithm
priv.getFormat
priv.getEncoded

val keyFactory = KeyFactory.getInstance("RSA")
val pubSpec = keyFactory.getKeySpec(pub, classOf[RSAPublicKeySpec])

val modulous = pubSpec.getModulus
val exponent = pubSpec.getPublicExponent

val x502 = keyFactory.getKeySpec(pub, classOf[X509EncodedKeySpec])

pub.getEncoded
x502.getEncoded