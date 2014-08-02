package sine.lite.dies

import scala.sys.process._
import java.nio.file._
import java.time._
import java.time.format._

object Git {
  def lastModifiedTime(path: Option[Path] = None): Option[ZonedDateTime] =
    (Seq("git", "log", "-1", "--date=iso", "--pretty=format:%ad") ++ path.toSeq.flatMap(x => Seq("--follow", x.toString)))
      .lineStream.dropWhile(_.isEmpty).headOption.map(ZonedDateTime.parse(_, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")))
}
