package sine.lite.dies

import java.security._
import org.apache.commons.codec.binary.Base32

object Atom {
  def stringToURN(x: String) = s"urn:sha1:${new Base32().encodeToString(MessageDigest.getInstance("SHA-1").digest(x.getBytes("UTF-8")))}"
}
