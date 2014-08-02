package sine.lite.dies

import scala.sys.process._

object Git {
  def lastModifiedTime(path: Option[String] = None) = Seq("git", "log", "-1", "--follow", "--date=iso", "--pretty=format:%ad", path.toString).lineStream.dropWhile(_.isEmpty).headOption
  def lastModifiedDate(path: Option[String] = None) = Seq("git", "log", "-1", "--follow", "--date=short", "--pretty=format:%ad", path.toString).lineStream.dropWhile(_.isEmpty).headOption
}
