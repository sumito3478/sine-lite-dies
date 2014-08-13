package sine.lite.dies

import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.sys.process._
import java.io._
import java.nio.file._
import java.time._
import java.time.format._
import org.eclipse.jgit.lib._
import org.apache.commons.codec.binary.Base32

object Git extends LazyLogging {
  def updatedAt(path: Option[Path] = None): Option[ZonedDateTime] =
    (Seq("git", "log", "-1", "--date=iso", "--pretty=format:%ad") ++ path.toSeq.flatMap(x => Seq("--follow", x.toString)))
      .lineStream.dropWhile(_.isEmpty).headOption.map(ZonedDateTime.parse(_, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")))
  def createdAt(path: Option[Path] = None): Option[ZonedDateTime] =
    (Seq("git", "log", "--reverse", "--date=iso", "--pretty=format:%ad") ++ path.toSeq.flatMap(x => Seq("--follow", x.toString)))
      .lineStream.dropWhile(_.isEmpty).headOption.map(ZonedDateTime.parse(_, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")))
  private[this] val GitDiffIndexLine = "index [0-9a-f]+\\.\\.([0-9a-f]+)".r
  def creationId(path: Path) =
    Seq("git", "log", "--full-index", "--reverse", "--follow", "-p", path.toString).lineStream.find(_.startsWith("index ")) match {
      case Some(GitDiffIndexLine(id)) => Some(ObjectId.fromString(id))
      case Some(x) =>
        logger.warn(s"Could not parse git index line $x")
        None
      case None =>
        logger.warn(s"Could not find the first blob id of $path")
        None
    }
  implicit class ObjectIdOps(val self: ObjectId) extends AnyVal {
    def toURN = {
      val bout = new ByteArrayOutputStream
      self.copyTo(bout)
      s"urn:sha1:${(new Base32).encodeToString(bout.toByteArray)}"
    }
  }
}
